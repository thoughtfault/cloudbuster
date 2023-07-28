package org.example;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This class provides some helper methods for other classes
 */
public class Helpers {
    public static boolean verbose = false;

    /**
     * This method is used for printing output/error messages
     * @param message - the message to print
     * @param mode - whether the message should be stdout or stderr
     */
    public static void print(String message, int mode) {
        if (!verbose) {
            return;
        }
        if (mode == 0)  {
            System.out.println("[***] [" + Thread.currentThread().getName() + "] " + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()) + " " + message + "...");
        } else {
            System.err.println("[***] [" + Thread.currentThread().getName() + "] " + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()) + " " + message + "...");
        }
    }

    /**
     * Reads a file into an ArrayList
     * @param filePath - the filepath to read
     * @return - an ArrayList containing the file lines
     */
    public static ArrayList<String> readFile(String filePath) {

        ArrayList<String> lines = new ArrayList<>();

        File file = new File(filePath);
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
        } catch (IOException exp) {
            print("An error occured while reading a file: " + exp.toString(), 1);
            System.exit(1);
        }
        return lines;
    }
}
