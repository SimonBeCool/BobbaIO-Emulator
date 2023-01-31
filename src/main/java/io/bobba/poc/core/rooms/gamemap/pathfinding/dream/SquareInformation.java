package io.bobba.poc.core.rooms.gamemap.pathfinding.dream;

public class SquareInformation {

    private SquarePoint[] mPos;
    private SquarePoint mPoint;

    public SquareInformation(int pX, int pY, SquarePoint pTarget, ModelInfo pMap, boolean pUserOverride, boolean CalculateDiagonal) {
        mPoint = new SquarePoint(pX, pY, pTarget.getX(), pTarget.getY(), pMap.getState(pX, pY), pUserOverride);
        mPos = new SquarePoint[8];

        int[][] offsets = {{0, -1}, {-1, -1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}};

        for (int i = 0; i < 8; i++) {
            int x = pX + offsets[i][0];
            int y = pY + offsets[i][1];

            if (CalculateDiagonal || (x == pX || y == pY)) {
                mPos[i] = new SquarePoint(x, y, pTarget.getX(), pTarget.getY(), pMap.getState(x, y), pUserOverride);
            }
        }
    }

    public SquarePoint getPoint() {
        return mPoint;
    }

    public SquarePoint pos(int val) {
        return mPos[val];
    }
}
