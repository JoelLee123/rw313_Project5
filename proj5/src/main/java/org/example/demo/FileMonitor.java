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
                //Low number causes a crash
                Thread.sleep(15000); // Check for changes every 15 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}