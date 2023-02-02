package io.bobba.poc.communication.protocol;

import java.util.Locale;

public abstract class ServerMessage {
    private final char SEPARATOR = '|';
    private StringBuilder body;

    public ServerMessage(int id) {
        this.body = new StringBuilder();
        this.body.append(id);
    }

    private void appendToken(String token) {
        body.append(SEPARATOR).append(token);
    }

    public void appendInt(int i) {
        appendToken(String.valueOf(i));
    }

    public void appendBoolean(boolean b) {
        appendInt(b ? 1 : 0);
    }

    public void appendFloat(double d) {
        appendToken(String.format(Locale.US, "%.2f", d));
    }

    public void appendString(String str) {
        int tickets = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == SEPARATOR)
                tickets++;
        }
        appendInt(tickets);
        appendToken(str);
    }

    @Override
    public String toString() {
        return this.body.toString();
    }
}