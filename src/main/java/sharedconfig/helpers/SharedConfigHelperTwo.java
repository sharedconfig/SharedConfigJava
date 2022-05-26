package sharedconfig.helpers;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class SharedConfigHelperTwo {

    public static void ensureConfigurationFilesExtracted(String jarPath) throws IOException {
        // Sample 3 - read all files from a resources folder (JAR version)
        try {
            String folder = "BOOT-INF/classes/up-configuration";

            // get paths from src/main/resources/up-configuration
            List<Path> result = getPathsFromResourceJAR(jarPath, folder);
            for (Path path : result) {
                System.out.println("Path : " + path);

                String filePathInJAR = path.toString();
                // Windows will returns /json/file1.json, cut the first /
                // the correct path should be json/file1.json
                if (filePathInJAR.startsWith("/")) {
                    filePathInJAR = filePathInJAR.substring(1, filePathInJAR.length());
                }

                System.out.println("filePathInJAR : " + filePathInJAR);

                // read a file from resource folder
                InputStream is = getFileFromResourceAsStream(filePathInJAR);
                // такой должен быть путь
                // "C:\\Users\\Vasypu\\Desktop\\softconsalt\\SharedConfigJava\\SharedConfigJavaTestApp\\target\\app-declaration.xml"
                moveFiles(is, jarPath, filePathInJAR, folder);
            }

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    // Get all paths from a folder that inside the JAR file
    private static List<Path> getPathsFromResourceJAR(String jarPath, String folder) throws URISyntaxException, IOException {

        List<Path> result = null;

        System.out.println("JAR Path :" + jarPath);

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
        ClassLoader classLoader = SharedConfigHelperTwo.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

    private static void moveFiles(InputStream fileToMove, String movePath, String filePathInJAR, String folder) throws IOException {
        if (movePath.startsWith("/")) {
            movePath = movePath.substring(1, movePath.length());
        }
        movePath.replace('\\','/');
        String newMovePath = movePath.replaceAll("\\SharedConfigJavaTestApp-0.0.1-SNAPSHOT.jar", "");
        Path pathFolder = Paths.get(folder);
        Path pathFilePathInJAR = Paths.get(filePathInJAR);
        Path fileName = pathFolder.relativize(pathFilePathInJAR);
        log.info("fileName: {}", fileName);
        //var appDeclXmlCopyPath = FileHelper.combinePaths(currentExecutableFolder, fileToCheck);
        Path currentExecutableFolder = Paths.get(newMovePath);
        InputStreamReader streamReader = new InputStreamReader(fileToMove, StandardCharsets.UTF_8);
        var appDeclXmlContent= new BufferedReader(streamReader).lines().collect(Collectors.toList());
        var appDeclXmlCopyPath = FileHelper.combinePaths(currentExecutableFolder.toString(), fileName.toString());
        //var appDeclXmlCopyPath = FileHelper.combinePaths(currentExecutableFolder, filePathInJAR);
        Files.write(appDeclXmlCopyPath, appDeclXmlContent, StandardCharsets.UTF_8);
        log.info("Files moved !!!");
    }
}
