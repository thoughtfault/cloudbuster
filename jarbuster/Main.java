package org.example;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.example.Helpers.print;

/**
 * This class the entry point loop that manages threads and input/output
 */
public class Main {
    private static Manager manager;
    private static boolean run;

    public static void main(String[] args) {
        String wordlistPath = "/opt/wordlist.txt";
        String outputPath = "/opt/output.txt";
        Helpers.verbose = true;

        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ignore) {}

        print("Starting thread pool", 0);
        ExecutorService pool = Executors.newCachedThreadPool();

        while (true) {

            print("Waiting for wordlist to appear", 0);
            manager = new Manager(wordlistPath);

            print("Sending requests", 0);
            run = true;
            while (run) {
                pool.execute(new Task());
            }

            print("Requests finsihed, waiting for queue to clear", 0);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignore) {}

            print("Deleting wordlist " + wordlistPath, 0);
            File wordlist = new File(wordlistPath);
            wordlist.delete();

            print("Writing to output", 0);
            try {
                File output = new File(outputPath);
                FileWriter writer = new FileWriter(output);
                for (String line : manager.getResults()) {
                    writer.write(line + "\n");
                }
                writer.flush();
            } catch (IOException exp) {
                print("An error occured when writing to " + outputPath + ": " + exp.toString(), 1);
                return;
            }
        }
    }

    /**
     * This class performs a single http request against a target and saves the response
     */
    private static class Task implements Runnable {

        public void run() {
            URL url = manager.getURL();
            if (url == null) {
                run = false;
                return;
            }

            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                manager.putResult(url.toString(), connection.getResponseCode(), connection.getContentLength());
            } catch (Exception exp) {
                print("An error occured while making a request to " + url.toString() + ": " + exp.toString(), 1);
            }
        }
    }
}
