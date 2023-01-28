package io.bobba.poc.communication.outgoing.users;

import io.bobba.poc.communication.protocol.ServerMessage;
import io.bobba.poc.communication.protocol.ServerOpCodes;

public class UpdateHabboClubComposer extends ServerMessage {
	public UpdateHabboClubComposer(int amount) {
		super(ServerOpCodes.HABBOCLUB_DAYS);
		appendInt(amount);
	}
}
