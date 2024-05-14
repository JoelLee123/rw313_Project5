package org.example.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * The Client class handles the client-side functionality of the chat
 * application.
 * It manages the connection to the server, sending and receiving messages, and
 * updating the chat GUI.
 */
public class Client extends Application {
    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String username;
    private ChatGuiController controller;
    public Stage chatStage;
    private FileTransferManager fileTransferManager; // Each client has its own FileTransferManager
    private String serverAddress;
    private int serverPort;

    String originalMessageKey;

    /**
     * Constructs a Client instance with a specified socket, username, and
     * controller.
     *
     * @param socket     The socket connecting to the server.
     * @param username   The username of the client.
     * @param controller The controller for the chat GUI.
     */
    public Client(Socket socket, String username, ChatGuiController controller, String serverAddress) {
        this.fileTransferManager = new FileTransferManager();
        try {
            this.controller = controller;
            this.socket = socket;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectOutputStream.flush();
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.username = username;
            this.serverAddress = serverAddress;

            // Send the username as a Message object to the server
            sendMessage(new Message("login", username, null, username, ""));
            listenForMessage();
        } catch (IOException e) {
            closeEverything(socket, objectInputStream, objectOutputStream);
        }
    }

    /**
     * Default constructor for Client.
     */
    public Client() {
    }

    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to be sent.
     */
    public void sendMessage(Message message) {
        try {
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        } catch (IOException e) {
            closeEverything(socket, objectInputStream, objectOutputStream);
        }
    }

    public void listenForMessage() {
        new Thread(() -> {
            while (socket.isConnected()) {
                try {
                    Message messageFromServer = (Message) objectInputStream.readObject();
                    if (messageFromServer != null) {
                        switch (messageFromServer.getType()) {
                            case "usernameTaken":
                                handleUsernameTaken();
                                break;
                            case "searchResults":
                                handleSearchResults(messageFromServer.getContent());
                                break;
                            case "initiateDownloadFrom":
                                handleInitiateDownloadFrom(messageFromServer);
                            case "checkFile":
                                handleCheckFileRequest(messageFromServer);
                                break;
                            default:
                                System.out.println("Unhandled message type: " + messageFromServer.getType());
                                break;
                        }
                    } else {
                        handleServerDown();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    handleException(e.getMessage());
                    break;
                }
            }
        }).start();
    }

    private void handleCheckFileRequest(Message message) {
        String filename = message.getContent();
        if (fileTransferManager.hasFile(filename)) {
            System.out.println("check file in client");

            originalMessageKey = generateRandomMessageKey(); // Generate a unique message key
            
            System.out.println("original key: " + originalMessageKey);

            String encryptedMessageKey = Encryption.encrypt(originalMessageKey);

            System.out.println("encrypted key: " + encryptedMessageKey); 

            sendMessage(new Message("fileAvailable", username, message.getRecipient(),
                    filename + ":" + fileTransferManager.getPort(), encryptedMessageKey));
        }
    }

    private void handleInitiateDownloadFrom(Message message) {
        String[] contentParts = message.getContent().split(":");
        if (contentParts.length < 2) {
            System.out.println("Invalid download initiation message format.");
            return;
        }
        String filename = contentParts[0];
        int port = Integer.parseInt(contentParts[1]);
        String decryptedMessageKey = message.getMessageKey();
        System.out.println("decrypted key:" + decryptedMessageKey);

        if (decryptedMessageKey.equals(originalMessageKey)) {
            Platform.runLater(() -> {
                try {
                    String relativePath = System.getProperty("user.dir") + "/downloads/";
                    fileTransferManager.downloadFile(getServerAddress(), port, filename,
                            relativePath + filename, controller.getProgressBar());
                } catch (Exception e) {
                    showAlert("Download Failed", "Failed to initiate download for " + filename + ": " + e.getMessage());
                }
            });
        } else {
            System.out.println("Message key verification failed. Download aborted.");
        }
    }

    public void sendSearchRequest(String query) {
        sendMessage(new Message("search", username, null, query, ""));
    }

    private void handleUsernameTaken() {
        Platform.runLater(() -> {
            try {
                closeEverything(socket, objectInputStream, objectOutputStream);
                restartClient(username);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleSearchResults(String results) {
        Platform.runLater(() -> controller.displaySearchResults(results));
    }

    private void handleServerDown() {
        Platform.runLater(() -> {
            closeEverything(socket, objectInputStream, objectOutputStream);
            System.out.println("SERVER: Server down, disconnecting clients...");
            System.exit(0);
        });
    }

    private String generateRandomMessageKey() {
        // Implement your logic to generate a random message key
        // For example, you can use UUID or a random string generator
        // Here's a simple example using UUID:
        return UUID.randomUUID().toString();
    }

    private void handleException(String errorMessage) {
        Platform.runLater(() -> {
            closeEverything(socket, objectInputStream, objectOutputStream);
            System.out.println("Error: " + errorMessage);
        });
    }

    /**
     * Closes the socket and input/output streams.
     */
    public void closeEverythingHelper() {
        closeEverything(socket, objectInputStream, objectOutputStream);
    }

    /**
     * Closes the socket and input/output streams.
     *
     * @param socket             The socket to close.
     * @param objectInputStream  The input stream to close.
     * @param objectOutputStream The output stream to close.
     */
    public void closeEverything(Socket socket, ObjectInputStream objectInputStream,
            ObjectOutputStream objectOutputStream) {
        try {
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
            if (objectInputStream != null) {
                objectInputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The entry point for the JavaFX application.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        launch();
    }

    /**
     * Starts the JavaFX application and loads the login screen.
     *
     * @param stage The primary stage for this application.
     * @throws IOException If the FXML file cannot be loaded.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("MainController.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Sets the chat stage for the client.
     *
     * @param chatStage The stage used for the chat window.
     */
    public void setChatStage(Stage chatStage) {
        this.chatStage = chatStage;
    }

    /**
     * Restarts the client when the username is already taken.
     * 
     * @param usernameTaken The username that is already taken.
     * @throws IOException If the FXML file cannot be loaded.
     */
    public void restartClient(String usernameTaken) throws IOException {
        Platform.runLater(() -> {
            if (chatStage != null) {
                chatStage.close();
            }
        });

        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("MainController.fxml"));
                Parent root = fxmlLoader.load();
                Stage stageRestart = new Stage();
                stageRestart.setTitle("Login");
                stageRestart.setScene(new Scene(root));
                stageRestart.show();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Username Taken");
                alert.setHeaderText(null);
                alert.setContentText(
                        "The username '" + usernameTaken + "' is already taken. Please choose a different username.");
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
