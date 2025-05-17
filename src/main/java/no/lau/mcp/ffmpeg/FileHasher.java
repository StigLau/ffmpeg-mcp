package no.lau.mcp.ffmpeg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHasher {



    /**
     * Generate an MD5 hash for a file
     * @param filePath Path to the file
     * @return MD5 hash of the file as a hex string
     */
    public static String getMd5Hash(Path filePath) throws IOException {
        MessageDigest md = digest();
        md.update(Files.readAllBytes(filePath));
        byte[] digestBytes = md.digest();
        return bytesToHex(digestBytes);
    }

    /**
     * Convert bytes to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm MD5 not available, " + e.getMessage());
        }
    }

    // Example usage
    /*
    public static void main(String[] args) {
        try {
            Path filePath = Path.of("pom.xml");
            String hash = getMd5Hash(filePath);
            System.out.println("MD5 Hash: " + hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

     */
}