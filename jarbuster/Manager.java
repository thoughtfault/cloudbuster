package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/**
 * This class is a thread-safe manager for workers to get inputs and return outputs to
 */
public class Manager {
    private Queue<URL> wordlist;
    private ArrayList<String> results;

    /**
     * The default constructor for Manager will block until wordlist input appears
     * @param filePath - the filepath to block on
     */
    public Manager(String filePath) {
        wordlist = new LinkedList<>();
        results = new ArrayList<>();

        while (true) {
            try {
                File f = new File(filePath);
                Scanner scanner = new Scanner(f);

                while (scanner.hasNextLine()) {
                    try {
                        URL url = new URL(scanner.nextLine());
                        wordlist.add(url);
                    } catch (MalformedURLException ignore) {}
                }

                if (!wordlist.isEmpty()) {
                    break;
                }
            } catch (FileNotFoundException ignore) {}

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {}
        }
    }

    /**
     * This method returns the output ArrayList
     * @return - the output ArrayList
     */
    public ArrayList<String> getResults() {
        return results;
    }

    /**
     * This method feeds urls to workers
     * @return - the least recently added item, or null if empty
     */

    public synchronized URL getURL() {
        if (wordlist.isEmpty()) {
            return null;
        }
        return wordlist.remove();
    }

    /**
     * Adds a comma seperated string to output ArrayList
     * @param url - the requested url
     * @param statusCode - the status code of the requested url
     * @param contentLength - the response content length
     */
    public synchronized void putResult(String url, int statusCode, int contentLength) {
        results.add(url + "," + String.valueOf(statusCode) + "," + String.valueOf(contentLength));
    }
}
