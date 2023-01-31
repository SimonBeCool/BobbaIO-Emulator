package io.bobba.poc.core.rooms.gamemap.pathfinding.dream;

import io.bobba.poc.core.rooms.gamemap.SqState;

public class ModelInfo {
	private int MaxX;
	private int MaxY;
	private SqState[][] gameMap;

	public ModelInfo(int pMaxX, int pMaxY, SqState[][] pGameMap) {
	    MaxX = pMaxX;
	    MaxY = pMaxY;
	    gameMap = pGameMap;
	}

	public int getMaxX() {
	    return MaxX;
	}

	public int getMaxY() {
	    return MaxY;
	}

	public SqState getState(int x, int y) {
	    return gameMap[x][y];
	}
}