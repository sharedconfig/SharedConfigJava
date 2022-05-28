package sharedconfig.helpers;

import lombok.extern.log4j.Log4j2;
import sharedconfig.core.exceptions.ApplicationSettingsCreationException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class SharedConfigConfigurer {
    /**
     * Если приложение запущено в контексте jar - извлекает папку up-configuration с конфигурационными файлами xml в папку с jar
     * Если нет - ничего не делает
     * @return current executable folder
     */
    public static String ensureConfigurationFilesExtracted(Class neededClass) throws IOException {
        log.info("Trying to ensure configuration files extracted");

        var currentExecutable = ClassLocationHelper.urlToFile(ClassLocationHelper.getLocation(neededClass));
        var currentExecutableFolder = currentExecutable.isDirectory() ? currentExecutable.toString() : currentExecutable.getParentFile().toString();

        if (!currentExecutable.isDirectory()) {
            try {
                String folder = "BOOT-INF/classes/up-configuration";
                String folderPath = "/" + currentExecutable.toString().replace('\\','/');

                // get paths from src/main/resources/up-configuration
                List<Path> result = getPathsFromResourceJAR(folderPath, folder);
                Path newCurrentExecutableFolder = createFolder(folderPath);
                for (Path path : result) {
                    log.info("Path: {} ", path);

                    String filePathInJAR = path.toString();
                    // Windows will returns /up-configuration/file1.json, cut the first /
                    // the correct path should be up-configuration/file1.json
                    if (filePathInJAR.startsWith("/")) {
                        filePathInJAR = filePathInJAR.substring(1, filePathInJAR.length());
                    }

                    log.info("filePathInJAR: {} ", filePathInJAR);

                    // read a file from resource folder
                    InputStream is = getFileFromResourceAsStream(filePathInJAR);
                    // move a file to folderPath
                    moveFiles(is, newCurrentExecutableFolder, filePathInJAR, folder);
                }

            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        }

        return currentExecutableFolder;
    }

    // Get all paths from a folder that inside the JAR file
    private static List<Path> getPathsFromResourceJAR(String jarPath, String folder) throws URISyntaxException, IOException {

        List<Path> result = null;

        log.info("JAR Path: {} ", jarPath);

        // file walks JAR
        URI uri = URI.create("jar:file:" + jarPath);
        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            result = Files.walk(fs.getPath(folder))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    // get a file from the resources folder
    // works everywhere, IDEA, unit test and JAR file.
    private static InputStream getFileFromResourceAsStream(String fileName) {

        // The class loader that loaded the class
        ClassLoader classLoader = SharedConfigConfigurer.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    private static Path createFolder(String movePath) throws IOException {
        // get currentExecutableFolder from movePath
        if (movePath.startsWith("/")) {
            movePath = movePath.substring(1, movePath.length());
        }
        movePath.replace('\\','/');
        String newMovePath = movePath.replaceAll("\\SharedConfigJavaTestApp-0.0.1-SNAPSHOT.jar", "");
        String newMovePathFolder = newMovePath + "up-configuration";
        // Path currentExecutableFolder = Paths.get(newMovePath);

        Path newCurrentExecutableFolder = Paths.get(newMovePathFolder);
        Files.createDirectories(newCurrentExecutableFolder);
        log.info("newCurrentExecutableFolder {} ", newCurrentExecutableFolder);
        return newCurrentExecutableFolder;
    }

    private static void moveFiles(InputStream fileToMove, Path newCurrentExecutableFolder, String filePathInJAR, String folder) throws IOException {
        // get fileName from two paths
        Path pathFolder = Paths.get(folder);
        Path pathFilePathInJAR = Paths.get(filePathInJAR);
        Path fileName = pathFolder.relativize(pathFilePathInJAR);
        log.info("fileName: {}", fileName);

        InputStreamReader streamReader = new InputStreamReader(fileToMove, StandardCharsets.UTF_8);
        var appDeclXmlContent= new BufferedReader(streamReader).lines().collect(Collectors.toList());
        var appDeclXmlCopyPath = FileHelper.combinePaths(newCurrentExecutableFolder.toString(), fileName.toString());
        Files.write(appDeclXmlCopyPath, appDeclXmlContent, StandardCharsets.UTF_8);
        log.info("File {} moved to {}", fileName, appDeclXmlCopyPath);
    }
}
