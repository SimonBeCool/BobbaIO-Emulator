package io.bobba.poc.communication.incoming.rooms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.incoming.IIncomingEvent;
import io.bobba.poc.communication.protocol.ClientMessage;
import io.bobba.poc.core.gameclients.GameClient;
import io.bobba.poc.core.rooms.users.RoomUser;

@SuppressWarnings("unused")
public class RequestChangeLooks implements IIncomingEvent {
	@Override
	public void handle(GameClient client, ClientMessage request) {
		RoomUser user = client.getUser().getCurrentRoomUser();
		if (user != null){
			String look = request.popString();
			String lookRequest = "UPDATE users SET look = ? WHERE id = ?";
			try (
				Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
				PreparedStatement statement = connection.prepareStatement(lookRequest);
			) {
				statement.setString(1, look);
				statement.setInt(2, user.getUser().getId());
				statement.executeUpdate();
			} catch (SQLException e) {
				System.out.println("Error updating user look: " + e);
			}
			user.getUser().setLook(look);
		}
	}
}
