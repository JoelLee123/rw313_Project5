package org.example.demo;

import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encryption {
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128;

    /**
     * Encrypts the given content using the provided key.
     *
     * @param content The content to be encrypted.
     * @param key     The encryption key.
     * @return The encrypted content, or null if an exception occurs.
     */
    public static String encrypt(String content, String key) {
        try {
            System.out.println("Content before encryption: " + content); // Debugging statement

            // Generate the secret key from the provided key
            Key secretKey = generateKey(key);

            // Get an instance of the cipher algorithm
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // Initialize the cipher in encryption mode with the secret key
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // Encrypt the content
            byte[] encryptedBytes = cipher.doFinal(content.getBytes());

            // Encode the encrypted bytes to a Base64 string
            String encryptedContent = Base64.getEncoder().encodeToString(encryptedBytes);

            System.out.println("Encrypted content: " + encryptedContent); // Debugging statement

            return encryptedContent;
        } catch (Exception e) {
            e.printStackTrace(); // Print the stack trace if an exception occurs
            return null;
        }
    }

    /**
     * Decrypts the given encrypted content using the provided key.
     *
     * @param encryptedContent The encrypted content to be decrypted.
     * @param key              The decryption key.
     * @return The decrypted content, or null if an exception occurs.
     */
    public static String decrypt(String encryptedContent, String key) {
        try {
            System.out.println("Encrypted content before decryption: " + encryptedContent); // Debugging statement

            // Generate the secret key from the provided key
            Key secretKey = generateKey(key);

            // Get an instance of the cipher algorithm
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // Initialize the cipher in decryption mode with the secret key
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            // Decode the encrypted content from Base64 to bytes
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedContent));

            // Convert the decrypted bytes to a string
            String decryptedContent = new String(decryptedBytes);

            System.out.println("Decrypted content: " + decryptedContent); // Debugging statement

            return decryptedContent;
        } catch (Exception e) {
            e.printStackTrace(); // Print the stack trace if an exception occurs
            return null;
        }
    }

    /**
     * Generates a secret key from the provided key string.
     *
     * @param keyString The key string.
     * @return The generated secret key.
     * @throws Exception If an exception occurs during key generation.
     */
    private static Key generateKey(String keyString) throws Exception {
        // Convert the key string to bytes
        byte[] keyBytes = keyString.getBytes();

        // Get an instance of the SHA-1 message digest
        MessageDigest sha = MessageDigest.getInstance("SHA-1");

        // Digest the key bytes using SHA-1
        keyBytes = sha.digest(keyBytes);

        // Truncate or pad the key bytes to the required length
        keyBytes = Arrays.copyOf(keyBytes, KEY_SIZE / 8);

        // Create a SecretKeySpec object with the key bytes and the algorithm
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}