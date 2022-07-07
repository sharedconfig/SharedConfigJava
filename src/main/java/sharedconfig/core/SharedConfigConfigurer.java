package sharedconfig.core;

import lombok.extern.log4j.Log4j2;
import sharedconfig.core.exceptions.SharedConfigConfigurerException;
import sharedconfig.helpers.ClassLocationHelper;
import sharedconfig.helpers.FileHelper;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class SharedConfigConfigurer {
    /**
     * Если приложение запущено в контексте jar - извлекает указанную директорию с конфигурационными файлами из ресурсов и перемещает ее в папку, в которой находится jar
     * Если нет - ничего не делает
     *
     * @param packageMarkerType   класс из сборки, в ресурсах которой лежит директория с конфигурационными файлами
     * @param resourceFolderName относительный путь папке к директории с конфигурацией (отноcительно classpath)
     * @return абсолютный путь к папке, в которую были извлечены файлы конфиграции
     */
    public static String ensureConfigurationFilesExtracted(Class<?> packageMarkerType, String resourceFolderName) throws SharedConfigConfigurerException {
        return ensureConfigurationFilesExtracted(packageMarkerType, resourceFolderName, resourceFolderName);
    }

    /**
     * Если приложение запущено в контексте jar - извлекает указанную директорию с конфигурационными файлами из ресурсов и перемещает ее по указанному пути
     * Если нет - ничего не делает
     *
     * @param packageMarkerType  класс из сборки, в ресурсах которой лежит директория с конфигурационными файлами
     * @param resourceFolderName относительный путь папке к директории с конфигурацией (отноcительно classpath)
     * @param targetFolderName   относительный (относительно current execution folder) или абсолютный путь к директории, в которую будут извлечены конфигурационные файлы
     * @return абсолютный путь к папке, в которую были извлечены файлы конфиграции
     */
    public static String ensureConfigurationFilesExtracted(Class<?> packageMarkerType, String resourceFolderName, String targetFolderName) throws SharedConfigConfigurerException {
        log.debug("Trying to ensure configuration files extracted");

        var currentExecutable = ClassLocationHelper.urlToFile(ClassLocationHelper.getLocation(packageMarkerType));
        var currentExecutableFolder = currentExecutable.isDirectory() ? currentExecutable.toString() : currentExecutable.getParentFile().toString();

        if (currentExecutable.isDirectory()) {
            log.info("Found directory enviroment");
            return FileHelper.combinePaths(currentExecutableFolder, resourceFolderName).toString();
        }
        if (currentExecutable.getName().endsWith(".jar")) {
            log.info("Found jar enviroment: {}", currentExecutable.getName());
            log.info("Found\r jar\n enviroment\\: {}", currentExecutable.getName());
            return extractConfigurationFolderFromJar(packageMarkerType, resourceFolderName, currentExecutable, Paths.get(targetFolderName)).toString();
        }
        throw new SharedConfigConfigurerException("Unsupported packaging type");
    }

    /**
     * Extract all configuration files from folderName
     *
     * @param packageMarkerType класс из сборки, в которой лежит директория с конфигурацией
     * @param folderName        имя директории в которой лежит конфигурация
     * @param currentExecutable исполняемый файл
     * @return возвращает новую директорию с конфигурационными файлами
     * @throws SharedConfigConfigurerException если не получилось извлечь файлы из jar файла
     */
    private static Path extractConfigurationFolderFromJar(Class<?> packageMarkerType, String folderName, File currentExecutable, Path extractConfigurationPath) throws SharedConfigConfigurerException {
        try {
            var jarPath = currentExecutable.toString().replace('\\', '/');
            var jarName = currentExecutable.getName();
            log.debug("Jar Name: {}", jarName);

            // get paths from src/main/resources/up-configuration
            var folderPathInJar = getFolderPathInJar(packageMarkerType, jarPath, folderName);
            var result = getPathsFromResourceJAR(jarPath, folderPathInJar);
            var newCurrentExecutableFolder = createNewExtractConfigurationPath(jarPath, folderName, extractConfigurationPath);
            for (Path path : result) {
                log.debug("Path: {} ", path);

                var filePathInJAR = path.toString();
                // Windows will returns /up-configuration/file1.json, cut the first /
                // the correct path should be up-configuration/file1.json
                if (filePathInJAR.startsWith("/")) {
                    filePathInJAR = filePathInJAR.substring(1);
                }

                log.debug("filePathInJAR: {} ", filePathInJAR);

                // read a file from resource folder
                var is = getFileFromResourceAsStream(filePathInJAR);
                // move a file to newCurrentExecutableFolder
                moveFile(is, newCurrentExecutableFolder, filePathInJAR, folderPathInJar);
            }
            return newCurrentExecutableFolder;

        } catch (URISyntaxException | IOException | SharedConfigConfigurerException e) {
            throw new SharedConfigConfigurerException(String.format("Не удалось извлечь папку c конфигурацией: [%s], из jar файла!", folderName), e);
        }
    }

    /**
     * Get path folder in jar
     *
     * @param packageMarkerType класс из сборки, в которой лежит директория с конфигурацией
     * @param jarPath           путь к исполняемому jar файлу
     * @param folderName        имя директории в которой лежит конфигурация
     * @return возвращает путь директория внутри jar файла
     */
    private static String getFolderPathInJar(Class<?> packageMarkerType, String jarPath, String folderName) throws SharedConfigConfigurerException {
        var folderPathInJar = Optional.ofNullable(packageMarkerType.getClassLoader().getResource(folderName))
                .map(URL::getPath)
                .orElseThrow(() -> new SharedConfigConfigurerException(String.format("Невозможно найти папку %s в ресурсах сборки", folderName)));
        log.debug("folderPathInJar: {}", folderPathInJar);

        folderPathInJar = jarPath.startsWith("/") ? folderPathInJar.replaceAll("file:", "") : folderPathInJar.replaceAll("file:/", "");
        folderPathInJar = folderPathInJar.replaceAll("!", "");
        if (folderPathInJar.startsWith(jarPath)) {
            folderPathInJar = folderPathInJar.replaceAll(jarPath, "");
        }
        if (folderPathInJar.startsWith("/")) {
            folderPathInJar = folderPathInJar.substring(1);
        }
        log.debug("Folder In Jar: {}", folderPathInJar);
        return folderPathInJar;
    }

    /**
     * Get all paths from a folderPathInJar that inside the JAR file
     *
     * @param jarPath         путь к исполняемому jar файлу
     * @param folderPathInJar путь к директории в которой лежит конфигурация внутри исполняемого jar файла
     * @return возвращает пути до конфигурационных файлов внутри jar файла
     * @throws URISyntaxException
     * @throws IOException
     * @throws SharedConfigConfigurerException если не обнаружен jar файл
     */
    private static List<Path> getPathsFromResourceJAR(String jarPath, String folderPathInJar) throws URISyntaxException, IOException, SharedConfigConfigurerException {

        log.debug("JAR Path: {} ", jarPath);

        var platformJarPath = jarPath.startsWith("/") ? jarPath : "/" + jarPath;
        log.debug("platformJarPath: {} ", platformJarPath);
        // file walks JAR
        var uri = URI.create("jar:file:" + platformJarPath);
        try (var fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            return Files.walk(fs.getPath(folderPathInJar))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (NoSuchFileException e) {
            throw new SharedConfigConfigurerException(String.format("В jar файле путь: [%s] не обнаружен", folderPathInJar), e);
        } catch (IOException e) {
            throw new SharedConfigConfigurerException(e);
        }
    }

    /**
     * Get a file from the resources folder
     * works everywhere, IDEA, unit test and JAR file.
     *
     * @param fileName имя файла для считывания входным потоком
     * @return возвращает поток ввода с считанным файлом
     * @throws SharedConfigConfigurerException если не удалось найти файл
     */
    private static InputStream getFileFromResourceAsStream(String fileName) throws SharedConfigConfigurerException {

        // The class loader that loaded the class
        var classLoader = SharedConfigConfigurer.class.getClassLoader();
        var inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new SharedConfigConfigurerException(String.format("Файл не найден: [%s]", fileName));
        } else {
            return inputStream;
        }
    }

    /**
     * Create new path near jar
     *
     * @param jarPath                  путь к исполняемому jar файлу
     * @param folderName               имя директории в которой лежит конфигурация
     * @param extractConfigurationPath путь извлечения директории с конфигурацией
     * @return возвращает путь новой директории находящейся возле jar
     */
    private static Path createNewExtractConfigurationPath(String jarPath, String folderName, Path extractConfigurationPath) {
        // get newCurrentExecutableFolder from jarPath
        if (extractConfigurationPath.toString().equals(folderName)) {
            jarPath = jarPath.replace('\\', '/');
            var currentExecutableFolder = Paths.get(jarPath).getParent();
            extractConfigurationPath = FileHelper.combinePaths(currentExecutableFolder.toString(), folderName);
        }

        // очищаем директории после предыдущего запуска
        if (Files.exists(extractConfigurationPath))
            FileHelper.deleteDirectoryContent(extractConfigurationPath.toString());

        log.debug("newCurrentExecutableFolder {} ", extractConfigurationPath);
        return extractConfigurationPath;
    }

    /**
     * Move file in new folder
     *
     * @param fileToMove                 файл который нужно переместить
     * @param newCurrentExecutableFolder новый путь по которому нужно переместить файл
     * @param filePathInJAR              путь файла для перемещения в jar файле
     * @param folderPathInJar            путь к директории в которой лежит конфигурация внутри исполняемого jar файла
     * @throws SharedConfigConfigurerException если не удалось записать файл по заданному пути
     */
    private static void moveFile(InputStream fileToMove, Path newCurrentExecutableFolder, String filePathInJAR, String folderPathInJar) throws SharedConfigConfigurerException {
        // get fileName from two paths
        var pathFolder = Paths.get(folderPathInJar);
        var pathFilePathInJAR = Paths.get(filePathInJAR);
        var fileName = pathFolder.relativize(pathFilePathInJAR);
        log.debug("fileName: {}", fileName);

        var streamReader = new InputStreamReader(fileToMove, StandardCharsets.UTF_8);
        var appDeclXmlContent = new BufferedReader(streamReader).lines().collect(Collectors.toList());
        var appDeclXmlCopyPath = FileHelper.combinePaths(newCurrentExecutableFolder.toString(), fileName.toString());
        // создаем папку если она не существует
        if (!Files.exists(appDeclXmlCopyPath.getParent()))
            try {
                Files.createDirectories(appDeclXmlCopyPath.getParent());
            } catch (IOException e) {
                throw new SharedConfigConfigurerException(String.format("Не удалось созадать новую дирректорию: [%s]", appDeclXmlCopyPath.getParent()), e);
            }

        try {
            Files.write(appDeclXmlCopyPath, appDeclXmlContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SharedConfigConfigurerException(String.format("Не удалось записать файл: [%s] по пути : [%s]", fileName, appDeclXmlCopyPath), e);
        }
        log.info("File {} moved to {}", fileName, appDeclXmlCopyPath);
    }

}
