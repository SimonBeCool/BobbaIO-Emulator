package io.bobba.poc.core.rooms.users;

import java.awt.Point;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.alert.HandleAlert;

import io.bobba.poc.core.rooms.Room;
import io.bobba.poc.misc.TextHandling;

public class SimpleChatCommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleChatCommandHandler.class);
    private SimpleChatCommandHandler() {
    	
    }

    private enum Command {
    	SIT("sit", "Makes you sit on the furniture."),
        MAPS("maps", "Generates maps of the room."),
        ABOUT("about", "Displays information about the emulator."),
        UPDATE_CATALOG("update_catalog", "Updates the catalog."),
        UNLOAD("unload", "Unloads the room."),
        ALERT("alert", "Sends an alert to all users in the server."),
        ROOM_ALERT("roomalert", "Sends an alert to all users in the room."),
        PICK_ALL("pickall", "Picks up all items in the room."),
        COORDS("coords", "Displays your current coordinates in the room."),
        USER_PULL("pull", "Pulls a user to your location."),
        USER_PUSH("push", "Pushes a user to a different location."),
        COMMANDS("commands", "Displays a list of available commands and their descriptions.");
    	

    	private final String commandString;
        private final String description;

        Command(String commandString, String description) {
            this.commandString = commandString;
            this.description = description;
        }

        public String getCommandString() {
            return commandString;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final Map<String, Command> COMMAND_MAP;

    static {
        COMMAND_MAP = new HashMap<>();
        for (Command command : Command.values()) {
            COMMAND_MAP.put(command.getCommandString(), command);
        }
    }

    public static boolean parse(RoomUser currentUser, String input) {
        String[] args = input.split(" ");

        if (currentUser == null || currentUser.getUser() == null || currentUser.getUser().getClient() == null || currentUser.getRoom() == null)
            return false;

        Command command = COMMAND_MAP.get(args[0].toLowerCase());
        if (command == null) {
            return false;
        }

        @SuppressWarnings("unused")
		RoomUser targetRoomUser = null;

        switch (command) {
            case SIT: {
                if (!currentUser.hasStatus("sit")) {
                    int newRot = currentUser.getRot();
                    if (newRot % 2 == 1) {
                        newRot = newRot - 1;
                    }
                    currentUser.setRot(newRot);
                    currentUser.addStatus("sit", "0.55");
                } else {
                    currentUser.removeStatus("sit");
                }
                currentUser.setNeedsUpdate(true);
                return true;
            }
            case MAPS: {
                currentUser.getRoom().getGameMap().generateMaps();
                return true;
            }
            case ABOUT: {
            	int loadedRooms = BobbaEnvironment.getGame().getRoomManager().getLoadedRooms().size();
                String credits = 
                    "Josedn - <a href='https://github.com/Josedn'>Github</a><br>" +
                    "Meliodas - <a href='https://github.com/SimonBeCool'>Github</a><br>" +
                    "code-rain002 - <a href='https://github.com/code-rain002'>Github</a><br>";
                currentUser.getUser().getClient().sendMessage(
                    new HandleAlert(true, 
                        "<b>Bobba.io Emulator</b><br><br>" +
                        "Currently loaded rooms: <b>" + loadedRooms + "</b><br><br>" +
                        "<b>Credits</b><br><br>" + credits
                    )
                );
                return true;
            }
            case UPDATE_CATALOG: {
            	try {
					BobbaEnvironment.getGame().getCatalogue().re_initialize(currentUser.getUser());
					BobbaEnvironment.getGame().getItemManager().initialize();
            	} catch (SQLException e) {
            		LOGGER.error("Catalog can't updated because: ", e);
				}
            	BobbaEnvironment.getGame().getCatalogue().serializePage(currentUser.getUser(), -1);
            	currentUser.getUser().getClient().sendMessage(new HandleAlert(true, "Der Shop wurde aktualisiert!"));
                return true;
            }
            case UNLOAD: {
            	List<RoomUser> currentUsers = BobbaEnvironment.getGame().getRoomManager().getLoadedRoom(currentUser.getRoom().getRoomData().getId()).getRoomUserManager().getUsers();
            	for (RoomUser user : currentUsers) {
            		BobbaEnvironment.getGame().getRoomManager().handleUserLeaveRoom(user.getUser());
            	}
                return true;
            }
            case ALERT: {
            	if (currentUser.getUser().getRank() >= 3) {
                    String Sentence = "";
                    if (args.length > 1) {
                        for (int lol = 1; lol < args.length; lol++) {
                            Sentence += args[lol] + " ";
                        }

                        List<RoomUser> currentUsers = null;
                        
                        currentUsers = new ArrayList<>();
                        for (Room room : BobbaEnvironment.getGame().getRoomManager().getLoadedRooms()) {
                        	currentUsers.addAll(room.getRoomUserManager().getUsers());
                        }

                        for (RoomUser user : currentUsers) {
                            user.getUser().getClient().sendMessage(new HandleAlert(true, Sentence));
                        }
                    }
                    return true;
            	}
            	return true;
            }
            case ROOM_ALERT: {
            	if(currentUser.getUser().getUsername() == currentUser.getRoom().getRoomData().getOwner() || currentUser.getUser().getRank() > 3) {
            		String Sentence = "";
                    if (args.length > 1) {
                        for (int lol = 1; lol < args.length; lol++) {
                            Sentence += args[lol] + " ";
                        }

                        List<RoomUser> currentUsers = null;   
                        currentUsers = BobbaEnvironment.getGame().getRoomManager().getLoadedRoom(currentUser.getRoom().getRoomData().getId()).getRoomUserManager().getUsers();
                        
                        for (RoomUser user : currentUsers) {
                            user.getUser().getClient().sendMessage(new HandleAlert(true, Sentence));
                        }
                    }
            	} else {
            		currentUser.chat("This is not your Room !!");
            	}
                return true;
            }
            case PICK_ALL: {
            	if(currentUser.getUser().getUsername() == BobbaEnvironment.getGame().getRoomManager().getLoadedRoom(currentUser.getRoom().getRoomData().getId()).getRoomData().getOwner()) {
            		currentUser.getRoom().getRoomItemManager().handlePickAll(currentUser);
            	}
                return true;
            }
            case COORDS: {
            	currentUser.chat("My coords: " + currentUser.getX() + ", " + currentUser.getY() + ", " + TextHandling.getFloatString(currentUser.getZ()) + ", Rot: " + currentUser.getRot());
            	return true;
            }
            case USER_PULL: {
            	Point targetPoint = null;
                switch (currentUser.getRot()) {
                    case 4:targetPoint = new Point(currentUser.getX(), currentUser.getY() + 2);break;
                    case 0:targetPoint = new Point(currentUser.getX(), currentUser.getY() - 2);break;
                    case 6:targetPoint = new Point(currentUser.getX() - 2, currentUser.getY());break;
                    case 2:targetPoint = new Point(currentUser.getX() + 2, currentUser.getY());break;
                }

                if (targetPoint != null) {
                    List<RoomUser> targetUsers = currentUser.getRoom().getGameMap().getRoomUsersForSquare(targetPoint);
                    if (!targetUsers.isEmpty()) {
                        RoomUser targetRoomUsers = targetUsers.get(0);
                        switch (currentUser.getRot()) {
                            case 0:targetRoomUsers.moveTo(targetRoomUsers.getX(), targetRoomUsers.getY() + 1);break;
                            case 4:targetRoomUsers.moveTo(targetRoomUsers.getX(), targetRoomUsers.getY() - 1);break;
                            case 2:targetRoomUsers.moveTo(targetRoomUsers.getX() - 1, targetRoomUsers.getY());break;
                            case 6:targetRoomUsers.moveTo(targetRoomUsers.getX() + 1, targetRoomUsers.getY());break;
                        }
                        currentUser.chat("*pulls " + targetRoomUsers.getUser().getUsername() + "*");
                    }
                }
                return true;
            }
            case USER_PUSH: {
            	RoomUser targetRoomUsers = null;
                if (args.length > 1) {
                    targetRoomUsers = currentUser.getRoom().getRoomUserManager().getUser(args[1]);
                } else {
                    Point point = null;
                    switch (currentUser.getRot()) {
                        case 4: point = new Point(currentUser.getX(), currentUser.getY() + 1); break;
                        case 0: point = new Point(currentUser.getX(), currentUser.getY() - 1); break;
                        case 6: point = new Point(currentUser.getX() - 1, currentUser.getY()); break;
                        case 2: point = new Point(currentUser.getX() + 1, currentUser.getY()); break;
                        default: break;
                    }
                    if (point != null) {
                        List<RoomUser> userList = currentUser.getRoom().getGameMap().getRoomUsersForSquare(point);
                        if (!userList.isEmpty()) {
                            targetRoomUsers = userList.get(0);
                        }
                    }
                }
                if (targetRoomUsers != null) {
                    int newX = targetRoomUsers.getX();
                    int newY = targetRoomUsers.getY();
                    switch (currentUser.getRot()) {
                        case 4: newY++; break;
                        case 0: newY--; break;
                        case 6: newX--; break;
                        case 2: newX++; break;
                        case 3: newX++; newY++; break;
                        case 1: newX++; newY--; break;
                        case 7: newX--; newY--; break;
                        case 5: newX--; newY++; break;
                        default: break;
                    }
                    targetRoomUsers.moveTo(newX, newY);
                    currentUser.chat("*pushes " + targetRoomUsers.getUser().getUsername() + "*");
                }
                return true;
            }
            case COMMANDS: {
                StringBuilder sb = new StringBuilder();
                for (Command cmd : Command.values()) {
                    sb.append("<b>" + cmd.getCommandString() + "</b>");
                    sb.append(": ");
                    sb.append(cmd.getDescription());
                    sb.append("<br><br>");
                }
                currentUser.getUser().getClient().sendMessage(new HandleAlert(true, sb.toString()));
                return true;
            }
            
        }
		return false;
    }
}

