package io.bobba.poc.threading.runnables;

import java.util.Random;
 
import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.core.rooms.Room;
import io.bobba.poc.core.rooms.items.RoomItem;


public class RandomDiceNumber implements Runnable {
    private final RoomItem item;
    private final Room room;
    private final int maxNumber;
    private int result;

    public RandomDiceNumber(RoomItem item, Room room, int maxNumber) {
        this.item = item;
        this.room = room;
        this.maxNumber = maxNumber;
        this.result = -1;
    }

    @Override
    public void run() {
    	Random rn = new Random();
        if (this.result <= 0)
        	this.result = (int)Math.floor(Math.random() * (maxNumber - 1 + 1) + 1); // TODO DO IT BETTER
        
        this.item.setState(this.result);
        this.item.updateState();
        BobbaEnvironment.getThreading().run(this.item);
    }
}