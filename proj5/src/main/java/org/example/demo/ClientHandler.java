package org.example.demo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * The ClientHandler class is responsible for managing individual client
 * connections to the server, including sending and receiving messages.
 */
public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String clientUsername;

    private static final String ENCRYPTION_KEY = "mySecretKey";

    /**
     * Constructs a ClientHandler instance with a specified socket.
     * Initializes the streams and sets up the client connection.
     *
     * @param socket The socket representing the client connection.
     */
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectOutputStream.flush();
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());

            Message usernameMessage = (Message) objectInputStream.readObject();
            this.clientUsername = usernameMessage.getSender();

            if (!Server.activeUsernames.add(this.clientUsername)) {
                sendMessage(new Message("login", "SERVER", null, "Username is already taken."));
                closeEverything();
                return;
            }

            clientHandlers.add(this);
            Server.activeUsernames.add(this.clientUsername);

            Server.updateClientActivity("New user has connected");

        } catch (IOException | ClassNotFoundException e) {
            closeEverything();
        }
    }

    /**
     * The main run method that listens for incoming messages from the client
     * and handles them accordingly.
     */
    public void run() {
        try {
            while (socket.isConnected()) {
                // Read an object from the input stream
                Message messageFromClient = (Message) objectInputStream.readObject();

                if (messageFromClient != null) {
                    // Decrypt the message content
                    String decryptedContent = Encryption.decrypt(messageFromClient.getContent(), ENCRYPTION_KEY);

                    // Create a new message object with the decrypted content
                    Message decryptedMessage = new Message(
                            messageFromClient.getType(),
                            messageFromClient.getSender(),
                            messageFromClient.getRecipient(),
                            decryptedContent);

                    // Handle different message types
                    switch (decryptedMessage.getType()) {
                        case "search":
                            handleSearchRequest(decryptedMessage);
                            break;
                        case "downloadRequest":
                            handleDownloadRequest(decryptedMessage);
                            break;
                        case "fileAvailable":
                            handleFileAvailable(decryptedMessage);
                            break;
                        default:
                            System.out.println("Unhandled message type: " + decryptedMessage.getType());
                            break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // Handle exceptions when the client disconnects
            System.out.println(clientUsername + " has disconnected.");
            Server.updateClientActivity("A user has disconnected");
        } finally {
            // Remove the client handler from the server
            removeClientHandler();
        }
    }

    /**
     * Handles a search request from the client.
     *
     * @param message The message containing the search query.
     */
    private void handleSearchRequest(Message message) {
        // Print the message content for debugging
        System.out.println("Message content: " + message.getContent());

        // Perform the search using the server's search manager
        List<String> searchResults = Server.searchManager.searchFiles(message.getContent());

        // Create a new message with the search results
        Message resultsMessage = new Message(
                "searchResults", // Message type
                "SERVER", // Sender (the server itself)
                clientUsername, // Recipient (the client who sent the search request)
                String.join(", ", searchResults) // Content (search results as a comma-separated string)
        );

        // Send the search results message to the client
        sendMessage(resultsMessage);
    }

    /**
     * Handles a download request from a client.
     * Broadcasts the request to other clients to check for file availability.
     *
     * @param message The message containing the requested file name.
     */
    private void handleDownloadRequest(Message message) {
        System.out.println("download request in client handler1"); // Debugging statement

        // Get the requested file name from the message content
        String requestedFile = message.getContent();

        // Iterate over all connected clients
        for (ClientHandler clientHandler : clientHandlers) {
            // Skip the client who initiated the download request
            if (!clientHandler.clientUsername.equals(this.clientUsername)) {
                System.out.println("download request in client handler"); // Debugging statement

                // Send a "checkFile" message to other clients to check for file availability
                clientHandler
                        .sendMessage(new Message("checkFile", clientUsername, message.getRecipient(), requestedFile));
            }
        }
    }

    /**
     * Handles a message indicating that a file is available for download.
     * Initiates the download process with the client who has the file.
     *
     * @param message The message containing the file information and the sender's
     *                details.
     */
    private void handleFileAvailable(Message message) {
        System.out.println("start init download method"); // Debugging statement
        System.out.println(message.getSender()); // Debugging statement
        System.out.println(message.getRecipient()); // Debugging statement

        // Find the client who requested the file download
        ClientHandler recipient = findClientHandler(message.getRecipient());

        // If the recipient is found
        if (recipient != null) {
            System.out.println("initiating download"); // Debugging statement

            // Send an "initiateDownloadFrom" message to the recipient with file information
            recipient.sendMessage(new Message("initiateDownloadFrom", clientUsername, null, message.getContent()));
        }
    }

    /**
     * Finds a ClientHandler instance based on the provided username.
     *
     * @param username The username to search for.
     * @return The ClientHandler instance associated with the provided username, or
     *         null if not found.
     */
    private ClientHandler findClientHandler(String username) {
        // Iterate over all connected clients
        for (ClientHandler handler : clientHandlers) {
            System.out.println(handler.clientUsername); // Debugging statement

            // Check if the client's username matches the provided username
            if (handler.clientUsername.equals(username)) {
                return handler; // Return the matching ClientHandler instance
            }
        }

        return null; // Return null if no matching client is found
    }

    /**
     * Sends a message to the client after encrypting the content.
     *
     * @param message The message to be sent.
     */
    private void sendMessage(Message message) {
        try {
            // Encrypt the message content
            String encryptedContent = Encryption.encrypt(message.getContent(), ENCRYPTION_KEY);

            // Create a new message with the encrypted content
            Message encryptedMessage = new Message(message.getType(), message.getSender(), message.getRecipient(),
                    encryptedContent);

            // Write the encrypted message to the output stream
            objectOutputStream.writeObject(encryptedMessage);
            objectOutputStream.flush();
        } catch (IOException e) {
            // If an IOException occurs, close all resources
            closeEverything();
        }
    }

    /**
     * Removes the ClientHandler instance from the server and closes all resources.
     */
    public void removeClientHandler() {
        // Remove the client's username from the list of active usernames
        Server.activeUsernames.remove(this.clientUsername);

        // Remove this ClientHandler instance from the list of client handlers
        clientHandlers.remove(this);

        // Close all resources
        closeEverything();
    }

    /**
     * Closes the input and output streams, and the socket connection.
     */
    private void closeEverything() {
        try {
            // Close the output stream if it's not null
            if (objectOutputStream != null)
                objectOutputStream.close();

            // Close the input stream if it's not null
            if (objectInputStream != null)
                objectInputStream.close();

            // Close the socket connection if it's not null
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            // Print the stack trace if an IOException occurs
            e.printStackTrace();
        }
    }
}
