package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Room {
    private final int xProp;
    private final int yProp;
    private final Point center;
    private final int mySize;
    private final List<Point> edgeWallTiles;

    /**
     * Constructs a square room of given size and places it randomly in the map
     * the constructor attempts to do this 10 times before skipping its placement
     *
     * The room is built with a floor interior and walls on the perimeter
     * edge wall tiles are saved in a list of points and exclude the corners
     * for future use in hallway generation.
     *
     * @param myWorld will be called as this in world function
     * @param size randomly generated in world function
     * @param rand propagates rand object to room randomness functionalities
     */

    public Room(World myWorld, int size, Random rand) {

        this.mySize = size;
        this.edgeWallTiles = new ArrayList<>();

        TETile[][] world = myWorld.getWorld();
        // Was made aware of point by chatGPT
        int worldWidth = world.length;
        int worldHeight = world[0].length;

        boolean valid = false;
        int x = 0;
        int y = 0;
        int count = 0;

        while(!valid && count < 20) {
            x = rand.nextInt(worldWidth);
            y = rand.nextInt(worldHeight);

            // Checks if propagation tile was placed in an existing room
            if (!myWorld.isGrass(x,y)) {
                continue;
            }

            if (!checkTileBuffer(x, y, size, world)) {
                count++;
                continue; // Try a new spot
            }

            if (checkTileBuffer(x, y, size, world)) {
                valid = true;
            }
        }

        if (!valid) {
            throw new IllegalStateException("Failed to place room after 20 attempts.");
        }

        // Save room Propagation position
        this.xProp = x;
        this.yProp = y;
        this.center = new Point(xProp + size / 2, yProp + size / 2);

        // Building room in valid space
        for (int i = xProp; i < xProp + size; i++) {
            for (int j = yProp; j < yProp + size; j++) {
                boolean edge = (i == xProp || i == xProp + size -1 || j == yProp || j == yProp + size - 1);
                boolean corner = (i == xProp || i == xProp + size - 1) && (j == yProp || j == yProp + size - 1);


                if (edge) {
                    world[i][j] = Tileset.WALL; // Perimeter
                    if (!corner) {
                        edgeWallTiles.add(new Point(i, j));
                    }
                } else {
                    world[i][j] = Tileset.FLOOR; // Inside
                }
            }
        }
    }

    public List<Point> getEdgeWallTiles() {
        return edgeWallTiles;
    }

    public int getXProp() {
        return xProp;
    }

    public int getYProp() {
        return yProp;
    }

    public int size() {
        return mySize;
    }

    public Point getCenter() {
        return center;
    }

    private boolean checkTileBuffer(int x, int y, int size, TETile[][] world) {
        int worldWidth = world.length;
        int worldHeight = world[0].length;

        for (int i = x - 1; i <= x + size; i++) {
            for (int j = y - 1; j <= y + size; j++) {
                // Skip out-of-bounds tiles (just treat them as unsafe)
                if (i < 0 || j < 0 || i >= worldWidth || j >= worldHeight) {
                    return false;
                }

                // If any tile in the 1-tile buffer isn't grass, reject
                if (world[i][j] != Tileset.GRASS) {
                    return false;
                }
            }
        }
        return true;
    }
}


