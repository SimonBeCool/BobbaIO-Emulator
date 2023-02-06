package io.bobba.poc.communication.incoming.rooms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.incoming.IIncomingEvent;
import io.bobba.poc.communication.protocol.ClientMessage;
import io.bobba.poc.core.gameclients.GameClient;
import io.bobba.poc.core.rooms.users.RoomUser;

public class RequestChangeMotto implements IIncomingEvent {
    @Override
    public void handle(GameClient client, ClientMessage request) {

        RoomUser user = client.getUser().getCurrentRoomUser();
        if (user != null){
        	String motto = request.popString();
        	String update_motto = "UPDATE users SET motto = ? WHERE id = ?";
        	
        	try (
        			Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
        			PreparedStatement statement = connection.prepareStatement(update_motto);
        		) {
        			statement.setString(1, motto);
        			statement.setInt(2, user.getUser().getId());
        			
        			statement.executeUpdate();
        			
        		} catch (SQLException e) {
        			System.out.println("Error updating user motto: " + e);
        		}
        	user.getUser().setMotto(motto);
        }
    }
}
