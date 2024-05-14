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
    @FXML
    private TextField searchInput;
    @FXML
    private ListView<String> searchResultsListView;
    @FXML
    private Button btnSearch, btnDownload;
    @FXML
    private ProgressBar downloadProgress;

    /**
     * Default constructor for ChatGuiController.
     */
    public ChatGuiController() {
        // Default constructor is required for FXML loading
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

    public void initialize() {
        btnSearch.setOnAction(event -> handleSearchButton());
        // Custom cell factory to format list cells
        System.out.println("Search button was clicked");
        searchResultsListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setText(null);
                        } else {
                            setText(formatSearchResult(item));
                        }
                    }
                };
            }
        });
    }

    @FXML
    private void handleDownloadButton() {
        String selectedItem = searchResultsListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            // Send a download request to the server
            Message downloadRequest = new Message("downloadRequest", username, username, selectedItem);
            client.sendMessage(downloadRequest);
        }
    }

    @FXML
    private void handleSearchButton() {
        String query = searchInput.getText().trim();
        if (!query.isEmpty()) {
            client.sendSearchRequest(query); // Send the search query to the server
            searchInput.clear(); // Clear the input field after sending the request
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
