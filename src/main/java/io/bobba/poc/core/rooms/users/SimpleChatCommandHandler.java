package io.bobba.poc.core.rooms.users;

import java.awt.Point;
import java.util.List;
import java.util.regex.Pattern;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePageComposer;
import io.bobba.poc.communication.outgoing.catalogue.CataloguePurchaseInformationComposer;
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
                        if (currentUser.getRot() % 2 == 1) {
                            currentUser.setRot(currentUser.getRot() - 1);
                        }
                        currentUser.addStatus("sit", "0.55");
                        currentUser.setNeedsUpdate(true);
                    } else {
                        currentUser.getRoom().getRoomUserManager().updateUserStatus(currentUser);
                    }
                }
                return true;

                case "maps": {
                    currentUser.getRoom().getGameMap().generateMaps();
                    return true;
                }
                
                case "about":{
                	List<Room> rooms = BobbaEnvironment.getGame().getRoomManager().getLoadedRooms();
                	int room_counter = 0;
                	for (Room room : rooms) {
                		room_counter++;
                	}
                	currentUser.getUser().getClient().sendMessage(new HandleAlert(true, "<b>Bobba.io Emulator</b> <br><br> Derzeitige geladene Räume: <b>"+ room_counter + "</b><br><br> <b>Credits</b><br><br> Josedn - <a href='https://github.com/Josedn'>Github</a> <br> Meliodas - <a href='https://github.com/SimonBeCool'>Github</a><br>code-rain002 - <a href='https://github.com/code-rain002'>Github</a>"));
                	return true;
                }
                
                case "update_catalog":{
                	BobbaEnvironment.getGame().getCatalogue().re_initialize(currentUser.getUser());
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
                
                case "roomalert": {
                	String Sentence = "";
                	if(args.length > 1) {
                		for(int lol = 0; lol < args.length; lol++) {
                			String arg = args[lol] + " ";
                		    Sentence = Sentence + arg;
                		}
                		
                		List<RoomUser> currentUsers = BobbaEnvironment.getGame().getRoomManager().getLoadedRoom(currentUser.getRoom().getRoomData().getId()).getRoomUserManager().getUsers();
	                	for (RoomUser user : currentUsers) {
	                		user.getUser().getClient().sendMessage(new HandleAlert(true, Sentence.replaceAll("\\broomalert\\b", "")));
	                	}
                	}
                	return true;
                }
                
                
                case "pickall": {
                    currentUser.getRoom().getRoomItemManager().handlePickAll(currentUser);
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
                    Point point = null;
                    if (currentUser.getRot() == 4) {
                        point = new Point(currentUser.getX(), currentUser.getY() + 2);
                    }
                    if (currentUser.getRot() == 0) {
                        point = new Point(currentUser.getX(), currentUser.getY() - 2);
                    }
                    if (currentUser.getRot() == 6) {
                        point = new Point(currentUser.getX() - 2, currentUser.getY());
                    }
                    if (currentUser.getRot() == 2) {
                        point = new Point(currentUser.getX() + 2, currentUser.getY());
                    }
                    if (point != null) {
                        List<RoomUser> userList = currentUser.getRoom().getGameMap().getRoomUsersForSquare(point);
                        if (userList.size() > 0) {
                            targetRoomUser = userList.get(0);
                        }
                    }
                    if (targetRoomUser != null) {
                        if (currentUser.getRot() == 0) {
                            targetRoomUser.moveTo(targetRoomUser.getX(), targetRoomUser.getY() + 1);
                        }
                        if (currentUser.getRot() == 4) {
                            targetRoomUser.moveTo(targetRoomUser.getX(), targetRoomUser.getY() - 1);
                        }
                        if (currentUser.getRot() == 2) {
                            targetRoomUser.moveTo(targetRoomUser.getX() - 1, targetRoomUser.getY());
                        }
                        if (currentUser.getRot() == 6) {
                            targetRoomUser.moveTo(targetRoomUser.getX() + 1, targetRoomUser.getY());
                        }
                        currentUser.chat("*pulls " + targetRoomUser.getUser().getUsername() + "*");
                    }
                    return true;
                }

                case "push": {
                    if (args.length > 1) {
                        targetRoomUser = currentUser.getRoom().getRoomUserManager().getUser(args[1]);
                    } else {

                        Point point = null;
                        if (currentUser.getRot() == 4) {
                            point = new Point(currentUser.getX(), currentUser.getY() + 1);
                        }
                        if (currentUser.getRot() == 0) {
                            point = new Point(currentUser.getX(), currentUser.getY() - 1);
                        }
                        if (currentUser.getRot() == 6) {
                            point = new Point(currentUser.getX() - 1, currentUser.getY());
                        }
                        if (currentUser.getRot() == 2) {
                            point = new Point(currentUser.getX() + 1, currentUser.getY());
                        }
                        if (point != null) {
                            List<RoomUser> userList = currentUser.getRoom().getGameMap().getRoomUsersForSquare(point);
                            if (userList.size() > 0) {
                                targetRoomUser = userList.get(0);
                            }
                        }
                    }
                    if (targetRoomUser != null) {
                        if ((targetRoomUser.getX() == currentUser.getX() - 1) || (targetRoomUser.getX() == currentUser.getX() + 1) || (targetRoomUser.getY() == currentUser.getY() - 1) || (targetRoomUser.getY() == currentUser.getY() + 1)) {
                            if (currentUser.getRot() == 4) {
                                targetRoomUser.moveTo(targetRoomUser.getX(), targetRoomUser.getY() + 1);
                            }

                            if (currentUser.getRot() == 0) {
                                targetRoomUser.moveTo(targetRoomUser.getX(), targetRoomUser.getY() - 1);
                            }

                            if (currentUser.getRot() == 6) {
                                targetRoomUser.moveTo(targetRoomUser.getX() - 1, targetRoomUser.getY());
                            }

                            if (currentUser.getRot() == 2) {
                                targetRoomUser.moveTo(targetRoomUser.getX() + 1, targetRoomUser.getY());
                            }

                            if (currentUser.getRot() == 3) {
                                targetRoomUser.moveTo(targetRoomUser.getX() + 1, targetRoomUser.getY() + 1);
                            }

                            if (currentUser.getRot() == 1) {
                                targetRoomUser.moveTo(targetRoomUser.getX() + 1, targetRoomUser.getY() - 1);
                            }

                            if (currentUser.getRot() == 7) {
                                targetRoomUser.moveTo(targetRoomUser.getX() - 1, targetRoomUser.getY() - 1);
                            }

                            if (currentUser.getRot() == 5) {
                                targetRoomUser.moveTo(targetRoomUser.getX() - 1, targetRoomUser.getY() + 1);
                            }

                            currentUser.chat("*pushes " + targetRoomUser.getUser().getUsername() + "*");
                        }
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
