package org.example.demo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

public class FileTransferManager {
    private ExecutorService executorService; // To manage threads efficiently
    private volatile boolean pauseDownloadFlag = false; // Flag to control download pausing
    private long downloadOffset = 0; // Variable to store the download offset
    private boolean isDownloadPaused = false; // Variable to track if the download is paused

    private ProgressBar progressBar;
    private ChatGuiController chatGuiController;
    ServerController serverController;
    private int port;

    private String serverAddress;
    private int serverPort;
    private String fileToDownload;
    private String savePath;

    String uploadPath = System.getProperty("user.dir") + "/files/";

    public FileTransferManager() {
        executorService = Executors.newCachedThreadPool();
        startUploadServer();
    }

    public void resumeDownload() {
        pauseDownloadFlag = false;
        isDownloadPaused = false;
        resumeDownload(this.serverAddress, this.serverPort, this.fileToDownload, this.savePath);
    }

    public void pauseDownload() {
        System.out.println("pauseDownload() is called");
        pauseDownloadFlag = true;
        isDownloadPaused = true;
        Server.updateClientActivity("Download paused");
    }

    public int getPort() {
        return port; // Retrieve the dynamically assigned port
    }

    void startUploadServer() {
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(0)) { // System-assigned port
                this.port = serverSocket.getLocalPort();
                System.out.println("Upload server started on dynamically assigned port: " + this.port);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    handleUploadRequest(clientSocket);
                }
            } catch (IOException e) {
                System.out.println("Upload server error: " + e.getMessage());
            }
        });
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
        // Save the parameters to instance variables
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.fileToDownload = fileToDownload;
        this.savePath = savePath;
        this.progressBar = progressBar;

        executorService.submit(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    FileOutputStream fos = new FileOutputStream(savePath)) {

                System.out.println("save dir in downloadFile: " + savePath);
                System.out.println("file to download: " + uploadPath + fileToDownload);
                dos.writeUTF(uploadPath + fileToDownload); // Send the file request
                dos.writeUTF("NORMAL");
                dos.flush();

                long fileSize = dis.readLong(); // Read file size
                System.out.println("File size to download: " + fileSize);
                long totalRead = 0;
                byte[] buffer = new byte[4096];
                int read;
                while ((read = dis.read(buffer)) > 0 && !pauseDownloadFlag) {
                    fos.write(buffer, 0, read);
                    totalRead += read;
                    downloadOffset = totalRead; // Update the offset
                    double progress = totalRead / (double) fileSize;
                    if (progressBar != null) {
                        Platform.runLater(() -> progressBar.setProgress(progress));
                    }
                    Server.updateClientActivity("Download progress: " + progress);
                }
                if (totalRead >= fileSize) {
                    System.out.println("Download complete.");
                    if (progressBar != null) {
                        Platform.runLater(() -> progressBar.setProgress(1.0)); // Complete the progress bar
                    }
                    Server.updateClientActivity("Download completed for file: " + fileToDownload);
                } else if (pauseDownloadFlag) {
                    System.out.println("Download paused at " + totalRead + " bytes.");
                    Server.updateClientActivity("Download paused for file: " + fileToDownload);
                }
            } catch (IOException e) {
                System.out.println("Download error: " + e.getMessage());
                Server.updateClientActivity("Download error for file: " + fileToDownload);
            }
        });
    }

    public void resumeDownload(String serverAddress, int serverPort, String fileToDownload, String savePath) {
        pauseDownloadFlag = false;
        isDownloadPaused = false;
        executorService.submit(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    RandomAccessFile raf = new RandomAccessFile(savePath, "rw")) {

                dos.writeUTF(fileToDownload); // Send the file name to resume downloading
                dos.writeUTF("RESUME"); // Indicate it's a resume request
                dos.writeLong(downloadOffset); // Send the offset to resume from

                raf.seek(downloadOffset); // Move the file pointer to the offset
                long fileSize = dis.readLong(); // Read the remaining file size
                long totalRead = downloadOffset;
                byte[] buffer = new byte[4096];
                int read;
                while ((read = dis.read(buffer)) > 0 && !pauseDownloadFlag) {
                    raf.write(buffer, 0, read);
                    totalRead += read;
                    downloadOffset = totalRead; // Update the offset
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

    /**
     * Checks if the specified file exists in the local storage.
     * 
     * @param fileName The name of the file to check.
     * @return true if the file exists, false otherwise.
     */
    public boolean hasFile(String fileName) {
        String relativePath = System.getProperty("user.dir") + "/files/";
        File fileToCheck = new File(relativePath + fileName);
        return fileToCheck.exists() && !fileToCheck.isDirectory();
    }

    private void handleUploadRequest(Socket clientSocket) {
        executorService.submit(() -> {
            try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {

                String fileName = dis.readUTF(); // Read the requested file name
                String requestType = dis.readUTF(); // Read the type of request (NORMAL or RESUME)

                System.out.println("filename in handleUploadRequest(): " + fileName);
                File fileToUpload = new File(fileName); // Ensure the correct file path is used

                if (fileToUpload.exists() && !fileToUpload.isDirectory()) {
                    // Send file size
                    dos.writeLong(fileToUpload.length());
                    dos.flush(); // Ensure the file size is sent before sending file data

                    if (requestType.equals("RESUME")) {
                        long offset = dis.readLong(); // Read the offset for resume
                        System.out.println("Resuming upload from offset: " + offset);

                        // Read and send file data from offset
                        try (RandomAccessFile raf = new RandomAccessFile(fileToUpload, "r")) {
                            raf.seek(offset);
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = raf.read(buffer)) > 0 && !pauseDownloadFlag) {
                                dos.write(buffer, 0, read);
                            }
                        }
                    } else {
                        System.out.println("Starting normal file upload.");

                        // Read and send file data from beginning
                        try (FileInputStream fis = new FileInputStream(fileToUpload)) {
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = fis.read(buffer)) > 0 && !pauseDownloadFlag) {
                                dos.write(buffer, 0, read);
                            }
                        }
                    }
                } else {
                    System.out.println("Requested file does not exist: " + fileName);
                }
            } catch (SocketException e) {
                // Handle client disconnection gracefully
                System.out.println("Client disconnected: " + e.getMessage());
            } catch (IOException e) {
                // System.out.println("Upload error: " + e.getMessage());
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
