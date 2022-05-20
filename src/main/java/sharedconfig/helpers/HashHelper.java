package sharedconfig.helpers;

import lombok.SneakyThrows;
import lombok.val;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;

public class HashHelper {
    @SneakyThrows
    public static byte[] getSHA256Hash(String originalString) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    public static String getSHA256HashString(String originalString) {
        val digest = MessageDigest.getInstance("SHA-256");
        val hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
        val hexString = new StringBuilder();
        for (byte b : hash) {
            val hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static long getSimpleFileHash(File file) {
        if (file == null || !file.exists())
            return 0L;

        try {
            var fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

            var hashCode = fileAttributes.size();
            hashCode = (hashCode * 397) ^ fileAttributes.creationTime().toMillis();
            hashCode = (hashCode * 397) ^ fileAttributes.lastModifiedTime().toMillis();

            return hashCode;
        } catch (IOException e) {
            return 0L;
        }
    }
}
