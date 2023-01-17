package io.bobba.poc.communication.outgoing.rooms;

import io.bobba.poc.communication.protocol.ServerMessage;
import io.bobba.poc.communication.protocol.ServerOpCodes;

public class LayComposer extends ServerMessage {
	public LayComposer(int userId) {
		super(ServerOpCodes.PLAYER_LAY);
		appendInt(userId);
	}
}
