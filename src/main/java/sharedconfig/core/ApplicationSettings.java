package sharedconfig.core;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.core.exceptions.ApplicationSettingsCreationException;
import sharedconfig.helpers.FileHelper;
import sharedconfig.helpers.StringHelper;
import sharedconfig.utils.Either;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ApplicationSettings {
    @Getter @NotNull private final String basePath;
    @Getter @NotNull private final String declarationPath;
    @Getter @NotNull private final String name;
    @Getter @NotNull private final String version;
    @Getter @NotNull private final Path baseDirectory;
    @Getter @NotNull private final File declarationFile;
    @Getter @NotNull private final Path storageDirectory;

    private ApplicationSettings(@NotNull String basePath,
                                @NotNull String declarationPath,
                                @NotNull String name,
                                @NotNull String version,
                                @NotNull Path baseDirectory,
                                @NotNull File declarationFile,
                                @NotNull Path storageDirectory) {
        this.basePath         = basePath;
        this.declarationPath  = declarationPath;
        this.name             = name;
        this.version          = version;
        this.baseDirectory    = baseDirectory;
        this.declarationFile  = declarationFile;
        this.storageDirectory = storageDirectory;
    }

    /**
     * Создать объект настроек приложения
     * @param basePath Базовый абсолютный путь приложения
     * @param declarationPath Относительный или абсолютный путь на шаблон приложения (xml файл аналог Application.xml)
     * @param storagePath Относительный или абсолютный путь на директорию в которой будет сохраняться подготовленная конфигурация
     * @param name Имя приложения
     * @param version Версия приложения
     * @return ApplicationSettings или Exception
     */
    public static @NotNull ApplicationSettings create(@NotNull String basePath,
                                                      @NotNull String declarationPath,
                                                      @NotNull String storagePath,
                                                      @NotNull String name,
                                                      @NotNull String version)
        throws ApplicationSettingsCreationException {
        try
        {
            if (StringHelper.isNullOrEmpty(basePath))
                throw new IllegalArgumentException("Value cannot be null or empty. basePath");
            if (StringHelper.isNullOrEmpty(declarationPath))
                throw new IllegalArgumentException("Value cannot be null or empty. declarationPath");
            if (StringHelper.isNullOrEmpty(storagePath))
                throw new IllegalArgumentException("Value cannot be null or empty. storagePath");
            if (StringHelper.isNullOrEmpty(name))
                throw new IllegalArgumentException("Value cannot be null or empty. name");

            var basePathProcessed = FileHelper.tryGetPath(basePath)
                    .orElseThrow(() -> new ApplicationSettingsCreationException("Имя папки пакета содержит недопустимые символы. basePath"));
            var declarationPathProcessed = FileHelper.tryGetPath(declarationPath)
                    .orElseThrow(() -> new ApplicationSettingsCreationException("Имя папки пакета содержит недопустимые символы. declarationPath"));
            var storagePathProcessed = FileHelper.tryGetPath(storagePath)
                    .orElseThrow(() -> new ApplicationSettingsCreationException("Имя папки пакета содержит недопустимые символы. storagePath"));

            if (!basePathProcessed.isAbsolute())
                throw new ApplicationSettingsCreationException("Имя папки пакета не может быть относительным путем. basePath");
            if (Files.notExists(basePathProcessed))
                throw new ApplicationSettingsCreationException(String.format("Не найдена базовая директория директория: [%s]", basePathProcessed.toAbsolutePath()));
            if (!storagePathProcessed.isAbsolute())
                storagePathProcessed = FileHelper.combinePaths(basePathProcessed.getParent().toString(), storagePathProcessed.toString()).toAbsolutePath();
            if (!Files.exists(storagePathProcessed) && !storagePathProcessed.toFile().mkdir())
                throw new ApplicationSettingsCreationException(String.format("Не найдена директория [%s] или невозможно ее создать", storagePathProcessed.toAbsolutePath()));

            if (!declarationPathProcessed.isAbsolute())
                declarationPathProcessed = FileHelper.combinePaths(basePathProcessed.toString(), declarationPathProcessed.toString()).toAbsolutePath();
            if (Files.notExists(declarationPathProcessed))
                throw new ApplicationSettingsCreationException(String.format("Не найден файл декларации приложения: [%s]", declarationPathProcessed.toAbsolutePath()));

            return new ApplicationSettings(basePath, declarationPath, name, version, basePathProcessed, declarationPathProcessed.toFile(), storagePathProcessed);
        } catch (ApplicationSettingsCreationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApplicationSettingsCreationException(ex);
        }
    }

    Optional<Path> resolveDirectory(@Nullable String path) {
        if (StringHelper.isNullOrWhitespace(path)) {
            return Optional.of(this.baseDirectory);
        }

        try {
            var typedPath = Path.of(path);
            return Optional.of(typedPath.isAbsolute() ? typedPath : FileHelper.combinePaths(this.baseDirectory.toAbsolutePath().toString(), path));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
