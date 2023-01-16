package io.bobba.poc.communication.outgoing.alert;

import io.bobba.poc.communication.protocol.ServerMessage;
import io.bobba.poc.communication.protocol.ServerOpCodes;

public class HandleAlert extends ServerMessage {
	public HandleAlert(Boolean state, String msg) {
		super(ServerOpCodes.STAFF_ALERT);
		appendBoolean(state);
		appendString(msg);
	}
}