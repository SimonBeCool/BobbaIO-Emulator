package io.bobba.poc.core.navigator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.navigator.NavigatorRoomListComposer;
import io.bobba.poc.core.rooms.Room;
import io.bobba.poc.core.rooms.roomdata.RoomData;
import io.bobba.poc.core.users.User;

public class Navigator {
	public Navigator() {

	}

	public void handleGetPopularRooms(User user) throws SQLException {
	    try (
	        Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
	        Statement statement = connection.createStatement();
	        ResultSet set = statement.executeQuery("SELECT id FROM rooms WHERE user_counter >= 1")
	    ) {
	        List<RoomData> data = new ArrayList<>();
	        
	        while (set.next()) {                
	            int roomid = set.getInt("id");
	            RoomData roomdata = BobbaEnvironment.getGame().getRoomManager().getLoadedRoom(roomid).getRoomData();
	            data.add(roomdata);
	        }

	        user.getClient().sendMessage(new NavigatorRoomListComposer(data));
	    } catch (SQLException e) {
	        throw e;
	    }
	}

	public void handleGetOwnRooms(User user) {
		List<Room> rooms = BobbaEnvironment.getGame().getRoomManager().getLoadedRooms();
		List<RoomData> data = new ArrayList<>();

		for (Room room : rooms) {
			if (room.getRoomData().getOwner().toLowerCase().equals(user.getUsername().toLowerCase())) {
				data.add(room.getRoomData());
			}
		}

		user.getClient().sendMessage(new NavigatorRoomListComposer(data));
	}

	public void handleSearchRooms(User user, String search) {
		List<Room> rooms = BobbaEnvironment.getGame().getRoomManager().getLoadedRooms();
		List<RoomData> data = new ArrayList<>();

		for (Room room : rooms) {
			if (room.getRoomData().getName().toLowerCase().contains(search.toLowerCase())) {
				data.add(room.getRoomData());
			}
		}

		user.getClient().sendMessage(new NavigatorRoomListComposer(data));
	}
}
