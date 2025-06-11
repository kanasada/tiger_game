package core;

import tileengine.TETile;
import tileengine.Tileset;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The Hallway class is responsible for the generation of hallways
 * which we define as connected paths between two rooms
 * We use a semi random pathfinding algorithm to connect points
 *
 * The algorithm aims to avoid cutting through other rooms
 * and prevents excessive overlapping by checking for valid path
 * positions and avoiding paths that are too close together.
 */

public class Hallway {
    private final World world;
    private final Random rand;
    private final TETile[][] tiles;

    /**
     * Creates new Hallway generator which references world and rand
     * to build new paths.
     *
     * @param world The world containing modifiable Tile Grid
     * @param rand shared rand object for seed reproduction.
     */

    public Hallway(World world, Random rand) {
        this.world = world;
        this.rand = rand;
        this.tiles = world.getWorld();
    }

    /**
     * Attempts to create a hallway from start point to a target point.
     * hallway starts by indentifying a valid intitial direction
     * (adjacent to a floor tile)
     *and switches between ideal horizontal and vertical movement
     * to approach the target.
     *
     * Tries to avoid invalid paths and connect to another FLOOR tile
     * (Valid room or hallway)
     *
     * @param start propagation point random point on the edge of room
     * @param target final point to connect to (often center of another room)
     * @param rooms List of all rooms to avoid during creation
     * @return true if the hallway successfully connects to a target;
     * false otherwise
     */

