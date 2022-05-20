package sharedconfig.core;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import sharedconfig.core.exceptions.ApplicationException;
import sharedconfig.helpers.FileHelper;
import sharedconfig.helpers.HashHelper;
import sharedconfig.helpers.StringHelper;
import sharedconfig.helpers.XmlHelper;
import sharedconfig.utils.Either;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

@Getter
/* package */ class ApplicationContext {
    private final String name;
    private final String version;
    private final File baseDirectoryInfo;
    private final File storageDirectoryInfo;
    private File appInfoFileInfo;
    private final String agentDirectoryFullName;

    private File refFileInfo;
    private final long refFileCreate;
    private final long refFileWrite;
    private final long refFileLength;

    private File decFileInfo;
    private final long decFileCreate;
    private final long decFileWrite;
    private final long decFileLength;

    @SneakyThrows
    private ApplicationContext(File agentDirectory, String name, String version,
                               File baseDirectoryInfo, File decFileInfo, File storageDirectoryInfo,
                               File appInfoFileInfo, File refFileInfo) {
        this.name = name;
        this.version = version;
        this.baseDirectoryInfo = baseDirectoryInfo;
        this.storageDirectoryInfo = storageDirectoryInfo;
        this.appInfoFileInfo = appInfoFileInfo;

        this.agentDirectoryFullName = agentDirectory.getAbsolutePath();

        this.refFileInfo = refFileInfo;
        val refFileInfoAttributes = Files.readAttributes(refFileInfo.toPath(), BasicFileAttributes.class);
        this.refFileCreate = refFileInfoAttributes.creationTime().toMillis();
        this.refFileWrite = refFileInfoAttributes.lastModifiedTime().toMillis();
        this.refFileLength = refFileInfoAttributes.size();

        this.decFileInfo = decFileInfo;
        val decFileInfoAttributes = Files.readAttributes(decFileInfo.toPath(), BasicFileAttributes.class);
        this.decFileCreate = decFileInfoAttributes.creationTime().toMillis();
        this.decFileWrite = decFileInfoAttributes.lastModifiedTime().toMillis();
        this.decFileLength = decFileInfoAttributes.size();
    }

    static @NotNull ApplicationContext create(@Nullable ApplicationContext currentDeclaration,
                                              @NotNull ApplicationSettings settings,
                                              @NotNull File agentDiscoveryDirectory) throws Exception {
        var baseDirectory = settings.getBaseDirectory();
        var declarationFile = settings.getDeclarationFile();
        var storageDirectory = settings.getStorageDirectory();

        if (baseDirectory == null || !Files.exists(baseDirectory))
            throw new FileNotFoundException(String.format("Не найдена базовая директория: [%s]", Optional.ofNullable(baseDirectory).map(Path::toString).orElse(null)));
        if (declarationFile == null || !declarationFile.exists())
            throw new FileNotFoundException(String.format("Не найден файл декларации приложения: [%s]", declarationFile));
        if (storageDirectory == null)
            throw new IllegalArgumentException("Не задан storageDirectory приложения");

        var agentDirectoryInfo = tryDetermineAgentDirectory(agentDiscoveryDirectory).getRightOrThrow();

        var isConfigChanged = false;
        // =======================================================================================================================================
        // Проверяем, что нет предыдущей конфигурации
        if (currentDeclaration == null) {
            isConfigChanged = true;
        }

        // Изменен путь интеграции с агентом (директория для размещения ref файлов).
        if (!isConfigChanged && !StringHelper.equalsIgnoreCase(currentDeclaration.getAgentDirectoryFullName(), agentDirectoryInfo.getAbsolutePath())) {
            isConfigChanged = true;
        }

        // Изменились базовые параметры приложения.
        if (!isConfigChanged && !StringHelper.equalsIgnoreCase(currentDeclaration.getBaseDirectoryInfo().getAbsolutePath(), baseDirectory.toString())) {
            isConfigChanged = true;
        }
        if (!isConfigChanged && !StringHelper.equalsIgnoreCase(currentDeclaration.getDecFileInfo().getAbsolutePath(), declarationFile.getAbsolutePath())) {
            isConfigChanged = true;
        }

        // Не определен файл ссылки на файл описание приложения
        if (!isConfigChanged && currentDeclaration.refFileInfo == null) {
            isConfigChanged = true;
        }

        // Файл ссылки модифицировали. Его нужно пересоздать.
        if (!isConfigChanged) {
            var curRefFile = currentDeclaration.refFileInfo = new File(currentDeclaration.refFileInfo.getAbsolutePath());
            var curRefFileAttributes = Files.readAttributes(currentDeclaration.refFileInfo.toPath(), BasicFileAttributes.class);

            if (!curRefFile.exists() || curRefFileAttributes.creationTime().toMillis() != currentDeclaration.refFileCreate ||
                    curRefFileAttributes.lastModifiedTime().toMillis() != currentDeclaration.refFileWrite ||
                    curRefFile.length() != currentDeclaration.refFileLength) {
                isConfigChanged = true;
            }
        }

        // Файл описания приложения был удален. Необходимо повторно опубликовать его.
        if (!isConfigChanged) {
            currentDeclaration.appInfoFileInfo = new File(currentDeclaration.appInfoFileInfo.getAbsolutePath());
            if (!currentDeclaration.appInfoFileInfo.exists()) {
                isConfigChanged = true;
            }
        }

        // Файл декларации приложения был изменен, необходимо повторно опубликовать приложение
        if (!isConfigChanged) {
            currentDeclaration.decFileInfo = new File(currentDeclaration.decFileInfo.getAbsolutePath());
            var curDecFileAttributes = Files.readAttributes(currentDeclaration.decFileInfo.toPath(), BasicFileAttributes.class);
            if (curDecFileAttributes.creationTime().toMillis() != currentDeclaration.decFileCreate ||
                    curDecFileAttributes.lastModifiedTime().toMillis() != currentDeclaration.decFileWrite ||
                    curDecFileAttributes.size() != currentDeclaration.decFileLength) {
                isConfigChanged = true;
            }
        }

        if (!isConfigChanged) {
            return currentDeclaration;
        }

        var declarationDocumentLoadResult = loadDeclaration(declarationFile);
        var declarationDocumentLoadError = declarationDocumentLoadResult.tryGetLeft().orElse(null);
        var declarationDocument = declarationDocumentLoadResult.tryGetRight().orElse(null);
        if (declarationDocumentLoadError != null) {
            throw new ApplicationException(String.format("Ошибка загрузки файла декларации приложения: [%s]", declarationFile.getAbsolutePath()), declarationDocumentLoadError);
        }
        if (declarationDocument == null || declarationDocument.getDocumentElement() == null) {
            throw new ApplicationException(String.format("Ошибка загрузки файла декларации приложения: [%s]. Файл не содержит корневого элемента.", declarationFile.getAbsolutePath()));
        }

        var documentRoot = declarationDocument.getDocumentElement();
        documentRoot.setAttribute("base-path", baseDirectory.toAbsolutePath().toString());     // базовый путь для инвентаризации
        documentRoot.setAttribute("store-path", storageDirectory.toAbsolutePath().toString()); // базовый путь где размещается конфигурационная информация

        var appName = settings.getName();
        var appVersion = settings.getVersion();
        var applicationElement = XmlHelper.getElementByTagName(documentRoot, "application");

        if (applicationElement != null) {
            if (!StringHelper.isNullOrWhitespace(appName)) {
                applicationElement.setAttribute("name", appName);
            } else {
                appName = applicationElement.getAttribute("name");
            }

            if (!StringHelper.isNullOrWhitespace(appVersion)) {
                applicationElement.setAttribute("version", appVersion);
            } else {
                appVersion = applicationElement.getAttribute("version");
            }
        }

        // вычисляем хэш
        val rawDocXml = XmlHelper.toXmlString(declarationDocument);
        val rawDocXmlHash = HashHelper.getSHA256HashString(rawDocXml)
                .toLowerCase().substring(0, 10);

        // prepare appName
        appName = FileHelper.removeIncorrectPathChars(appName);
        appName = appName.length() > 256 ? appName.substring(0, 256) : appName;
        var maxDeclareFileNameLength = 256 - appName.length();

        var declareFileNameParts = new ArrayList<String>();
        var curDir1 = baseDirectory;
        while (curDir1 != null) {
            var parent = curDir1.getParent();
            if (parent == null)
                break;

            var dirName = curDir1.getFileName().toString();
            if (maxDeclareFileNameLength <= dirName.length()) {
                break;
            }

            declareFileNameParts.add(dirName);
            maxDeclareFileNameLength -= dirName.length();
            curDir1 = parent;
        }

        Collections.reverse(declareFileNameParts);

        declareFileNameParts.add(rawDocXmlHash);
        declareFileNameParts.add(appName);

        var storeFolderName = String.join("_", appName, rawDocXmlHash);

        var destinationDirectory = new File(FileHelper.combinePaths(storageDirectory.toAbsolutePath().toString(), storeFolderName).toString());
        if (!destinationDirectory.exists() && !destinationDirectory.mkdir()) {
            throw new ApplicationException(String.format("Невозможно создать директорию %s", destinationDirectory));
        }

        var appInfoFileName = FileHelper.combinePaths(destinationDirectory.getAbsolutePath(), rawDocXmlHash + ".app.xml");
        if (!Files.exists(appInfoFileName)) {
            XmlHelper.writeToFile(declarationDocument, appInfoFileName);
        }

        var appInfoFileInfo = new File(appInfoFileName.toString());

        var appInfoRefFileName = String.join("_", declareFileNameParts) + ".ref";
        var appInfoRefFilePath = FileHelper.combinePaths(agentDirectoryInfo.getPath(), appInfoRefFileName);

        Files.writeString(appInfoRefFilePath, appInfoFileInfo.getAbsolutePath(), StandardCharsets.UTF_8);

        var appInfoRefFileInfo = new File(appInfoRefFilePath.toString());

        return new ApplicationContext(agentDirectoryInfo, appName, appVersion, baseDirectory.toFile(), declarationFile, destinationDirectory, appInfoFileInfo, appInfoRefFileInfo);
    }


    private static Either<Exception, Document> loadDeclaration(File declarationFile) {
        try {
            var document = XmlHelper.tryLoadDocument(declarationFile.toPath());
            var documentRoot = document.toOptional().map(Document::getDocumentElement).orElse(null);

            if (documentRoot == null)
                throw new IllegalStateException(String.format("Не задан корневой элемент файла: [%s] декларации приложения", declarationFile.getAbsolutePath()));

            if (!StringHelper.equalsIgnoreCase(documentRoot.getNodeName(), "configuration"))
                throw new IllegalStateException(String.format("Корневой элемент файла: [%s] декларации отличен от <configuration>", declarationFile.getAbsolutePath()));

            return document;
        } catch (Exception exception) {
            return Either.left(exception);
        }
    }


    /**
     * Получить директорию агента, в котором лежит ref file
     * @param agentDiscoveryDirectory
     * @return
     */
    private static Either<Exception, File> tryDetermineAgentDirectory(@NotNull File agentDiscoveryDirectory) {
        try {
            for (var curDirectory = agentDiscoveryDirectory; curDirectory != null; curDirectory = curDirectory.getParentFile()) {
                var mountConfigAnchorPath = FileHelper.tryCombinePaths(curDirectory.toString(), ".mount-configuration")
                        .orElse(null);
                if (mountConfigAnchorPath == null) {
                    throw new ApplicationException("Problem with finding .mount-configuration path");
                }
                if (!Files.exists(mountConfigAnchorPath))
                    continue;

                var declarationStorage = FileHelper.readFile(mountConfigAnchorPath.toString());
                if (StringHelper.isNullOrWhitespace(declarationStorage)) {
                    throw new ApplicationException(String.format(".mount-configuration file content is empty, [%s]", mountConfigAnchorPath));
                }

                var storageFullPath = FileHelper.tryGetPath(declarationStorage)
                        .map(Path::toAbsolutePath)
                        .orElse(null);
                if (storageFullPath == null || !declarationStorage.equalsIgnoreCase(storageFullPath.toString())) {
                    throw new ApplicationException(String.format("Invalid .mount-configuration file content, [%s]", mountConfigAnchorPath));
                }

                if (!storageFullPath.isAbsolute()) {
                    throw new ApplicationException(String.format(".mount-configuration file content must contain absolute path, [%s]", mountConfigAnchorPath));
                }

                var storageFullPathFolder = new File(storageFullPath.toString());
                if (!storageFullPathFolder.exists()) {
                    if (!storageFullPathFolder.mkdir())
                        throw new ApplicationException(String.format("Невозможно создать папку с путем [%s] взятому из .mount-configuration [%s]", storageFullPathFolder, mountConfigAnchorPath));
                }

                return Either.right(storageFullPathFolder);
            }

            return Either.left(new ApplicationException(String.format("Couldn't find .mount-configuration file at [%s] or it parents", agentDiscoveryDirectory.getAbsolutePath())));
        } catch (Exception e) {
            return Either.left(e);
        }
    }
}
