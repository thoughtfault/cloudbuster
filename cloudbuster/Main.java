package org.example;

import com.jcraft.jsch.*;
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static org.example.Helpers.print;
import static org.example.Helpers.readFile;

/**
 * This class parses command line inputs and polls workers for output
 */
public class Main {
    // Argument defaults
    private static String target;
    private static String wordlistPath;
    public static String[] extensions;
    private static String[] regions = {"us-west-2", "us-east-2"};
    public static int instanceCount = 4;
    public static String instanceType = "t2.micro";
    private static int mode = 0;
    private static ArrayList<String> filterStatusCodes = new ArrayList<>();
    private static ArrayList<String> filterContentSizes = new ArrayList<>();

    // Global
    private static Manager manager;
    private static String keyName;

    public static int maxRetries = 10;

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (mode == 0) {
                    for (String region : regions) {
                        new Infrastructure(region).stop();
                    }
                }
            }
        });

        Options options = new Options();

        options.addOption("h", "help", false, "show this help message");
        options.addOption("d", "destroy", false, "Destroy infrastructure in specified regions");
        options.addOption("u", "url", true, "The target url");
        options.addOption("w", "wordlist", true,"The wordlist to use");
        options.addOption("x", "extensions", true, "Comma seperated extensions to use");
        options.addOption("r", "regions", true, "N. Virginia - us-east-\n Ohio - us-east-2\n N. California - us-west-1\n Oregon - us-west-2\n GovCloud West - us-gov-west-1\n GovCloud East - us-gov-east-1\n Canada - ca-central-1\n Stockholm - eu-north-1\n Ireland - eu-west-1\n London - eu-west-2\n Paris - eu-west-3\n Frankfurt - eu-central-1\n Milan - eu-south-1\n Cape Town - af-south-1\n Tokyo - ap-northeast-1\n Seoul - ap-northeast-2\n Osaka - ap-northeast-3\n Singapore - ap-southeast-1\n Sydney - ap-southeast-2\n Jakarta - ap-southeast-3\n Hong Kong - ap-east-1\n Mumbai - ap-south-1\n Sao Paulo - sa-east-1\n Bahrain - me-south-1\n Beijing - cn-north-1\n Ningxia - cn-northwest-1\n");
        options.addOption("c", "count", true, "The number of instances to use");
        options.addOption("t", "type", true, "The instance type");
        options.addOption("s", "status", true, "Status codes to filter out");
        options.addOption("c", "content", true, "Content lengths to filter out");
        options.addOption("v", "verbose", false, "To enable verbose output");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("cloudbuster", options);
                return;
            }

            if (cmd.hasOption("d")) {
                mode = 1;
            }

            if (cmd.hasOption("u")) {
                target = cmd.getOptionValue("u");
            } else if (!cmd.hasOption("d")) {
                print("A target is required (-u/--url)", 1);
                return;
            }

            if (cmd.hasOption("w")) {
                wordlistPath = cmd.getOptionValue("w");
            } else if (!cmd.hasOption("d")) {
                print("A wordlist is required (-w/--wordlist)", 1);
                return;
            }

            if (cmd.hasOption("x")) {
                extensions = cmd.getOptionValue("x").split(",");
            }

            if (cmd.hasOption("r")) {
                regions = cmd.getOptionValue("r").split(",");
            }

            if (cmd.hasOption("c")) {
                instanceCount = Integer.parseInt(cmd.getOptionValue("c"));
            }

            if (cmd.hasOption("t")) {
                instanceType = cmd.getOptionValue("2");
            }

            if (cmd.hasOption("s")) {
                for (String statusCode : cmd.getOptionValue("s").split(",")) {
                    filterStatusCodes.add(statusCode);
                }
            } else {
                filterStatusCodes.add("404");
            }

            if (cmd.hasOption("c")) {
                for (String contentSize : cmd.getOptionValue("c").split(",")) {
                    filterContentSizes.add(contentSize);
                }
            }

            if (cmd.hasOption("v")) {
                Helpers.verbose = true;
            }
        } catch (ParseException e) {
            print("Invalid command line arguments: " + e.getMessage(), 1);
            return;
        }

        ArrayList<Thread> regionalWorkers = new ArrayList<>();
        for (int i = 0; i < regions.length; i++) {
            regionalWorkers.add(new Thread(new RegionalInfrastructure(regions[i], mode)));
            regionalWorkers.get(i).start();
        }

        for (Thread worker: regionalWorkers) {
            try {
                worker.join();
            } catch (InterruptedException ignore) {}
        }
    }

    public static void startDiscovery(Infrastructure infrastructure) {

        ArrayList<String> addresses = infrastructure.getAddresses();

        manager = new Manager(wordlistPath);
        keyName = infrastructure.keyName;

        print("Starting pollers", 0);
        ArrayList<Thread> pollers = new ArrayList<>();
        for (int i = 0; i < addresses.size(); i++) {
            pollers.add(new Thread(new Poller(addresses.get(i))));
            pollers.get(i).start();
        }

        for (Thread poller : pollers) {
            try {
                poller.join();
            } catch (InterruptedException ignore) {}
        }
    }

    private static class Poller implements Runnable {
        private String address;

        public Poller(String address) {
            this.address = address;
        }

        @Override
        public void run() {
            print("Establishing ssh session to " + address, 0);
            Session session;
            int retryCounter = 0;
            while (true) {
                try {
                    JSch jsch = new JSch();
                    session = jsch.getSession("root", address, 22);
                    jsch.setConfig("StrictHostKeyChecking", "no");
                    jsch.addIdentity(keyName + ".pem");
                    session.connect();
                    break;
                } catch (JSchException exp) {
                    print("An error occured while establishing the ssh session to " + address + ": " + exp.toString(), 1);
                    if (retryCounter != maxRetries) {
                        retryCounter += 1;
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException ignore) {}
                        continue;
                    }
                    print("The maximum number of retries has been reached for " + address + ".  Exiting now", 1);
                    System.exit(1);
                }
            }

            Channel commandChannel;
            try {
                commandChannel = session.openChannel("exec");
            } catch (JSchException exp) {
                print("An error occured while esablishing a command channel with " + address + ": " + exp.toString(), 1);
                return;
            }

            print("Removing old output files", 0);
            ((ChannelExec) commandChannel).setCommand("rm /opt/*.txt");
            try {
                commandChannel.connect();
            } catch (JSchException exp) {
                print("An error occured while connecting to a command channel for " + address + ": " + exp.toString(), 1);
                return;
            }

            print("Establishing sftp channel to " + address, 0);
            ChannelSftp fileChannel;
            try {
                fileChannel = (ChannelSftp) session.openChannel("sftp");
                fileChannel.connect();
            } catch (JSchException exp) {
                print("An error occured when establishing the sftp channel to " + address + ": " + exp.toString(), 1);
                return;
            }

            int chunkSize;
            while (true) {
                chunkSize = manager.getChunkSize();
                if (chunkSize == 0) {
                    session.disconnect();
                    return;
                }

                String filename = "data/chunk-" + chunkSize + ".txt";
                String output = filename.replace("chunk", "output");
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
                    ArrayList<String> segment = manager.take();
                    if (segment == null) {
                        return;
                    }
                    for (String line : segment) {
                        writer.write(target + line + "\n");
                    }
                    writer.flush();
                } catch (IOException exp) {
                    print("An error occured while writing the wordlist segment: " + exp.toString(), 1);
                    return;
                }

                try {
                    fileChannel.put(filename, "/opt/wordlist.txt");
                } catch (SftpException exp) {
                    System.err.println("An error occured while transfering the wordlist to " + address + ": " + exp.toString());
                    return;
                }

                while (true) {
                    SftpATTRS attributes;
                    try {
                        attributes = fileChannel.stat("/opt/output.txt");

                        if (attributes != null) {
                            fileChannel.get("/opt/output.txt", output);
                            fileChannel.rm("/opt/output.txt");
                            break;
                        }
                    } catch (SftpException sftpExp) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException intExp) {
                            System.err.println("An error occured while waiting for output on " + address + ": " + intExp.toString());
                            return;
                        }
                    }
                }

                for (String line : readFile(output)) {
                    String[] split_line = line.split(",");
                    if (!filterStatusCodes.contains(split_line[1]) && !filterContentSizes.contains(split_line[2])) {
                        System.out.println(split_line[1] + "\t\t" + split_line[2] + "c\t" + address + " -> " + split_line[0]);
                    }
                }
            }
        }
    }
}
