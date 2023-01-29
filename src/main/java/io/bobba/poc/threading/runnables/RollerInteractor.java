package io.bobba.poc.threading.runnables;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.core.rooms.items.RoomItem;
import io.bobba.poc.core.rooms.users.RoomUser;

public class RollerInteractor implements Runnable {
	private final RoomUser user;
	private final int item_rotation;
	private final RoomItem item;
	
    public RollerInteractor(RoomUser user, int item_rotation, RoomItem item) {
        this.user = user;
        this.item_rotation = item_rotation;
        this.item = item;
    }

    @Override
    public void run() {
    	if (item_rotation == 0) {
    		user.moveTo(user.getX(), user.getY() - 1);
        }
        if (item_rotation == 4) {
            user.moveTo(user.getX(), user.getY() + 1);
        }
        if (item_rotation == 2) {
            user.moveTo(user.getX() + 1, user.getY());
        }
        if (item_rotation == 6) {
            user.moveTo(user.getX() - 1, user.getY());
        }
        this.item.setState(0);
        this.item.updateState();
        BobbaEnvironment.getThreading().run(this.user);
    }
}
