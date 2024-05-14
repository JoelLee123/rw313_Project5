package org.example.demo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

public class FileTransferManager {
    private ExecutorService executorService; // To manage threads efficiently
    private volatile boolean pauseDownloadFlag = false; // Flag to control download pausing
    private ProgressBar progressBar;
    private int port;
    // Progress bar to update (passed during initialization or method call)

    public FileTransferManager() {
        executorService = Executors.newCachedThreadPool();
        startUploadServer();
    }

    /**
     * Initiates a file download from another peer, with progress update.
     * 
     * @param serverAddress  The IP address of the peer from which to download.
     * @param serverPort     The port number on the peer for downloading.
     * @param fileToDownload The name of the file to download.
     * @param savePath       The local path to save the downloaded file.
     * @param progressBar    The progress bar UI element to update.
     */
    public void downloadFile(String serverAddress, int serverPort, String fileToDownload, String savePath,
            ProgressBar progressBar) {
        executorService.submit(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    FileOutputStream fos = new FileOutputStream(savePath)) {

                dos.writeUTF(fileToDownload); // Send the file request
                dos.flush();

                long fileSize = dis.readLong(); // Read file size
                long totalRead = 0;
                byte[] buffer = new byte[4096];
                int read;
                while ((read = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                    double progress = totalRead / (double) fileSize;
                    Platform.runLater(() -> progressBar.setProgress(progress));
                }
                if (totalRead >= fileSize) {
                    System.out.println("Download complete.");
                    Platform.runLater(() -> progressBar.setProgress(1.0)); // Complete the progress bar
                }
            } catch (IOException e) {
                System.out.println("Download error: " + e.getMessage());
            }
        });
    }

    public void pauseDownload() {
        pauseDownloadFlag = true;
    }

    public int getPort() {
        return port; // Retrieve the dynamically assigned port
    }

    public void resumeDownload(String serverAddress, int serverPort, String fileToDownload, String savePath,
            long offset) {
        pauseDownloadFlag = false;
        executorService.submit(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    RandomAccessFile raf = new RandomAccessFile(savePath, "rw")) {

                dos.writeUTF("RESUME");
                dos.writeLong(offset); // Send the offset to resume from

                raf.seek(offset); // Move the file pointer to the offset
                long fileSize = dis.readLong(); // Read the remaining file size
                long totalRead = offset;
                byte[] buffer = new byte[4096];
                int read;
                while ((read = dis.read(buffer)) > 0 && !pauseDownloadFlag) {
                    raf.write(buffer, 0, read);
                    totalRead += read;
                    double progress = totalRead / (double) fileSize;
                    Platform.runLater(() -> progressBar.setProgress(progress));
                }

                if (!pauseDownloadFlag) {
                    System.out.println("Resume complete.");
                    Platform.runLater(() -> progressBar.setProgress(1.0));
                } else {
                    System.out.println("Download paused at " + totalRead + " bytes during resume.");
                }
            } catch (IOException e) {
                System.out.println("Resume download error: " + e.getMessage());
            }
        });
    }

    void startUploadServer() {
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(0)) { // System-assigned port
                this.port = serverSocket.getLocalPort();
                System.out.println("Upload server started on dynamically assigned port: " + this.port);
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("Start of loop loop");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("In the while loop");
                    handleUploadRequest(clientSocket);
                }
            } catch (IOException e) {
                System.out.println("Upload server error: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if the specified file exists in the local storage.
     * 
     * @param fileName The name of the file to check.
     * @return true if the file exists, false otherwise.
     */
    public boolean hasFile(String fileName) {
        // Assuming files are stored in a specific directory, adjust path as necessary.
        File fileToCheck = new File("Group_36_Project_1/files/" + fileName);
        return fileToCheck.exists() && !fileToCheck.isDirectory();
    }

    private void handleUploadRequest(Socket clientSocket) {
        System.out.println("Banana");
        executorService.submit(() -> {
            try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                String fileName = dis.readUTF(); // Read the requested file name
                System.out.println("filename in handleUploadRequest(): " + fileName);

                File fileToUpload = new File(fileName);
                if (fileToUpload.exists() && !fileToUpload.isDirectory()) {
                    FileInputStream fis = new FileInputStream(fileToUpload);
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, read);
                    }
                    fis.close();
                } else {
                    System.out.println("Requested file does not exist: " + fileName);
                }
            } catch (IOException e) {
                System.out.println("Upload error: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing client socket: " + e.getMessage());
                }
            }
        });
    }
}
