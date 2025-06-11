package core;

import tileengine.TERenderer;
import tileengine.TETile;
import edu.princeton.cs.algs4.StdDraw;

import java.awt.*;
import java.util.Arrays;
import java.util.List;


public class Main {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 50;

    private static World world;
    private static TETile[][] tiles;
    private static TERenderer image;
    private static StringBuilder inputHistory = new StringBuilder();

    private static int lastHarvest = -1;
    private static String lastHoveredTile = "";

    public static void main(String[] args) {
        MainMenu();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char input = Character.toUpperCase(StdDraw.nextKeyTyped());

                if (input == 'N') {
                    String seed = getSeedInput();
                    if (!seed.isEmpty()) {
                        inputHistory.append('N');
                        inputHistory.append(seed).append('S');
                        long seedValue = Long.parseLong(seed);
                        startGame(seedValue);
                        break; // exit menu loop
                    }
                } else if (input == 'L') {
                    inputHistory.setLength(0);
                    world = SaveLoad.loadGame(inputHistory);
                    if (world != null) {
                        tiles = world.getWorld();
                        image = new TERenderer();
                        image.initialize(tiles.length, tiles[0].length);
                        image.renderFrame(tiles);
                        runGameLoop();
                        return;
                    } else {
                        System.out.println("No Loaded Game Available");
                    }
                } else if (input == 'Q') {
                    System.exit(0);
                }
            }
        }

        runGameLoop();
    }

    private static void MainMenu() {
        StdDraw.setCanvasSize(WIDTH * 16, HEIGHT * 16);
        StdDraw.setXscale(0, WIDTH);
        StdDraw.setYscale(0, HEIGHT);
        StdDraw.clear();
        StdDraw.enableDoubleBuffering();
        drawBackground();
        StdDraw.setPenColor(StdDraw.WHITE);
        // Make title 3x bigger than default (default is ~16pt)
        StdDraw.setFont(new Font("SansSerif", Font.BOLD, 48));
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.8, "Village of Sundar सुंदर");

        StdDraw.setFont(new Font("SansSerif", Font.PLAIN, 20));
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.7, "Tiger Game");
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.5, "(N) New Game");
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.45, "(L) Load Game");
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.4, "(Q) Quit");
        StdDraw.show();
    }

    private static String getSeedInput() {
        StringBuilder seedBuilder = new StringBuilder();
        drawBackground();
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.text(WIDTH / 2.0, HEIGHT * 0.7, "Enter a seed followed by S");
        StdDraw.show();

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char input = StdDraw.nextKeyTyped();
                if (Character.toUpperCase(input) == 'S') {
                    break;
                } else if (Character.isDigit(input)) {
                    seedBuilder.append(input);
                }

                drawBackground();
                StdDraw.setPenColor(StdDraw.WHITE);
                StdDraw.text(WIDTH / 2.0, HEIGHT * 0.7, "Enter a seed followed by S");
                StdDraw.text(WIDTH / 2.0, HEIGHT * 0.5, seedBuilder.toString());
                StdDraw.show();
            }
        }
        return seedBuilder.toString();
    }

    private static void startGame(long seed) {
        world = new World(seed);
        tiles = world.getWorld();
        image = new TERenderer();
        image.initialize(tiles.length, tiles[0].length);
        image.renderFrame(tiles);
        StdDraw.enableDoubleBuffering();
    }

    private static void drawBackground() {
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.filledRectangle(WIDTH / 2.0, HEIGHT / 2.0, WIDTH, HEIGHT);
    }

    private static void drawHUD(int harvest, String tileDes) {
        double hudWidth = 10.0;
        double hudHeight = 4.0;
        double hudX = world.width - 2.0 - (hudWidth / 2.0); // MIGHT NEED SOME ADJUSTMENT
        double hudY = world.height - 2.0 - (hudHeight / 2.0);

        // HUD creation and placement
        StdDraw.setPenColor(Color.BLACK);
        StdDraw.filledRectangle(hudX, hudY, hudWidth / 2.0, hudHeight / 2.0);
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.rectangle(hudX, hudY, hudWidth / 2.0, hudHeight / 2.0);

        // Text logic
        StdDraw.setFont(new Font("SansSerif", Font.BOLD, 18));
        StdDraw.setPenColor(Color.WHITE);

        // Harvest information
        String textHarvest = "Harvest: " + world.Farmer.getHarvest();
        StdDraw.textLeft(hudX - (hudWidth / 2.0) + 0.5, hudY + 0.5, textHarvest);

        String mouse = "*" + tileDes;
        StdDraw.textLeft(hudX - (hudWidth / 2.0) + 0.5, hudY - 1.0, mouse);
    }

    private static void runGameLoop() {
        List<Character> movements = Arrays.asList('W', 'A', 'S', 'D');
        boolean colonPressed = false;

        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char input = Character.toUpperCase(StdDraw.nextKeyTyped());

                if (colonPressed && input == 'Q') {
                    inputHistory.append(":Q");
                    SaveLoad.saveGame(world, inputHistory.toString());
                    System.exit(0);
                }

                if (input == ':') {
                    colonPressed = true;
                    continue;
                }

                colonPressed = false;

                if (movements.contains(input)) {
                    world.Farmer.movePlayer(input);
                    inputHistory.append(input);
                    image.renderFrame(tiles);
                    drawHUD(lastHarvest, lastHoveredTile);
                    StdDraw.show();
                }
            }

            // Mouse information
            int mouseX = (int) StdDraw.mouseX();
            int mouseY = (int) StdDraw.mouseY();
            String hoveredTile = "";

            if (mouseX >= 0 && mouseX < world.width && mouseY >=0 && mouseY < world.height) {
                hoveredTile = tiles[mouseX][mouseY].description();
            }

            int harvest = world.Farmer.getHarvest();

            if (harvest != lastHarvest || !hoveredTile.equals(lastHoveredTile)) {
                lastHoveredTile = hoveredTile;
                lastHarvest = harvest;
                image.renderFrame(tiles);
                drawHUD(harvest, hoveredTile);
                StdDraw.show();
            }
        }
    }
}

