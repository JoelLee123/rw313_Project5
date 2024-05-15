package org.example.demo;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import java.io.File;

/**
 * ServerController handles the user interface updates on the server side for a
 * chat application.
 * It provides methods to update the active user list and append activities to
 * the client activity log.
 */
public class ServerController {

    @FXML
    private TextArea ListOfFilesTA;

    @FXML
    private TextArea ClientActivityTA; // Text area to display client activity logs

    /**
     * Appends a new activity string to the client activity log.
     *
     * @param activity A string describing the client's activity.
     */
    public void appendClientActivity(String activity) {
        ClientActivityTA.appendText(activity + "\n");
    }
/**
 * Appends the names of files from a given array to a TextArea.
 *
 * @param files The array of files to be listed.
 */
public void appendFileList(File[] files) {
    // Clear the existing content of the TextArea
    ListOfFilesTA.clear();

    // Check if the input array is not null
    if (files != null) {
        // Iterate over each file in the array
        for (File file : files) {
            // Check if the current item is a file (not a directory)
            if (file.isFile()) {
                // Append the file name to the TextArea, followed by a newline character
                ListOfFilesTA.appendText(file.getName() + "\n");
            }
        }
    } else {
        // If the input array is null, display a message in the TextArea
        ListOfFilesTA.appendText("No files found in the directory.");
    }
}
}
