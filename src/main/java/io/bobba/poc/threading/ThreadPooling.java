/*
 * CREATED BY ARCTURUS COMMUNITY
 * GITHUB: https://github.com/billsonnn/Arcturus-Community
 */

package io.bobba.poc.threading;

import io.bobba.poc.BobbaEnvironment;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ThreadPooling {
	 private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPooling.class);

	    public final int threads;
	    private final ScheduledExecutorService scheduledPool;
	    private volatile boolean canAdd;

	    public ThreadPooling(Integer threads) {
	        this.threads = threads;
	        this.scheduledPool = new HabboExecutorService(this.threads, new DefaultThreadFactory("HabExec"));
	        this.canAdd = true;
	        LOGGER.info("Thread Pool -> Loaded!");
	    }

	    public ScheduledFuture run(Runnable run) {
	        try {
	            if (this.canAdd) {
	                return this.run(run, 0);
	            } else {
	                if (BobbaEnvironment.isShuttingDown) {
	                    run.run();
	                }
	            }
	        } catch (Exception e) {
	            LOGGER.error("Caught exception", e);
	        }

	        return null;
	    }

	    public ScheduledFuture run(Runnable run, long delay) {
	        try {
	            if (this.canAdd) {
	                return this.scheduledPool.schedule(() -> {
	                    try {
	                        run.run();
	                    } catch (Exception e) {
	                        LOGGER.error("Caught exception", e);
	                    }
	                }, delay, TimeUnit.MILLISECONDS);
	            }
	        } catch (Exception e) {
	            LOGGER.error("Caught exception", e);
	        }

	        return null;
	    }

	    public void shutDown() {
	        this.canAdd = false;
	        this.scheduledPool.shutdownNow();

	        LOGGER.info("Threading -> Disposed!");
	    }

	    public void setCanAdd(boolean canAdd) {
	        this.canAdd = canAdd;
	    }

	    public ScheduledExecutorService getService() {
	        return this.scheduledPool;
	    }

		

}
