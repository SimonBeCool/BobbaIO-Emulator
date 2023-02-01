package io.bobba.poc.core.rooms.items;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.awt.Point;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.rooms.FurniRemoveComposer;
import io.bobba.poc.communication.outgoing.rooms.SerializeFloorItemComposer;
import io.bobba.poc.communication.outgoing.rooms.SerializeWallItemComposer;
import io.bobba.poc.core.items.BaseItem;
import io.bobba.poc.core.items.ItemType;
import io.bobba.poc.core.rooms.Room;
import io.bobba.poc.core.rooms.users.RoomUser;
import io.bobba.poc.core.users.Coord;
import io.bobba.poc.core.users.inventory.UserItem;
import io.bobba.poc.threading.ThreadPooling;
import io.bobba.poc.threading.runnables.HabboWheelInteractor;
import io.bobba.poc.threading.runnables.RandomBottleNumber;
import io.bobba.poc.threading.runnables.RandomDiceNumber;

public class RoomItemManager {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RoomItemManager.class);
	private Map<Integer, RoomItem> floorItems;
	private Map<Integer, WallItem> wallItems;
	private Room room;

	public RoomItemManager(Room room) {
		this.room = room;
		floorItems = new HashMap<>();
		wallItems = new HashMap<>();
	}
	
	public void initialize() throws SQLException {
		//this.loadRoomsFurnisFromDb();
	}


	public void loadRoomsFurnisFromDb() throws SQLException {
	    Map<Integer, BaseItem> itemCache = new HashMap<>();
	    String query = "SELECT * FROM room_furnis WHERE room_id = ?";
	    try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
	         PreparedStatement statement = connection.prepareStatement(query)) {
	        statement.setInt(1, room.getRoomData().getId());
	        try (ResultSet set = statement.executeQuery()) {
	            while (set.next()) {
	                int id = set.getInt("id");
	                int itemId = set.getInt("item_id");
	                int posX = set.getInt("x");
	                int posY = set.getInt("y");
	                int rotation = set.getInt("rot");
	                int extraData = set.getInt("extra_data");

	                BaseItem baseItem = itemCache.get(itemId);
	                if (baseItem == null) {
	                    baseItem = BobbaEnvironment.getGame().getItemManager().findItemByBaseId(itemId);
	                    itemCache.put(itemId, baseItem);
	                }

	                if (baseItem == null) {
	                    LOGGER.error("Item is null for id: " + itemId);
	                } else if (baseItem.getType() == ItemType.WallItem) {
	                    addWallItemToRoom(id, posX, posY, rotation, extraData, baseItem);
	                } else {
	                    double nextZ = room.getGameMap().sqAbsoluteHeight(new Point(posX, posY));
	                    addFloorItemToRoom(id, posX, posY, nextZ, rotation, extraData, baseItem);
	                }
	            }
	        }
	    } catch (SQLException e) {
	        throw e;
	    }
	}
	
	
	
	public Coord getCoordinate(int x, int y) {
        return new Coord(x, y);
    }
	
	public RoomItem getItem(int id) {
		if (floorItems.containsKey(id))
			return floorItems.get(id);
		if (wallItems.containsKey(id))
			return wallItems.get(id);
		return null;
	}

	public List<RoomItem> getFloorItems() {
		return new ArrayList<>(floorItems.values());
	}

	public List<WallItem> getWallItems() {
		return new ArrayList<>(wallItems.values());
	}
	
	public void addFloorItemToRoom(int id, int x, int y, double z, int rot, int extraData, BaseItem baseItem) {
		if (getItem(id) == null) {
			floorItems.put(id, new RoomItem(id, x, y, z, rot, extraData, room, baseItem));
			room.getGameMap().addItemToMap(floorItems.get(id));
			room.sendMessage(new SerializeFloorItemComposer(floorItems.get(id)));
			room.getRoomUserManager().updateUserStatusses();
		}
	}
	
	public boolean IsWallItem(int id) {
		RoomItem item = getItem(id);
		if (item != null && item.getBaseItem().getType().toString() == "i") {
			return true;
		}
		return false;
	}
	
	public boolean IsFloorItem(int id) {
		RoomItem item = getItem(id);
		if (item != null && item.getBaseItem().getType().toString() == "s") {
			return true;
		}
		return false;
	}
	
	public Coord getSquareInFront(RoomItem item) {
		int x_tile = item.getX();
		int y_tile = item.getY();
        Coord sq = new Coord(x_tile, y_tile);

        if (item.getRot() == 0) {
            sq.y--;
        } else if (item.getRot() == 2) {
            sq.x++;
        } else if (item.getRot() == 4) {
            sq.y++;
        } else if (item.getRot() == 6) {
            sq.x--;
        }

        return sq;
    }
	
	public Coord getSquareBehind(RoomUser user) {
		int x_tile = user.getX();
		int y_tile = user.getY();
		
        Coord sq = new Coord(x_tile, y_tile);

        if (user.getRot() == 0) {
            sq.y++;
        } else if (user.getRot() == 2) {
            sq.x--;
        } else if (user.getRot() == 4) {
            sq.y--;
        } else if (user.getRot() == 6) {
            sq.x++;
        }
        return sq;
    }

	public void removeItem(int id) {
		RoomItem item = getItem(id);
		if (item != null) {
			if (item instanceof WallItem) {
				wallItems.remove(id);
			} else {
				floorItems.remove(id);
				room.getGameMap().removeItemFromMap(item);
				room.getRoomUserManager().updateUserStatusses();
			}
			room.sendMessage(new FurniRemoveComposer(id));
		}
	}

	public void removeAllFurniture() {
		List<Integer> items = new ArrayList<>();
		items.addAll(floorItems.keySet());
		items.addAll(wallItems.keySet());
		for (int itemId : items) {
			removeItem(itemId);
		}
	}

	public void addWallItemToRoom(int id, int x, int y, int rot, int state, BaseItem baseItem) {
		if (getItem(id) == null) {
			wallItems.put(id, new WallItem(id, x, y, rot, state, room, baseItem));
			room.sendMessage(new SerializeWallItemComposer(wallItems.get(id)));
		}
	}
	
	// INTERACTION HANDLER - NEW (15.01.2023)
	public void furniInteract(RoomUser user, int itemId) {
		RoomItem item = getItem(itemId);
		if (item != null) {
			item.getInteractor().onTrigger(user, true);
			String interact = item.getBaseItem().getInteractionType().toString();
			if(interact != null) {
				switch (interact) {
					case "CURRENCY":
						user.getUser().setCredits(user.getUser().getCredits() + BobbaEnvironment.getGame().getCatalogue().findItem(item.getBaseItem().getItemName()).getCost());
						removeItem(itemId);
						user.chat("Du hast: "+ BobbaEnvironment.getGame().getCatalogue().findItem(item.getBaseItem().getItemName()).getCost()+ " Taler eingelöst.");
					break;
					case "DICE":
						item.setState(-1);
						item.updateState();
						BobbaEnvironment.getThreading().run(new RandomDiceNumber(item), 1500);
					break;
					case "HABBOWHEEL":
						item.setState(-1);
						item.updateState();
						BobbaEnvironment.getThreading().run(new HabboWheelInteractor(item), 1500);
					break;
					case "BOTTLE":
						item.setState(-1);
						item.updateState();
						BobbaEnvironment.getThreading().run(new RandomBottleNumber(item), 1500);
					break;
				}
			}
		}
	}

	public void onCycle() {
	}

	public void handleItemMovement(int itemId, int x, int y, int rot, RoomUser user) {
		RoomItem item = getItem(itemId);
		if (item != null) {
			if (item.getBaseItem().getDirections().contains(rot)) {
				this.removeItem(itemId);
				if (item instanceof WallItem) {
					this.addWallItemToRoom(itemId, x, y, rot, item.getState(), item.getBaseItem());
				} else {
					double nextZ = room.getGameMap().sqAbsoluteHeight(new Point(x, y));
					this.addFloorItemToRoom(itemId, x, y, nextZ, rot, item.getState(), item.getBaseItem());
				}
			}
		}
	}

	public void handleItemPickUp(int itemId, RoomUser user) {
		RoomItem item = getItem(itemId);
		if (item != null) {
			this.removeItem(itemId);
			
			user.getUser().getInventory().addItem(itemId, item.getBaseItem(), item.getState());
		}
	}
	
	public void handlePickAll(RoomUser user) {
		for (RoomItem item: new ArrayList<>(floorItems.values())) {
			handleItemPickUp(item.getId(), user);
		}
		for (RoomItem item: new ArrayList<>(wallItems.values())) {
			handleItemPickUp(item.getId(), user);
		}
	}

	public void handleItemPlacement(int itemId, int x, int y, int rot, RoomUser user) {
		UserItem userItem = user.getUser().getInventory().getItem(itemId);
		if (userItem != null) {
			user.getUser().getInventory().removeItem(itemId);
			if (userItem.getBaseItem().getType() == ItemType.WallItem) {
				addWallItemToRoom(itemId, x, y, rot, userItem.getState(), userItem.getBaseItem());
			} else {
				double nextZ = room.getGameMap().sqAbsoluteHeight(new Point(x, y));
				this.addFloorItemToRoom(itemId, x, y, nextZ, rot, userItem.getState(), userItem.getBaseItem());
			}
		}

	}
}
