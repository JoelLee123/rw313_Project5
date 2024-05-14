package org.example.demo;

import java.io.File;

public class FileMonitor extends Thread {
    private final String directoryPath;
    private final Server server;

    public FileMonitor(String directoryPath, Server server) {
        this.directoryPath = directoryPath;
        System.out.println("directoryPath " + directoryPath);
        this.server = server;
    }

    @Override
    public void run() {
        while (true) {
            File directory = new File(directoryPath);


            File[] files = directory.listFiles();

            if (files != null) {
                server.updateFileList(files);
            }

            try {
                Thread.sleep(5000); // Check for changes every 5 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}