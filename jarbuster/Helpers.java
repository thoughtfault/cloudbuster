package org.example;

import java.text.SimpleDateFormat;

/**
 * This class provides helper functions
 */
public class Helpers {
    public static boolean verbose;

    /**
     * Helper function to log messages and errors
     * @param message - the message to log
     * @param mode - used to determine if the message is an error
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

}
