package core;

import java.io.*;

public class SaveLoad {
    private static final String SAVE_FILE = "save.txt";

    //this saves the current world state and the seed
    public static void saveGame(World world, String inputHistory) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            out.writeObject(inputHistory);
        } catch (IOException e) {
            System.out.println("Failed to save game: " + e.getMessage());
        }
    }

    //this loads the save file using the inputHistory
    public static World loadGame(StringBuilder inputHistory) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            String savedInput = (String) in.readObject();
            inputHistory.append(savedInput);
            return reconstructWorldFromInput(savedInput);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Failed to load game: " + e.getMessage());
            return null;
        }
    }

    //getting the world back to how it was when saved
    private static World reconstructWorldFromInput(String input) {
        input = input.toUpperCase();
        if (!input.startsWith("N")) {
            return null;
        }
        int sIndex = input.indexOf('S');
        if (sIndex == -1) {
            return null;
        }
        String seedString = input.substring(1, sIndex);
        long seed = Long.parseLong(seedString);
        World world = new World(seed);
        for (int i = sIndex + 1; i < input.length(); i++) {
            char move = input.charAt(i);
            if ("WASD".indexOf(move) >= 0) {
                world.Farmer.movePlayer(move);
            }
        }
        return world;
    }

    //
}
