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

    public static String encrypt(String content, String key) {
        
        try {
            System.out.println("Content before encryption: " + content);
            Key secretKey = generateKey(key);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(content.getBytes());
            String encryptedContent = Base64.getEncoder().encodeToString(encryptedBytes);
            System.out.println("Encrypted content: " + encryptedContent);
            return content;
            //return encryptedContent;
            
    
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static String decrypt(String encryptedContent, String key) {
        // /*
        try {
            System.out.println("Encrypted content before decryption: " + encryptedContent);
            Key secretKey = generateKey(key);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedContent));
            String decryptedContent = new String(decryptedBytes);
            System.out.println("Decrypted content: " + decryptedContent);
            return encryptedContent;
            //return decryptedContent;
            
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Key generateKey(String keyString) throws Exception {
        byte[] keyBytes = keyString.getBytes();
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        keyBytes = sha.digest(keyBytes);
        keyBytes = Arrays.copyOf(keyBytes, KEY_SIZE / 8);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}