package org.example;

/**
 * This class is a handler class for the Infrastructure class
 */
public class RegionalInfrastructure implements Runnable {
    private String region;
    private int mode;

    public RegionalInfrastructure(String region, int mode) {
        this.region = region;
        this.mode = mode;
    }


    @Override
    public void run() {
        Infrastructure infrastructure = new Infrastructure(region);

        if (mode == 0) {

            if (!infrastructure.created()) {
                infrastructure.create();
            }
            if (!infrastructure.running()) {
                infrastructure.start();
            }

            Main.startDiscovery(infrastructure);
        } else {
            infrastructure.destroy();
        }
    }
}
