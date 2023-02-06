package io.bobba.poc.core.rooms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.roomdata.HeightMapComposer;
import io.bobba.poc.communication.outgoing.roomdata.RoomDataComposer;
import io.bobba.poc.communication.outgoing.roomdata.RoomModelInfoComposer;
import io.bobba.poc.core.rooms.gamemap.RoomModel;
import io.bobba.poc.core.rooms.roomdata.LockType;
import io.bobba.poc.core.rooms.roomdata.RoomData;
import io.bobba.poc.core.users.User;
import io.bobba.poc.threading.ThreadPooling;

public class RoomManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPooling.class);
	private static int roomId = 1;
	private Map<Integer, Room> rooms;
	private Map<String, RoomModel> models;
	
	public RoomManager() {
		this.rooms = new HashMap<>();
		this.models = new HashMap<>();
	}
	
	public RoomModel getModel(String modelId) {
		return models.getOrDefault(modelId, null);
	}
	
	public Room getLoadedRoom(int roomId) {
		return rooms.getOrDefault(roomId, null);
	}
	
	public void initialize() throws SQLException {
		this.loadModelsFromDb();
		this.loadRoomsFromDb();
	}
	
	private void loadModelsFromDb() throws SQLException {
	    String sql = "SELECT id, door_x, door_y, door_z, door_dir, heightmap FROM room_models";
	    try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
	         PreparedStatement statement = connection.prepareStatement(sql)) {
	        try (ResultSet set = statement.executeQuery()) {
	            while (set.next()) {
	                String name = set.getString("id");
	                int doorX = set.getInt("door_x");
	                int doorY = set.getInt("door_y");
	                int doorZ = set.getInt("door_z");
	                int doorDir = set.getInt("door_dir");
	                String heightmap = set.getString("heightmap");

	                models.put(name, new RoomModel(doorX, doorY, doorZ, doorDir, heightmap));
	            }
	        }
	    }
	}
	
	private void loadRoomsFromDb() throws SQLException {
	    try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
	         Statement statement = connection.createStatement();
	         ResultSet set = statement.executeQuery("SELECT * FROM rooms")) {
	        
	        while (set.next()) {                
	            int roomid = set.getInt("id");
	            String roomName = set.getString("name");
	            String roomOwner = set.getString("owner");
	            String roomDesc = set.getString("description");
	            String roomPass = set.getString("password");
	            String roomModel = set.getString("modelId");
	            String roomLockType = set.getString("lockType");
	            int roomCapacity = set.getInt("capacity");
	            LockType lockStyle = LockType.valueOf(roomLockType);
	            
	            if("Locked".equals(roomLockType))
	                lockStyle = LockType.Locked;
	            else if("Password".equals(roomLockType))
	                lockStyle = LockType.Password;
	            
	            RoomData roomData = new RoomData(roomid, roomName, roomOwner, roomDesc, roomCapacity, roomPass, roomModel, lockStyle);
	            Room room = new Room(roomData, getModel(roomData.getModelId()));		
	            this.rooms.put(room.getRoomData().getId(), room);
	            roomId++;
	            
	            if (room != null)
	                room.getRoomItemManager().loadRoomsFurnisFromDb();
	        }
	    } catch (SQLException e) {
	        throw e;
	    }
	    LOGGER.info("Rooms -> Loaded!");
	}

	public void onCycle() {
		List<Room> cyclingRooms = new ArrayList<>(rooms.values());
		for (Room room : cyclingRooms) {
			room.onCycle();
		}
	}
	
	public void prepareRoomForUser(User user, int roomId, String password) {
	    Room currentRoom = user.getCurrentRoom();
	    if (currentRoom != null) {
	        currentRoom.getRoomUserManager().removeUserFromRoom(user);
	    }
	    Room newRoom = roomId == -1 ? this.getLoadedRooms().get(0) : this.getLoadedRoom(roomId);
	    if (newRoom != null) {
	        user.setLoadingRoomId(newRoom.getRoomData().getId());
	        user.getClient().sendMessage(new RoomModelInfoComposer(newRoom.getRoomData().getModelId(), newRoom.getRoomData().getId()));
	    }
	}
	
	public void prepareHeightMapForUser(User user) {
		Room room = this.getLoadedRoom(user.getLoadingRoomId());
		if (room != null) {
			user.getClient().sendMessage(new HeightMapComposer(room.getGameMap().getRoomModel()));
		}
	}

	public void finishRoomLoadingForUser(User user) {
		Room room = this.getLoadedRoom(user.getLoadingRoomId());
		if (room != null) {
				room.getRoomUserManager().addUserToRoom(user);
				user.setLoadingRoomId(0);
				user.getClient().sendMessage(new RoomDataComposer(room.getRoomData()));
				try (
					Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
				    Statement statement = connection.createStatement();
				) {
					String query = "UPDATE rooms SET user_counter = user_counter + 1 WHERE id = " + room.getRoomData().getId();
				    statement.executeUpdate(query);
				} catch (SQLException e) {
					System.out.println("Error updating room user_counter: " + e);
				}
			}
		}

	public void handleUserLeaveRoom(User user) {
		Room room = this.getLoadedRoom(user.getCurrentRoom().getRoomData().getId());
		if (room != null) {
			room.getRoomUserManager().removeUserFromRoom(user);
			try (
				Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
				Statement statement = connection.createStatement();
				) {
					statement.execute("UPDATE rooms SET user_counter = user_counter - 1 WHERE id = " + room.getRoomData().getId());
				} catch (SQLException e) {
					System.out.println("Error updating user counter: " + e);
				}
		}
	}
	
	public List<Room> getLoadedRooms() {
		return new ArrayList<>(rooms.values());
	}

	public void createRoom(User user, String roomName, String modelId) {
	    RoomModel model = getModel(modelId);
	    if (roomName.length() > 0 && model != null) {
	        RoomData roomData = new RoomData(roomId++, roomName, user.getUsername(), "", 25, "", modelId, LockType.Open);
	        Room room = new Room(roomData, model);
	        this.rooms.put(room.getRoomData().getId(), room);
	        prepareRoomForUser(user, room.getRoomData().getId(), "");

	        try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
	             PreparedStatement statement = connection.prepareStatement("INSERT INTO rooms (name, owner, description, capacity, password, modelId, lockType) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
	            statement.setString(1, roomName);
	            statement.setString(2, user.getUsername());
	            statement.setString(3, "");
	            statement.setInt(4, 25);
	            statement.setString(5, "");
	            statement.setString(6, modelId);
	            statement.setString(7, LockType.Open.toString());
	            statement.execute();
	        } catch (SQLException e) {
	            System.out.println("Error creating new room: " + e);
	        }
	    }
	}
}
