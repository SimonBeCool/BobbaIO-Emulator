package io.bobba.poc.core.users;

public class Coord {
    public int x;
    public int y;
    
    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public int hashCode() {
        return x ^ y;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coord) {
            Coord other = (Coord) obj;
            return x == other.x && y == other.y;
        }
        return false;
    }
}