package io.bobba.poc.core.rooms.users;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePageComposer;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePurchaseInformationComposer;
import io.bobba.poc.communication.outgoing.navigator.NavigatorLeaveRoomComposer;
import io.bobba.poc.communication.outgoing.rooms.PlayerRemoveComposer;
import io.bobba.poc.communication.outgoing.alert.HandleAlert;
import io.bobba.poc.core.catalogue.Catalogue;
import io.bobba.poc.core.catalogue.CatalogueItem;
import io.bobba.poc.core.catalogue.CataloguePage;
import io.bobba.poc.core.items.BaseItem;
import io.bobba.poc.core.rooms.Room;
import io.bobba.poc.misc.TextHandling;
import io.bobba.poc.misc.logging.Logging;

public class SimpleChatCommandHandler {
    private SimpleChatCommandHandler() {
    	
    }

    public static boolean parse(RoomUser currentUser, String input) {
        String[] args = input.split(" ");

        if (currentUser == null || currentUser.getUser() == null || currentUser.getUser().getClient() == null || currentUser.getRoom() == null)
            return false;
        RoomUser targetRoomUser = null;

        try {
            switch (args[0].toLowerCase()) {
	            case "sit": {
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
                
                case "maps": {
                    currentUser.getRoom().getGameMap().generateMaps();
                    return true;
                }
                
                case "about": {
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
                
                case "update_catalog":{
                	BobbaEnvironment.getGame().getCatalogue().re_initialize(currentUser.getUser());
                	BobbaEnvironment.getGame().getItemManager().initialize();
                	BobbaEnvironment.getGame().getCatalogue().serializePage(currentUser.getUser(), -1);
                	currentUser.getUser().getClient().sendMessage(new HandleAlert(true, "Der Shop wurde aktualisiert!"));
                	return true;
                }
                
                case "unload": {
                	List<RoomUser> currentUsers = BobbaEnvironment.getGame().getRoomManager().getLoadedRoom(currentUser.getRoom().getRoomData().getId()).getRoomUserManager().getUsers();
                	for (RoomUser user : currentUsers) {
                		BobbaEnvironment.getGame().getRoomManager().handleUserLeaveRoom(user.getUser());
                	}
                	return true;
                }
                
                case "alert":
                case "roomalert": {
                	String command = args[0].toLowerCase();
                    String Sentence = "";
                    if (args.length > 1) {
                        for (int lol = 1; lol < args.length; lol++) {
                            Sentence += args[lol] + " ";
                        }

                        List<RoomUser> currentUsers = null;
                        if (command.equals("roomalert")) {
                            currentUsers = BobbaEnvironment.getGame().getRoomManager().getLoadedRoom(currentUser.getRoom().getRoomData().getId()).getRoomUserManager().getUsers();
                        } else {
                            currentUsers = new ArrayList<>();
                            for (Room room : BobbaEnvironment.getGame().getRoomManager().getLoadedRooms()) {
                                currentUsers.addAll(room.getRoomUserManager().getUsers());
                            }
                        }

                        for (RoomUser user : currentUsers) {
                            user.getUser().getClient().sendMessage(new HandleAlert(true, Sentence));
                        }
                    }
                    return true;
                }
                
                case "pickall": {
                    currentUser.getRoom().getRoomItemManager().handlePickAll(currentUser);
                    return true;
                }
                
                case "teleport": {
                	// TODO
                	return true;
                }
                
                case "coords": {
                    currentUser.chat("My coords: " + currentUser.getX() + ", " + currentUser.getY() + ", " + TextHandling.getFloatString(currentUser.getZ()) + ", Rot: " + currentUser.getRot());
                    return true;
                }

                case "spawn": {
                    String itemName = args[1];
                    BaseItem item = BobbaEnvironment.getGame().getItemManager().findItem(itemName);
                    if (item != null) {
                        int rot = currentUser.getRot();
                        if (!item.getDirections().contains(rot))
                            rot = item.getDirections().get(0);
                        currentUser.getRoom().getRoomItemManager().addFloorItemToRoom(Catalogue.generateItemId(), currentUser.getX(), currentUser.getY(), currentUser.getZ(), rot, 0, item);
                    }
                    return true;
                }
                
                case "buy": {
                	 String itemName = args[1];
                     CatalogueItem item = BobbaEnvironment.getGame().getCatalogue().findItem(itemName);
                     if (item != null) {
                    	 BobbaEnvironment.getGame().getCatalogue().handlePurchase(currentUser.getUser(), item.getPageId(), item.getId());
                    	 return true;
                     }
                     return false;
                }

                case "pull": {
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

                case "push": {
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
            }

        } catch (Exception e) {
            Logging.getInstance().logError("Error handling command", e, SimpleChatCommandHandler.class);
        }

        return false;
    }

}
