package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.awt.*;

public class Player {
    private final World world;
    private final TETile[][] myWorld;
    private Point location;
    private int harvest;

    public Player(World world, Point start) {
        this.world = world;
        this.myWorld = world.getWorld();
        this.location = start;
        this.harvest = 0;

        myWorld[start.x][start.y] = Tileset.AVATAR;
    }

    public void movePlayer(char key) {
        int dx = 0;
        int dy = 0;

        if (key == 'W') {
            dy++;
        }
        if (key == 'A') {
            dx--;
        }
        if (key == 'S') {
            dy--;
        }
        if (key == 'D') {
            dx++;
        }

        Point nextLocation = new Point(location.x + dx, location.y + dy);

        updateLocation(this.location, nextLocation);
    }

    private void updateLocation(Point start, Point end) {
        if (!validPos(end)) {
            return;
        }

        this.location = end;
        myWorld[end.x][end.y] = Tileset.AVATAR;
        myWorld[start.x][start.y] = Tileset.FLOOR;
    }

    private boolean validPos(Point end) {

        TETile nextTile = myWorld[end.x][end.y];

        // Out of bounds condition
        if (end.x < 0 || end.x >= myWorld.length || end.y < 0 || end.y >= myWorld[0].length) {
            return false;
        }
        // Wall condition
        if (nextTile == Tileset.WALL) {
            return false;
        }
        // Grass condition
        if (nextTile == Tileset.GRASS) {
            harvest++;
            return true;
        }
        // Floor condition
        return true;
    }

    public int getHarvest() {
        return harvest;
    }


}
