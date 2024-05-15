package org.example.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

    private static final String ENCRYPTION_KEY = "mySecretKey";

    /**
     * Constructs a Client instance with a specified socket, username, and
     * controller.
     *
     * @param socket     The socket connecting to the server.
     * @param username   The username of the client.
     * @param controller The controller for the chat GUI.
     */
    public Client(Socket socket, String username, ChatGuiController controller, String serverAddress) {
        this.fileTransferManager = controller.getFileTransferManager();
        try {
            this.controller = controller;
            this.socket = socket;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectOutputStream.flush();
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.username = username;
            this.serverAddress = serverAddress;

            // Send the username as a Message object to the server
            sendMessage(new Message("login", username, null, null));
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

    /**
     * Returns the FileTransferManager instance used by the client.
     *
     * @return The FileTransferManager instance.
     */
    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }

    /**
     * Returns the IP address or hostname of the server.
     *
     * @return The server address as a String.
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Returns the port number on which the server is listening.
     *
     * @return The server port number.
     */
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

            String encryptedContent = Encryption.encrypt(message.getContent(), ENCRYPTION_KEY);
            Message encryptedMessage = new Message(message.getType(), message.getSender(), message.getRecipient(),
                    encryptedContent);
            objectOutputStream.writeObject(encryptedMessage);
            objectOutputStream.flush();
        } catch (IOException e) {
            closeEverything(socket, objectInputStream, objectOutputStream);
        }
    }

    /**
     * Listens for incoming messages from the server and handles them accordingly.
     * This method runs in a separate thread to ensure non-blocking behavior.
     */
    public void listenForMessage() {
        new Thread(() -> {
            while (socket.isConnected()) {
                try {
                    // Read an object from the input stream
                    Message messageFromServer = (Message) objectInputStream.readObject();

                    if (messageFromServer != null) {
                        // Decrypt the message content
                        String decryptedContent = Encryption.decrypt(messageFromServer.getContent(), ENCRYPTION_KEY);

                        // Create a new message object with the decrypted content
                        Message decryptedMessage = new Message(
                                messageFromServer.getType(),
                                messageFromServer.getSender(),
                                messageFromServer.getRecipient(),
                                decryptedContent);

                        // Check if the username is already taken
                        if (decryptedContent.equals("Username is already taken.")) {
                            // Run the following code on the JavaFX Application Thread
                            Platform.runLater(() -> {
                                try {
                                    // Close all resources
                                    closeEverything(socket, objectInputStream, objectOutputStream);
                                    // Restart the client
                                    restartClient(username);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            stop(); // Stop the listener thread
                            return;
                        }

                        // Handle different message types
                        switch (decryptedMessage.getType()) {
                            case "searchResults":
                                handleSearchResults(decryptedMessage.getContent());
                                break;
                            case "initiateDownloadFrom":
                                handleInitiateDownloadFrom(decryptedMessage);
                                break;
                            case "checkFile":
                                handleCheckFileRequest(decryptedMessage);
                                break;
                            default:
                                System.out.println("Unhandled message type: " + decryptedMessage.getType());
                                break;
                        }
                    } else {
                        // Handle server shutdown
                        handleServerDown();
                    }
                } catch (Exception e) {
                    // Handle exceptions
                    handleException(e.getMessage());
                    break;
                }
            }
        }).start(); // Start the listener thread
    }

    /**
     * Handles a request to check the availability of a file.
     *
     * @param message The message containing the filename to check.
     */
    private void handleCheckFileRequest(Message message) {
        // Get the filename from the message content
        String filename = message.getContent();

        // Check if the requested file is available in the client's file transfer
        // manager
        if (fileTransferManager.hasFile(filename)) {
            System.out.println("check file in client"); // Debugging statement

            // Construct a new message with the "fileAvailable" type
            // indicating that the file is available for transfer
            Message fileAvailableMessage = new Message("fileAvailable",
                    username,
                    message.getRecipient(),
                    filename + ":" + fileTransferManager.getPort());

            // Send the "fileAvailable" message to the requesting peer
            sendMessage(fileAvailableMessage);
        }
    }

    /**
     * Handles the initiation of a file download from a peer.
     *
     * @param message The message containing the file information and peer address.
     */
    private void handleInitiateDownloadFrom(Message message) {
        System.out.println("I am called"); // Debugging statement

        // Split the message content into parts, assuming the format is "filename:port"
        String[] contentParts = message.getContent().split(":");

        // Check if the message content has the expected format
        if (contentParts.length < 2) {
            System.out.println("Invalid download initiation message format."); // Debugging statement
            return; // Exit the method if the message format is invalid
        }

        String filename = contentParts[0]; // Extract the filename from the message
        int port; // Declare a variable for the port number

        try {
            port = Integer.parseInt(contentParts[1]); // Parse the port number from the message
        } catch (NumberFormatException e) {
            // Handle the case where the port number is not a valid integer
            System.out.println("Invalid port number in download initiation message."); // Debugging statement
            return; // Exit the method if the port number is invalid
        }

        System.out.println(filename + ": " + port + ": " + getServerAddress()); // Debugging statement

        // Run the following code on the JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                // Get the relative path for the downloads directory
                String relativePath = System.getProperty("user.dir") + "/downloads/";

                // Initiate the file download
                fileTransferManager.downloadFile(getServerAddress(), port, filename, relativePath + filename,
                        controller.getProgressBar());
            } catch (Exception e) {
                // Show an alert if the download failed
                showAlert("Download Failed", "Failed to initiate download for " + filename + ": " + e.getMessage());
            }
        });
    }

    /**
     * Sends a search request to the server with the given query.
     *
     * @param query The search query to be sent to the server.
     */
    public void sendSearchRequest(String query) {
        // Create a new Message object with the "search" type, the client's username,
        // null as the recipient, and the query string
        sendMessage(new Message("search", username, null, query));
    }

    /**
     * Handles the case when the chosen username is already taken.
     * Closes all resources, restarts the client, and prompts for a new username.
     */
    private void handleUsernameTaken() {
        // Run the following code on the JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                // Close the socket, input stream, and output stream
                closeEverything(socket, objectInputStream, objectOutputStream);
                // Restart the client and prompt for a new username
                restartClient(username);
            } catch (IOException e) {
                // Print the stack trace if an IOException occurs
                e.printStackTrace();
            }
        });
    }

    /**
     * Handles the search results received from the server.
     * Displays the search results on the user interface.
     *
     * @param results The search results received from the server.
     */
    private void handleSearchResults(String results) {
        // Run the following code on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Display the search results on the user interface
            controller.displaySearchResults(results);
        });
    }

    /**
     * Handles the scenario when the server is shutting down.
     * Closes all resources, prints a message, and exits the application.
     */
    private void handleServerDown() {
        // Run the following code on the JavaFX Application Thread
        Platform.runLater(() -> {
            // Close the socket, input stream, and output stream
            closeEverything(socket, objectInputStream, objectOutputStream);

            // Print a message indicating that the server is down and disconnecting clients
            System.out.println("SERVER: Server down, disconnecting clients...");

            // Exit the application with a status code of 0 (normal termination)
            System.exit(0);
        });
    }

    /**
     * Handles an exception by closing all resources and printing the error message.
     *
     * @param errorMessage The error message to be printed.
     */
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
