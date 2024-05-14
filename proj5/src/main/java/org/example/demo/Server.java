package org.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Server class is responsible for handling the server-side operations
 * of the chat application. It listens for incoming client connections and
 * creates a new thread for each connected client.
 */
public class Server extends Application {

    private ServerSocket serverSocket;
    public static Set<String> activeUsernames = ConcurrentHashMap.newKeySet();
    private static ServerController controller;
    public static SearchManager searchManager;

    private FileMonitor fileMonitor;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize SearchManager with the folder path
            //System independent absolute path - this will only check your specific PC atm
            String currentDir = System.getProperty("user.dir");
            //String projectPath = "/src/main/java/org/example/demo/files";
            String relativePath = currentDir + "/files";

            searchManager = new SearchManager(relativePath);
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ServerGUI.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            primaryStage.setTitle("Server");
            primaryStage.setScene(scene);
            primaryStage.show();

            controller = fxmlLoader.getController();

            Thread serverThread = new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(4044);
                    startServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            //This code is executed in the main JavaFX thread
            fileMonitor = new FileMonitor(relativePath, this);
            fileMonitor.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the server and listens for incoming client connections.
     * When a new client connects, it starts a new ClientHandler thread to handle
     * the client.
     */
    public void startServer() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            closeServerSocket();
        }
    }

    /**
     * Closes the server socket and releases any system resources associated with
     * it.
     */
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateFileList(File[] files) {
        controller.appendFileList(files);
    }

    /**
     * Appends a log of client activity to the server GUI to assist in monitoring.
     *
     * @param activity Description of the client activity
     */
    public static void updateClientActivity(String activity) {
        controller.appendClientActivity(activity);
    }

    /**
     * The main entry point for the server application.
     *
     * @param args Command-line arguments (not used).
     * @throws IOException If an I/O error occurs when opening the socket.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
