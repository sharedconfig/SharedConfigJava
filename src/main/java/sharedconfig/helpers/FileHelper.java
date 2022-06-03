package sharedconfig.helpers;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class FileHelper {
    public static boolean isValidPath(@Nullable String path) {
        try {
            assert path != null;
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }

    public static @NotNull Optional<@NotNull Path> tryGetPath(@Nullable String path) {
        try {
            assert path != null;
            return Optional.of(Paths.get(path));
        } catch (InvalidPathException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    public static Path combinePaths(String firstPath, String... otherPaths) {
        var file = new File(firstPath);
        for (String otherPath : otherPaths) {
            file = new File(file, otherPath);
        }
        return file.toPath().normalize();
    }


    public static Optional<Path> tryCombinePaths(String firstPath, String... otherPaths) {
        try {
            return Optional.of(combinePaths(firstPath, otherPaths));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static String readFile(String path) throws IOException {
        return readFile(path, StandardCharsets.UTF_8);
    }

    public static void deleteDirectoryContent(String dir) {
        var directory = Path.of(dir).toFile();
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
    }

    private static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static String removeIncorrectPathChars(String path) {
        return path.replaceAll("[^A-Za-z0-9_ ]", "");
    }

    public static boolean containsIncorrectPathChars(String path) {
        return path.matches("[^A-Za-z0-9_ ]");
    }
}
