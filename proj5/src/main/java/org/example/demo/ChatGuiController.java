package org.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.util.Callback;
import javafx.scene.control.TextField;

/**
 * The ChatGuiController class is responsible for handling the user interface
 * interactions within the chat window of the chat application.
 */
public class ChatGuiController extends Application {
    private String username;
    private Client client;
    private FileTransferManager fileTransferManager;
    private boolean isDownloadPaused;
    @FXML
    private TextField searchInput;
    @FXML
    private ListView<String> searchResultsListView;
    @FXML
    private Button btnSearch, btnDownload, btnPauseDownload;
    @FXML
    private ProgressBar downloadProgress;

    /**
     * Default constructor for ChatGuiController.
     */
    public ChatGuiController() {
        // Default constructor is required for FXML loading
        fileTransferManager = new FileTransferManager(downloadProgress);
    }

    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }

    /**
     * The main entry point for the JavaFX application.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Sets the username for the client using this chat GUI.
     *
     * @param username The username of the client.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the client instance associated with this chat GUI.
     *
     * @param client The client instance.
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /*
     * public void setFileManager(FileTransferManager fileManager) {
     * this.fileTransferManager = fileManager;
     * }
     */

    /**
     * Initializes the controller and sets up event handlers and list cell
     * formatting.
     */
    public void initialize() {
        // Set the event handler for the search button
        btnSearch.setOnAction(event -> handleSearchButton());

        System.out.println("Search button was clicked"); // Debugging statement

        // Set a custom cell factory for the search results list view
        searchResultsListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null); // Clear the cell text if the item is null or empty
                        } else {
                            setText(formatSearchResult(item)); // Format the search result for display
                        }
                    }
                };
            }
        });
    }

    /**
     * Handles the download button action.
     * Sends a download request to the server for the selected search result.
     */
    @FXML
    private void handleDownloadButton() {
        System.out.println("Download button is called"); // Debugging statement

        // Get the selected item from the search results list view
        String selectedItem = searchResultsListView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            System.out.println("selectedItem is not null"); // Debugging statement

            // Create a new download request message
            Message downloadRequest = new Message("downloadRequest", username, username, selectedItem);

            // Send the download request to the server
            client.sendMessage(downloadRequest);
        }
    }

    /**
     * Handles the search button action.
     * Retrieves the search query from the input field, sends it to the server,
     * and clears the input field.
     */
    @FXML
    private void handleSearchButton() {
        String query = searchInput.getText().trim();
        if (!query.isEmpty()) {
            client.sendSearchRequest(query); // Send the search query to the server
            searchInput.clear(); // Clear the input field after sending the request
        }
    }

    /**
     * Handles the pause button action.
     * Pauses or resumes the file download process based on the current download
     * state.
     */
    @FXML
    private void handlePauseButton() {
        System.out.println("Pause button is linked"); // Debugging statement

        // Pause the current download
        fileTransferManager.pauseDownload();

        // Check the current download state
        if (isDownloadPaused) {
            // If the download is paused, resume it
            fileTransferManager.resumeDownload();
            isDownloadPaused = false;
        } else {
            // If the download is in progress, pause it
            fileTransferManager.pauseDownload();
            isDownloadPaused = true;
        }
    }

    /**
     * Returns the progress bar used for showing file download progress.
     * 
     * @return ProgressBar the progress bar control.
     */
    public ProgressBar getProgressBar() {
        return downloadProgress;
    }

    public void displaySearchResults(String results) {
        Platform.runLater(() -> {
            searchResultsListView.getItems().clear();
            btnSearch.setDisable(false); // Re-enable the button
            if (results != null && !results.isEmpty()) {
                searchResultsListView.getItems().addAll(results.split(", "));
            } else {
                searchResultsListView.getItems().add("No results found.");
            }
        });
    }

    // Format search result string for display
    private String formatSearchResult(String result) {
        // You can format the string however you need; this is a simple example
        return "File: " + result;
    }

    /**
     * Starts the JavaFX application. This method is not used in this controller.
     *
     * @param primaryStage The primary stage for this application.
     * @throws Exception If an error occurs during application start.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

}
