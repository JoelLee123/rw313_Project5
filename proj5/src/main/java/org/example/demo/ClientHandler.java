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
     * Listens for messages from the connected client and processes them.
     */
    @Override
    public void run() {
        try {
            while (socket.isConnected()) {
                Message messageFromClient = (Message) objectInputStream.readObject();
                if (messageFromClient != null) {
                    String decryptedContent = Encryption.decrypt(messageFromClient.getContent(), ENCRYPTION_KEY);
                    Message decryptedMessage = new Message(messageFromClient.getType(), messageFromClient.getSender(),
                            messageFromClient.getRecipient(), decryptedContent);
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
            System.out.println(clientUsername + " has disconnected.");
            Server.updateClientActivity("A user has disconnected");
        } finally {
            removeClientHandler();
        }
    }

    private void handleSearchRequest(Message message) {
        System.out.println("Message content: " + message.getContent());
        List<String> searchResults = Server.searchManager.searchFiles(message.getContent());
        sendMessage(new Message("searchResults", "SERVER", clientUsername, String.join(", ", searchResults)));
    }

    private void handleDownloadRequest(Message message) {
        System.out.println("download request in client handler1");
        String requestedFile = message.getContent();
        for (ClientHandler clientHandler : clientHandlers) {
            if (!clientHandler.clientUsername.equals(this.clientUsername)) {
                System.out.println("download request in client handler");
                clientHandler
                        .sendMessage(new Message("checkFile", clientUsername, message.getRecipient(), requestedFile));
            }
        }
    }

    private void handleFileAvailable(Message message) {
        System.out.println("start init download method");
        System.out.println(message.getSender());
        System.out.println(message.getRecipient());
        // Find the client who requested the file download
        ClientHandler recipient = findClientHandler(message.getRecipient());
        if (recipient != null) {
            System.out.println("initiating download");
            recipient.sendMessage(new Message("initiateDownloadFrom", clientUsername, null, message.getContent()));
        }
    }

    private ClientHandler findClientHandler(String username) {
        for (ClientHandler handler : clientHandlers) {
            System.out.println(handler.clientUsername);
            if (handler.clientUsername.equals(username)) {
                return handler;
            }
        }
        return null;
    }

    private void sendMessage(Message message) {
        try {
            // Pretend to encrypt the content
            String encryptedContent = Encryption.encrypt(message.getContent(), ENCRYPTION_KEY);
            Message encryptedMessage = new Message(message.getType(), message.getSender(), message.getRecipient(),
                    encryptedContent);
            objectOutputStream.writeObject(encryptedMessage);
            objectOutputStream.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void removeClientHandler() {
        Server.activeUsernames.remove(this.clientUsername);
        clientHandlers.remove(this);
        closeEverything();
    }

    private void closeEverything() {
        try {
            if (objectOutputStream != null)
                objectOutputStream.close();
            if (objectInputStream != null)
                objectInputStream.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
