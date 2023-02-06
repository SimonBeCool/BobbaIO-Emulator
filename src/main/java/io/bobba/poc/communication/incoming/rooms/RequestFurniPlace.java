package io.bobba.poc.communication.incoming.rooms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import io.bobba.poc.BobbaEnvironment;

import io.bobba.poc.communication.incoming.IIncomingEvent;
import io.bobba.poc.communication.protocol.ClientMessage;
import io.bobba.poc.core.gameclients.GameClient;
import io.bobba.poc.core.items.BaseItem;
import io.bobba.poc.core.rooms.users.RoomUser;
import io.bobba.poc.core.rooms.Room;
import io.bobba.poc.core.rooms.items.RoomItem;
import io.bobba.poc.core.rooms.roomdata.RoomData;

@SuppressWarnings("unused")
public class RequestFurniPlace implements IIncomingEvent {
    private static final String INSERT_QUERY = "INSERT INTO room_furnis (room_id, item_id, x, y, rot) VALUES (?, ?, ?, ?, ?)";

    @Override
    public void handle(GameClient client, ClientMessage request) {
        int itemId = request.popInt();
        int x = request.popInt();
        int y = request.popInt();
        int rot = request.popInt();

        RoomUser user = client.getUser().getCurrentRoomUser();
        if (user == null) return;
      
        user.getRoom().getRoomItemManager().handleItemPlacement(itemId, x, y, rot, user);
        
        RoomItem furni = user.getRoom().getRoomItemManager().getItem(itemId);
        int furniBaseId = furni.getBaseItem().getBaseId();
        int roomId = user.getRoom().getRoomData().getId();

        try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_QUERY)) {
            statement.setInt(1, roomId);
            statement.setInt(2, furniBaseId);
            statement.setInt(3, x);
            statement.setInt(4, y);
            statement.setInt(5, rot);
            statement.execute();
        } catch (SQLException e) {
            System.out.println("Furni cannot be placed: " + e);
        }
    }
}