package sharedconfig.helpers;

import lombok.extern.log4j.Log4j2;
import sharedconfig.core.exceptions.SharedConfigException;

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
     *
     * @param packageMarkerType
     * @param folderName
     * @return current executable folder
     * @throws SharedConfigException
     */
    public static String ensureConfigurationFilesExtracted(Class packageMarkerType, String folderName) throws SharedConfigException {
        log.info("Trying to ensure configuration files extracted");

        var currentExecutable = ClassLocationHelper.urlToFile(ClassLocationHelper.getLocation(packageMarkerType));
        var currentExecutableFolder = currentExecutable.isDirectory() ? currentExecutable.toString() : currentExecutable.getParentFile().toString();

        if (!currentExecutable.isDirectory())
            try {
                String jarPath = "/" + currentExecutable.toString().replace('\\', '/');
                String jarName = currentExecutable.getName();
                log.info("Jar Name: {}", jarName);

                // get paths from src/main/resources/up-configuration
                var folderPathInJar = getFolderPathInJar(packageMarkerType, jarPath, folderName);
                List<Path> result = getPathsFromResourceJAR(jarPath, folderPathInJar);
                Path newCurrentExecutableFolder = createFolder(jarPath, jarName, folderName);
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
                    // move a file to newCurrentExecutableFolder
                    moveFile(is, newCurrentExecutableFolder, filePathInJAR, folderPathInJar);
                }

            } catch (URISyntaxException | IOException e) {
                throw new SharedConfigException(e);
            }

        return currentExecutableFolder;
    }

    /**
     * Get path folder in jar
     *
     * @param packageMarkerType
     * @param jarPath
     * @param folderName
     * @return
     */
    private static String getFolderPathInJar(Class packageMarkerType, String jarPath, String folderName) {
        var folderPathInJar = packageMarkerType.getClassLoader().getResource(folderName).getPath();
        log.info("folderPathInJar: {}", folderPathInJar);

        if (folderPathInJar.startsWith("file:")) {
            folderPathInJar = folderPathInJar.replaceAll("file:", "");
            folderPathInJar = folderPathInJar.replaceAll("!", "");
        }
        if (folderPathInJar.startsWith(jarPath)) {
            folderPathInJar = folderPathInJar.replaceAll(jarPath, "");
        }
        if (folderPathInJar.startsWith("/")) {
            folderPathInJar = folderPathInJar.substring(1, folderPathInJar.length());
        }
        log.info("Folder In Jar: {}", folderPathInJar);
        return folderPathInJar;
    }

    /**
     * Get all paths from a folderPathInJar that inside the JAR file
     *
     * @param jarPath
     * @param folderPathInJar
     * @return
     * @throws URISyntaxException
     * @throws IOException
     * @throws SharedConfigException
     */
    private static List<Path> getPathsFromResourceJAR(String jarPath, String folderPathInJar) throws URISyntaxException, IOException, SharedConfigException {

        List<Path> result = null;
        log.info("JAR Path: {} ", jarPath);

        // file walks JAR
        URI uri = URI.create("jar:file:" + jarPath);
        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            result = Files.walk(fs.getPath(folderPathInJar))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            return result;
        } catch (NoSuchFileException e) {
            throw new SharedConfigException(String.format("В jar файле путь: [%s] не обнаружен", folderPathInJar));
        } catch (IOException e) {
            throw new SharedConfigException(e);
        }
    }

    /**
     *get a file from the resources folder
     * works everywhere, IDEA, unit test and JAR file.
     *
     * @param fileName
     * @return
     * @throws SharedConfigException
     */
    private static InputStream getFileFromResourceAsStream(String fileName) throws SharedConfigException {

        // The class loader that loaded the class
        ClassLoader classLoader = SharedConfigConfigurer.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new SharedConfigException(String.format("File not found: [%s]", fileName));
        } else {
            return inputStream;
        }
    }

    /**
     * Create new folder in pathToJar
     *
     * @param pathToJar
     * @param jarName
     * @param folderName
     * @return
     * @throws IOException
     */
    private static Path createFolder(String pathToJar, String jarName, String folderName) throws IOException {
        // get newCurrentExecutableFolder from pathToJar
        if (pathToJar.startsWith("/")) {
            pathToJar = pathToJar.substring(1, pathToJar.length());
        }
        pathToJar.replace('\\', '/');
        String currentExecutableFolder = pathToJar.replaceAll(jarName, "");
        String newCurrentExecutableFolder = currentExecutableFolder + folderName;

        Path newCurrentExecutableFolderPath = Paths.get(newCurrentExecutableFolder);
        Files.createDirectories(newCurrentExecutableFolderPath);
        log.info("newCurrentExecutableFolder {} ", newCurrentExecutableFolderPath);
        return newCurrentExecutableFolderPath;
    }

    /**
     * Move file in new folder
     *
     * @param fileToMove
     * @param newCurrentExecutableFolder
     * @param filePathInJAR
     * @param folderPathInJar
     * @throws IOException
     */
    private static void moveFile(InputStream fileToMove, Path newCurrentExecutableFolder, String filePathInJAR, String folderPathInJar) throws IOException {
        // get fileName from two paths
        Path pathFolder = Paths.get(folderPathInJar);
        Path pathFilePathInJAR = Paths.get(filePathInJAR);
        Path fileName = pathFolder.relativize(pathFilePathInJAR);
        log.info("fileName: {}", fileName);

        InputStreamReader streamReader = new InputStreamReader(fileToMove, StandardCharsets.UTF_8);
        var appDeclXmlContent = new BufferedReader(streamReader).lines().collect(Collectors.toList());
        var appDeclXmlCopyPath = FileHelper.combinePaths(newCurrentExecutableFolder.toString(), fileName.toString());
        Files.write(appDeclXmlCopyPath, appDeclXmlContent, StandardCharsets.UTF_8);
        log.info("File {} moved to {}", fileName, appDeclXmlCopyPath);
    }
}
