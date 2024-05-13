package org.example.group_36_project_5;

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

    public void appendFileList(File[] files) {

        ListOfFilesTA.clear();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    ListOfFilesTA.appendText(file.getName() + "\n");
                }
            }
        } else {
            ListOfFilesTA.appendText("No files found in the directory.");
        }
    }
}
