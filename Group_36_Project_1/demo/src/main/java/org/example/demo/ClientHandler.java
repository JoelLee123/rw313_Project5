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
            this.clientUsername = usernameMessage.getContent();

            if (!Server.activeUsernames.add(this.clientUsername)) {
                sendMessage(new Message("login", "SERVER", null, "Username is already taken."));
                closeEverything();
                return;
            }

            clientHandlers.add(this);
            Server.activeUsernames.add(this.clientUsername);

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
            Message messageFromClient;
            while (socket.isConnected()) {
                messageFromClient = (Message) objectInputStream.readObject();
                if (messageFromClient != null) {
                    switch (messageFromClient.getType()) {
                        case "search":
                            handleSearchRequest(messageFromClient);
                            break;
                        case "downloadRequest":
                            handleDownloadRequest(messageFromClient);
                            break;
                        case "fileAvailable":
                            handleFileAvailable(messageFromClient);
                            break;
                        default:
                            System.out.println("Unhandled message type: " + messageFromClient.getType());
                            break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(clientUsername + " has disconnected.");
        } finally {
            removeClientHandler();
        }
    }

    private void handleSearchRequest(Message message) {
        List<String> searchResults = Server.searchManager.searchFiles(message.getContent());
        sendMessage(new Message("searchResults", "SERVER", clientUsername, String.join(", ", searchResults)));
    }

    private void handleDownloadRequest(Message message) {
        String requestedFile = message.getContent();
        for (ClientHandler clientHandler : clientHandlers) {
            if (!clientHandler.clientUsername.equals(this.clientUsername)) {
                System.out.println("download request in client handler");
                clientHandler.sendMessage(new Message("checkFile", clientUsername, null, requestedFile));
            }
        }
    }

    private void handleFileAvailable(Message message) {
        System.out.println("start init download method");
        System.out.println(message.getSender());
        // Find the client who requested the file download
        ClientHandler requester = findClientHandler(message.getSender());
        if (requester != null) {
            System.out.println("initiating download");
            requester.sendMessage(new Message("initiateDownloadFrom", clientUsername, null, message.getContent()));
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
            objectOutputStream.writeObject(message);
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