    public boolean createHallway(Point start, Point target, List<Room> rooms) {
        int startX = (int) start.getX();
        int startY = (int) start.getY();
        int targetX = (int) target.getX();
        int targetY = (int) target.getY();

        if (startX == 0 || startY == 0 || startX == tiles.length - 1 || startY == tiles[0].length - 1) {
            return false;
        }

        // Convert wall to floor
        tiles[startX][startY] = Tileset.FLOOR;
        int dx = 0;
        int dy = 0;
        int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}};

        // Find initial direction to go in by location nearby floor
        // setting dx and dy to the opposite direction.
        for (int[] dir : directions) {
            int nx = startX + dir[0];
            int ny = startY + dir[1];
            int nx2 = startX + (2 * dir[0]);
            int ny2 = startY + (2 * dir[1]);

            // Checks neighbor and next neighbor
            if (isInBounds(nx, ny) && tiles[nx][ny] == Tileset.FLOOR) {
                if (isInBounds(nx2, ny2) && tiles[nx2][ny2] == Tileset.FLOOR) {
                    dx = -dir[0];
                    dy = -dir[1];
                    break;
                }
            }
        }

        // Removed dx==0 && dy==0 condition April 22 12:24AM - Kyle

        int currentX = startX + dx;
        int currentY = startY + dy;
        List<Point> path = new ArrayList<>();
        path.add(new Point(startX, startY));

        int maxSteps = 200;
        int steps = 0;
        boolean reachedTarget = false;
        boolean horizontalPhase = (dx != 0);

        while (steps < maxSteps && !reachedTarget) {
            steps++;

            //always checking if current position is valid
            if (!isInBounds(currentX, currentY)) {
                if (horizontalPhase) {
                    dx = 0;
                    dy = Integer.compare(targetY - currentY, 0);
                } else {
                    dx = Integer.compare(targetX - currentX, 0);
                    dy = 0;
                }
                horizontalPhase = !horizontalPhase;
                continue;
            }

            //check if there is a floor after this wall, if so it is a room
            if (tiles[currentX][currentY] == Tileset.WALL) {
                int nextX = currentX + dx;
                int nextY = currentY + dy;

                if (!isInBounds(nextX, nextY) || nextX == 0 || nextY == 0 ||
                        nextX == tiles.length - 1 || nextY == tiles[0].length - 1) {
                    break;
                }

                if (isInBounds(nextX, nextY) && tiles[nextX][nextY] == Tileset.FLOOR) {
                    tiles[currentX][currentY] = Tileset.FLOOR;
                    path.add(new Point(currentX, currentY));
                    reachedTarget = true;
                    break;
                }
            }
            //if the floor is another hallway
            if (tiles[currentX][currentY] == Tileset.FLOOR &&
                    !adjacentToPath(currentX, currentY, path)) {
                path.add(new Point(currentX, currentY));
                reachedTarget = true;
                break;
            }

            path.add(new Point(currentX, currentY));


            // Added check current + dx or dy April 22 12:33AM - Kyle
            if (tiles[currentX][currentY] == Tileset.GRASS ||
                    (tiles[currentX][currentY] == Tileset.WALL && tiles[currentX + dx][currentY + dy] != Tileset.WALL)) {
                tiles[currentX][currentY] = Tileset.FLOOR;
            }

            if (horizontalPhase && currentX == targetX) {
                dx = 0;
                dy = Integer.compare(targetY - currentY, 0);
                horizontalPhase = false;
            } else if (!horizontalPhase && currentY == targetY) {
                dx = Integer.compare(targetX - currentX, 0);
                dy = 0;
                horizontalPhase = true;
            }

            if (steps % 10 == 0 && rand.nextDouble() < 0.5) {
                if (horizontalPhase) {
                    dx = 0;
                    dy = Integer.compare(targetY - currentY, 0);
                } else {
                    dx = Integer.compare(targetX - currentX, 0);
                    dy = 0;
                }
                horizontalPhase = !horizontalPhase;
            }

            //when we cant move in same direction, turn
            int nextX = currentX + dx;
            int nextY = currentY + dy;

            if (!isValidHallwayPos(nextX, nextY) ||
                    (tiles[nextX][nextY] == Tileset.FLOOR && !adjacentToPath(nextX, nextY, path))) {

                if (horizontalPhase) {
                    dx = 0;
                    dy = Integer.compare(targetY - currentY, 0);
                } else {
                    dx = Integer.compare(targetX - currentX, 0);
                    dy = 0;
                }
                horizontalPhase = !horizontalPhase;

                nextX = currentX + dx;
                nextY = currentY + dy;

                if (!isValidHallwayPos(nextX, nextY) ||
                        (tiles[nextX][nextY] == Tileset.FLOOR && !adjacentToPath(nextX, nextY, path))) {
                    boolean found = false;
                    for (int[] dir : directions) {
                        int newDx = dir[0];
                        int newDy = dir[1];
                        int newX = currentX + newDx;
                        int newY = currentY + newDy;

                        if (isValidHallwayPos(newX, newY) &&
                                !(tiles[newX][newY] == Tileset.FLOOR && !adjacentToPath(newX, newY, path))) {
                            dx = newDx;
                            dy = newDy;
                            horizontalPhase = (dx != 0);
                            found = true;
                            break;
                        }
                    }

                    //at this point we just give up and move on
                    if (!found) {
                        break;
                    }
                }
            }
            currentX += dx;
            currentY += dy;
        }

        return reachedTarget;
    }

    /**
     * Determines if given (x,y) is adjacent to any tile in given path.
     * Used to avoid repeating operations
     *
     * @param x x-coordinate to check.
     * @param y y-coordinate to check.
     * @param path The current hallway path.
     * @return true if adjacent to path, false otherwise
     */

    private boolean adjacentToPath(int x, int y, List<Point> path) {
        for (Point p : path) {
            int px = (int) p.getX();
            int py = (int) p.getY();

            if ((Math.abs(px - x) + Math.abs(py - y)) == 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates if tile is a valid position for hallway construction.
     * Valid positions are either GRASS, WALL, or existing FLOOR.
     *
     * @param x x-coordinate to check.
     * @param y y-coordinate to check.
     * @return true if tile is valid hallway tile.
     */

    private boolean isValidHallwayPos(int x, int y) {
        return isInBounds(x, y) && (tiles[x][y] == Tileset.GRASS || tiles[x][y] == Tileset.FLOOR || tiles[x][y] == Tileset.WALL);
    }

    /**
     * Checks if the coordinate (x,y) is within the bounds of world.
     *
     * @param x x-coordinate to check.
     * @param y y-coordinate to check.
     * @return true if in bounds, false otherwise.
     */

    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < tiles.length && y >= 0 && y < tiles[0].length;
    }
}
