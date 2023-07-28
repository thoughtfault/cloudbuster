package org.example;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import static org.example.Helpers.print;
import static org.example.Helpers.readFile;

/**
 * This class provides thread-safe wordlist retrival for workers
 */
public class Manager {
    private Queue<ArrayList<String>> chunks;
    private String target;

    /**
     * Loads the specified wordlist and applies extensions.  Splits into multiple sub-wordlists.
     * @param filePath - the filepath of the wordlist to load
     */
    public Manager(String filePath) {
        print("Loading " + filePath, 0);
        ArrayList<String> wordlist = readFile(filePath);

        if (Main.extensions != null) {
            for (String word : wordlist) {
                for (String extension : Main.extensions) {
                    wordlist.add(word + extension);
                }
            }
        }

        java.util.Collections.shuffle(wordlist);

        chunks = new LinkedList<>();
        int splitSize = wordlist.size() / 30;

        for (int i = 0; i < wordlist.size(); i += splitSize) {
            chunks.add(new ArrayList<>(wordlist.subList(i, Math.min(i + splitSize, wordlist.size()))));
        }
    }

    /**
     * Retrives a sub-wordlist
     * @return a sub-wordlist
     */
    public synchronized ArrayList<String> take() {
        if (chunks.isEmpty()) {
            return null;
        }
        return chunks.remove();
    }

    public synchronized int getChunkSize() {
        return chunks.size();
    }
}
