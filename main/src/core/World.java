package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.awt.Point;
import java.util.Random;
import java.util.List;
import java.util.*;

public class World {

    private final TETile[][] myWorld;
    public final int width;
    public final int height;
    private final Random rand;
    private final List<Room> rooms;
    public Room townHall;
    public Player Farmer;

    /**
     * This will create a new world with a 16:10 aspect ratio
     * which I just based on the aspect ratio of a 13in laptop
     * screen.
     * The world will be completely filled with grass tiles
     *
     * @param seed Used to generate all random functionalities
     */

    public World(long seed) {
        // We will use the aspect ratio of 16:10 for our world size
        this.rand = new Random(seed);
        this.width = 80;//(rand.nextInt(9) + 5) * 20;
        this.height = (int) Math.round(width * (10.0/16.0));
        this.myWorld = new TETile[this.width][this.height];
        this.rooms = new ArrayList<>();

        generateWorld(this.rand);
    }

    /**
     * We will use generateWorld to place our rooms and hallways randomly.
     *
     * @param rand used to generate the same behavior for given seed
     */

    public void generateWorld(Random rand) {

        // Makes entire world grass
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                myWorld[x][y] = Tileset.GRASS;
            }
        }

        // Generates 5 to 15 rooms
        int numRooms =  rand.nextInt(10) + 5;
        int maxSize = Math.max(6, width / 5);

        for (int i = 0; i < numRooms; i++) {
            // Random room size between 4 and maxSize
            int roomSize = rand.nextInt(maxSize - 3) + 4;
            try {
                Room newRoom = new Room(this, roomSize, rand);
                rooms.add(newRoom); //needed to add this to save the new rooms
                if (townHall == null || newRoom.size() > townHall.size()) {
                    this.townHall = newRoom;
                }

            } catch (IllegalStateException e) {
                // Room failed to generate — skip it
                System.out.println("Skipped a room: couldn't place after 20 tries.");
            }
        }

        // Hallway generation
        connectRooms();

        // Generate Farmer
        Farmer = new Player(this, townHall.getCenter());
    }


    private void connectRooms() {
        if (rooms.size() < 2) {
            return;
        }

        // Creates a new Hallway
        Hallway hallwayGenerator = new Hallway(this, rand);
        boolean[][] connected = new boolean[rooms.size()][rooms.size()];
        List<Integer> connectedIndices = new ArrayList<>();
        List<Integer> unconnectedIndices = new ArrayList<>();

        connectedIndices.add(0);
        for (int i = 1; i < rooms.size(); i++) {
            unconnectedIndices.add(i);
        }
        //min spanning tree, keep going until no unconnected indices left
        while (!unconnectedIndices.isEmpty()) {
            int bestConnectedIdx = -1;
            int bestUnconnectedIdx = -1;
            double minDistance = Double.MAX_VALUE;

            for (int connectedIdx : connectedIndices) {
                Room connectedRoom = rooms.get(connectedIdx);

                for (int i = 0; i < unconnectedIndices.size(); i++) {
                    int unconnectedIdx = unconnectedIndices.get(i);
                    Room unconnectedRoom = rooms.get(unconnectedIdx);

                    double distance = getDistance(connectedRoom.getCenter(), unconnectedRoom.getCenter());
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestConnectedIdx = connectedIdx;
                        bestUnconnectedIdx = unconnectedIdx;
                    }
                }
            }

            if (bestConnectedIdx != -1 && bestUnconnectedIdx != -1) {
                Room sourceRoom = rooms.get(bestConnectedIdx);
                Room targetRoom = rooms.get(bestUnconnectedIdx);

                boolean success = connectRoomPair(sourceRoom, targetRoom, hallwayGenerator);

                if (success) {
                    connected[bestConnectedIdx][bestUnconnectedIdx] = true;
                    connected[bestUnconnectedIdx][bestConnectedIdx] = true;
                    connectedIndices.add(bestUnconnectedIdx);
                    unconnectedIndices.remove(Integer.valueOf(bestUnconnectedIdx));
                } else {
                    //if cannot connect, force a connection by brute forcing (trying every possible wall tile from source)
                    List<Point> sourceWalls = new ArrayList<>(sourceRoom.getEdgeWallTiles());
                    List<Point> targetWalls = new ArrayList<>(targetRoom.getEdgeWallTiles());

                    boolean forcedConnection = false;
                    for (Point sourceWall : sourceWalls) {
                        for (Point targetWall : targetWalls) {
                            if (hallwayGenerator.createHallway(sourceWall, targetWall, rooms)) {
                                connected[bestConnectedIdx][bestUnconnectedIdx] = true;
                                connected[bestUnconnectedIdx][bestConnectedIdx] = true;
                                forcedConnection = true;
                                break;
                            }
                        }
                        if (forcedConnection) break;
                    }
                    //break infinite loops
                    connectedIndices.add(bestUnconnectedIdx);
                    unconnectedIndices.remove(Integer.valueOf(bestUnconnectedIdx));
                }
            }
        }
        int additionalConnections = rooms.size() / 4 + rand.nextInt(rooms.size() / 4);

        for (int i = 0; i < additionalConnections; i++) {
            int attempts = 0;
            while (attempts < 20) {  // Limit attempts to prevent infinite loop
                int room1Idx = rand.nextInt(rooms.size());
                int room2Idx = rand.nextInt(rooms.size());

                if (room1Idx != room2Idx && !connected[room1Idx][room2Idx]) {
                    Room room1 = rooms.get(room1Idx);
                    Room room2 = rooms.get(room2Idx);

                    if (connectRoomPair(room1, room2, hallwayGenerator)) {
                        connected[room1Idx][room2Idx] = true;
                        connected[room2Idx][room1Idx] = true;
                        break;
                    }
                }
                attempts++;
            }
        }
        eliminateDeadEnds();
    }

    //connects two rooms. as seen before, we iterate through all the rooms to ensure connectivity
    private boolean connectRoomPair(Room sourceRoom, Room targetRoom, Hallway hallwayGenerator) {
        List<Point> sourceWalls = new ArrayList<>(sourceRoom.getEdgeWallTiles());
        final Point targetCenter = targetRoom.getCenter();
        Collections.sort(sourceWalls, (p1, p2) -> {
            double d1 = getDistance(p1, targetCenter);
            double d2 = getDistance(p2, targetCenter);
            return Double.compare(d1, d2);
        });
        int attempts = Math.min(5, sourceWalls.size());
        for (int i = 0; i < attempts; i++) {
            if (hallwayGenerator.createHallway(sourceWalls.get(i), targetCenter, rooms)) {
                return true;
            }
        }
        Collections.shuffle(sourceWalls, rand);
        attempts = Math.min(10, sourceWalls.size());
        for (int i = 0; i < attempts; i++) {
            if (hallwayGenerator.createHallway(sourceWalls.get(i), targetCenter, rooms)) {
                return true;
            }
        }

        return false;
    }

    //was producing lots of dead ends so this gets rid of any
    private void eliminateDeadEnds() {
        boolean foundDeadEnd;
        do {
            foundDeadEnd = false;

            for (int x = 1; x < myWorld.length - 1; x++) {
                for (int y = 1; y < myWorld[0].length - 1; y++) {
                    if (myWorld[x][y] == Tileset.FLOOR) {
                        int floorCount = 0;
                        if (myWorld[x+1][y] == Tileset.FLOOR) floorCount++;
                        if (myWorld[x-1][y] == Tileset.FLOOR) floorCount++;
                        if (myWorld[x][y+1] == Tileset.FLOOR) floorCount++;
                        if (myWorld[x][y-1] == Tileset.FLOOR) floorCount++;

                        if (floorCount == 1) {
                            boolean extended = tryExtendDeadEnd(x, y);

                            if (!extended) {
                                myWorld[x][y] = Tileset.GRASS;
                            }
                            foundDeadEnd = true;
                        }
                    }
                }
            }
        } while (foundDeadEnd);
    }

    private boolean tryExtendDeadEnd(int x, int y) {
        int connDx = 0, connDy = 0;
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (isInBounds(nx, ny) && myWorld[nx][ny] == Tileset.FLOOR) {
                connDx = dir[0];
                connDy = dir[1];
                break;
            }
        }
        int[][] perpDirs = new int[2][2];
        if (connDx == 0) {
            perpDirs[0] = new int[]{1, 0};
            perpDirs[1] = new int[]{-1, 0};
        } else {
            perpDirs[0] = new int[]{0, 1};
            perpDirs[1] = new int[]{0, -1};
        }

        Collections.shuffle(Arrays.asList(perpDirs), rand);

        for (int[] dir : perpDirs) {
            int dx = dir[0];
            int dy = dir[1];
            int newX = x + dx;
            int newY = y + dy;
            int steps = 0;
            int maxSteps = Math.max(myWorld.length, myWorld[0].length) / 3;

            while (isInBounds(newX, newY) && steps < maxSteps) {
                if (myWorld[newX][newY] == Tileset.FLOOR) {
                    int pathX = x;
                    int pathY = y;
                    while (pathX != newX || pathY != newY) {
                        myWorld[pathX][pathY] = Tileset.FLOOR;
                        pathX += dx;
                        pathY += dy;
                    }
                    return true;
                }
                if (myWorld[newX][newY] == Tileset.WALL) {
                    break;
                }
                newX += dx;
                newY += dy;
                steps++;
            }
        }
        return false;
    }

    private void addWallsAroundTile(int x, int y) {
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};

        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];

            if (isInBounds(nx, ny) && myWorld[nx][ny] == Tileset.GRASS) {
                myWorld[nx][ny] = Tileset.WALL;
            }
        }
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < myWorld.length && y >= 0 && y < myWorld[0].length;
    }

    private double getDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.getX() - p2.getX(), 2) + Math.pow(p1.getY() - p2.getY(), 2));
    }

    //basically goes back to all of the floor tiles and makes sure that it is surrounded by another floor or a wall.
    private void finalizeWallPadding() {
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                if (myWorld[x][y] == Tileset.FLOOR) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (Math.abs(dx) + Math.abs(dy) == 1) {
                                int nx = x + dx;
                                int ny = y + dy;
                                if (myWorld[nx][ny] == Tileset.GRASS) {
                                    myWorld[nx][ny] = Tileset.WALL;
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Checks if the tile at the give (x,y) is a grass tile.
     *
     * @param x x-coordinate to check.
     * @param y y-coordinate to check.
     * @return true if tile is grass and within bounds
     */

    public boolean isGrass(int x, int y) {

        // edge case when x or y are out of bounds
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }

        return myWorld[x][y].equals(Tileset.GRASS);
    }

    /**
     * Checks a straight line of tiles or diagonal by specifying
     * direction with dx and dy.
     * Verifies that the length of tiles are all grass.
     *
     * @param xProp Propagation x-coordinate.
     * @param yProp Propagation y-coordinate.
     * @param length Number of tiles to check in the given direction.
     * @param dx step in the x-direction. Usually, 1 (Right) or 0.
     * @param dy step in the y-direction. Usually, 1 (Up) or 0.
     * @return The final x or y coordinate of the line if valid or -1 if any tile is not grass.
     */

    public int boundCheckDirection(int xProp, int yProp, int length, int dx, int dy) {
        for (int i = 0; i < length; i++) {
            int x = xProp + (dx * i);
            int y = yProp + (dy * i);
            if (!isGrass(x, y)) {
                return -1;
            }
        }

        int endX = xProp + dx * (length - 1);
        int endY = yProp + dy * (length - 1);

        if (dx != 0) {
            return endX;
        } else {
            return endY;
        }
    }

    /**
     * This is the unbounded version. Starts from (xProp, yProp) and
     * walks in the given direction until hitting
     * a non-grass tile or edge of the map.
     *
     * @param xProp Propagation x-coordinate.
     * @param yProp Propagation y-coordinate.
     * @param dx step in the x-direction. Usually, 1 (Right) or 0.
     * @param dy step in the y-direction. Usually, 1 (Up) or 0.
     * @return The number of steps taken in specified direction before finding invalid tile.
     */

    public int unboundCheckDirection(int xProp, int yProp, int dx, int dy) {
        int steps = 0;
        int x = xProp;
        int y = yProp;

        /* Stops when a tile is either not grass or if the next
           step is off the map */
        while (isGrass(x,y)) {
            steps++;
            x += dx;
            y += dy;
            if (x < 0 || x >= myWorld.length || y < 0 || y >= myWorld[0].length) {
                break; //Stops if reaches edge of world
            }
        }

        if (steps > 0) {
            return steps;
        } else {
            return -1;
        }
    }

    /**
     * returns 2D tile array of the current world.
     * Good for reading and modifying world from other classes.
     *
     * @return world.
     */

    public TETile[][] getWorld() {
        return myWorld;
    }
    // build your own world!

    public List<Room> getRooms() {
        return rooms;
    }

}
