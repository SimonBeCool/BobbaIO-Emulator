package io.bobba.poc.threading.runnables;
 
import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.core.rooms.items.RoomItem;


public class HabboWheelInteractor implements Runnable {
    private final RoomItem item;
    private int result;

    public HabboWheelInteractor(RoomItem item) {
        this.item = item;
        this.result = -1;
    }

    @Override
    public void run() {
        if (this.result <= 0)
        	this.result = this.item.getRandomNumber(1, 10);
        
        this.item.setState(this.result);
        this.item.updateState();
        BobbaEnvironment.getThreading().run(this.item);
    }
}