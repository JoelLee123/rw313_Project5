package org.example.demo;

import java.util.concurrent.ConcurrentHashMap;

public class FileManager {
    static ConcurrentHashMap<String, String> fileOwners = new ConcurrentHashMap<>();

    public static void addFileOwner(String fileName, String username) {
        fileOwners.putIfAbsent(fileName, username);
    }

    public static String findFileOwner(String fileName) {
        return fileOwners.get(fileName);
    }

    public static void removeFileOwner(String fileName, String username) {
        fileOwners.remove(fileName, username);
    }
}
