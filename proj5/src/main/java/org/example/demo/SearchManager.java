package org.example.demo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchManager {
    private String folderPath;
    private File[] fileList;

    public SearchManager(String folderPath) {
        System.out.println("Directory path: " + folderPath); // Log the path to check it
        this.folderPath = folderPath;
        updateFileList(); // Initialize file list at startup
    }

    public void updateFileList() {
        File folder = new File(folderPath);
        fileList = folder.listFiles();
        if (fileList == null) {
            System.out.println("No files found or not a directory");
        } else {
            System.out.println("Number of files found: " + fileList.length);


        }
    }

    /**
     * Searches for files that contain the specified query in their name.
     * 
     * @param query The substring to search for within file names.
     * @return List of matching file names.
     */
    public List<String> searchFiles(String query) {
        List<String> results = new ArrayList<>();
        if (fileList == null) {
            System.out.println("File list not initialized or directory is empty.");
            return results;
        }
        for (File file : fileList) {
            if (file.getName().contains(query)) {
                results.add(file.getName());
            }
        }
        System.out.println("Search results count: " + results.size());

        Server.updateClientActivity("Client searched for: " + query + " with " + results.size() + " results found");
        return results;
    }

}
