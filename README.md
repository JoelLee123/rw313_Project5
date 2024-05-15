# Peer-to-Peer File Sharing Application

This is a peer-to-peer file sharing application that allows users to search for and download files from other connected peers. The application uses a client-server architecture, where the server acts as a central coordinator for facilitating communication and file discovery among the clients.

## Features

- **User Registration and Authentication**: Users can create an account and log in with a unique username.
- **File Search**: Users can search for files available on the network by entering keywords or file names.
- **File Download**: Users can download files from other peers who have the requested file.
- **File Transfer**: Files are transferred directly between peers, without going through the server.
- **Progress Tracking**: Users can monitor the progress of file downloads through a progress bar.
- **Pause/Resume Downloads**: Users can pause and resume file downloads as needed.
- **Encryption**: All communications between the server and clients are encrypted for security purposes.

## Architecture

The application consists of three main components:

1. **Server**: The server acts as a central coordinator, facilitating communication between clients and managing user authentication and file search requests. It does not store any shared files.

2. **Client**: The client application provides a user interface for interacting with the application. It handles user input, sends search requests to the server, and initiates file downloads from other peers.

3. **File Transfer Manager**: Each client has a file transfer manager that handles incoming and outgoing file transfers. It listens for file transfer requests from other peers and establishes direct connections for transferring files.

## Technologies Used

- **Programming Language**: Java
- **User Interface**: JavaFX
- **Networking**: Java Sockets
- **Encryption**: Java Cryptography Architecture (JCA)

## Getting Started

To run the application, you need to have Java Development Kit (JDK) installed on your system. Follow these steps:

1. Clone the repository or download the source code.
2. Open the project in your preferred Java IDE.
3. Build and run the `Server` class to start the server.
4. Build and run the `Client` class to start the client application.
5. Register a new user or log in with an existing account.
6. Start searching for files and downloading them from other connected peers.

## Contributing

Contributions to this project are welcome! If you find any issues or have suggestions for improvements, please open an issue or submit a pull request. Make sure to follow the project's coding conventions and guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Acknowledgments

- The application uses the [Java Cryptography Architecture (JCA)](https://docs.oracle.com/javase/8/docs/technotes/guides/cryptography/intro.html) for encrypting communications.
- The user interface is built with [JavaFX](https://openjfx.io/).
