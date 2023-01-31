package io.bobba.poc.core.rooms.items;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.rooms.FurniStateComposer;
import io.bobba.poc.communication.protocol.ServerMessage;
import io.bobba.poc.core.items.BaseItem;
import io.bobba.poc.core.rooms.Room;
import io.bobba.poc.core.rooms.RoomManager;
import io.bobba.poc.core.rooms.users.RoomUser;
import io.bobba.poc.core.rooms.gamemap.GameMap;
import io.bobba.poc.core.rooms.items.interactors.InteractorGenericSwitch;
import io.bobba.poc.core.rooms.items.interactors.RoomItemInteractor;
import io.bobba.poc.threading.runnables.*;
import io.bobba.poc.misc.TextHandling;

import java.awt.Point;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RoomItem implements Runnable {
    private int id;
    private int x, y, rot;
    private double z;
    private int state;
    private int extraData;
    private Room room;

    private boolean needsUpdate;
    private BaseItem baseItem;

    private RoomItemInteractor interactor;
    private List<Point> coords;

    public RoomItem(int id, int x, int y, double z, int rot, int extraData, Room room, BaseItem baseItem) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.rot = rot;
        this.z = z;
        this.state = extraData;
        this.room = room;
        this.needsUpdate = false;
        this.baseItem = baseItem;
        this.interactor = new InteractorGenericSwitch(this);
        updateCoords();
    }

    public RoomItemInteractor getInteractor() {
        return interactor;
    }

    private void updateCoords() {
        coords = new ArrayList<>();
        coords = GameMap.getAffectedTiles(baseItem.getX(), baseItem.getY(), x, y, rot);
        coords.add(new Point(x, y));
    }

    public List<Point> getCoords() {
        if (needsUpdate || coords == null) {
            updateCoords();
        }
        return coords;
    }

    public int getState() {
        return state;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getRot() {
        return rot;
    }

    public void setRot(int rot) {
        this.rot = rot;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public boolean isNeedsUpdate() {
        return needsUpdate;
    }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    public BaseItem getBaseItem() {
        return baseItem;
    }

    public void setBaseItem(BaseItem baseItem) {
        this.baseItem = baseItem;
    }
    
    public int getRandomNumber(int min, int max) {
        Random random = new Random();
        return random.ints(min, max)
          .findFirst()
          .getAsInt();
    }

    public void updateState() {
        this.needsUpdate = true;
        try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection(); Statement statement = connection.createStatement()) {
            String query = "UPDATE room_furnis SET extra_data = "+ state +" WHERE id = "+id;
            if (statement.execute(query)) {}
        } catch (SQLException e) {
            System.out.println("RoomItem SQL ERROR: "+e);
        }
        
        ServerMessage updateMessage = new FurniStateComposer(id, state);
        room.sendMessage(updateMessage);
    }

    public double getTotalHeight() {
    	if (baseItem.getZ() < 0 || z < 0) {
            throw new IllegalStateException("Invalid height values: baseItem.getZ() = " + baseItem.getZ() + ", z = " + z);
        } else {
        	double height = baseItem.getZ() + z;
        	return height;
        }
    }

    public void onUserWalk(RoomUser user) {
    	// IS IT SITABLE ?
        if (getBaseItem().isSeat()) {
            user.addStatus("sit", TextHandling.getFloatString(getBaseItem().getZ()));
            user.setZ(getZ());
            user.setRot(getRot());
            user.setNeedsUpdate(true);
        }
        // IS ROLLER ?
        if(getBaseItem().getInteractionType().toString() == "ROLLER") {
			Point point = null;
            if (getRot() == 4) {
                point = new Point(user.getX(), user.getY() + 1);
            }
            if (getRot() == 0) {
                point = new Point(user.getX(), user.getY() - 1);
            }
            if (getRot() == 6) {
                point = new Point(user.getX() - 1, user.getY());
            }
            if (getRot() == 2) {
                point = new Point(user.getX() + 1, user.getY());
            }
            if (point != null) {
                List<RoomUser> userList = user.getRoom().getGameMap().getRoomUsersForSquare(point);
                if (userList.size() > 0) {
                    user = userList.get(0);
                }
            }
            
            RoomItem item = BobbaEnvironment.getGame().getRoomManager().getLoadedRoom(user.getRoom().getRoomData().getId()).getRoomItemManager().getItem(getId());
            
            if (user != null) {
            	item.setState(-1);
				item.updateState();
            	BobbaEnvironment.getThreading().run(new RollerInteractor(user, getRot(), item), 1500);
            }
           
        }
        
        // IS WATER ?
        if(getBaseItem().getInteractionType().toString() == "WATER1" || getBaseItem().getInteractionType().toString() == "WATER2" || getBaseItem().getInteractionType().toString() == "WATER3") {
        	if (!user.hasStatus("swim"))
                user.addStatus("swim", "0");
                user.setNeedsUpdate(true);
        }
    }

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
