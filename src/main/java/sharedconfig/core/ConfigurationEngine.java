package sharedconfig.core;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sharedconfig.core.exceptions.ConfigurationEngineCreationException;
import sharedconfig.core.interfaces.IConfigurationVersionSnapshot;
import sharedconfig.core.interfaces.IScopedConfigurationService;
import sharedconfig.utils.tuples.NameVersionTuple;
import sharedconfig.utils.tuples.StrictNameVersionTuple;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;

@Log4j2
public class ConfigurationEngine implements AutoCloseable {
    @Getter
    private final @NotNull ApplicationSettings applicationSettings;
    private final @NotNull File agentDiscoveryDirectory;
    @Getter
    /*package*/ @Nullable ConfigurationStore store;
    private final Object waitStoreSyncObject = new Object();
    private static final BackgroundWorker worker = new BackgroundWorker();

    private ConfigurationEngine(@NotNull ApplicationSettings applicationSettings, @NotNull File agentDiscoveryDirectory) {
        this.applicationSettings = applicationSettings;
        this.agentDiscoveryDirectory = agentDiscoveryDirectory;
    }

    public static ConfigurationEngine create(@NotNull ApplicationSettings applicationSettings, @Nullable String agentDiscoveryPath) throws ConfigurationEngineCreationException {
        var discoveryDirectory = applicationSettings.resolveDirectory(agentDiscoveryPath)
                .orElseThrow(() -> new ConfigurationEngineCreationException("Couldn't resolve agent discovery path"));
        return worker.put(applicationSettings, discoveryDirectory.toFile());
    }

    /**
     * Метод проверки изменений в конфигурации
     */
    @SneakyThrows
    private void tryUpdate() {

        var appCtx = this.store == null ? null : this.store.getApplicationContext();
        var applicationContext = ApplicationContext.create(appCtx, this.applicationSettings, this.agentDiscoveryDirectory);

        if (this.store == null || this.store.getApplicationContext() != applicationContext) {
            // Создаем контроллер хранилища подготовленной конфигурационной информации
            this.store = ConfigurationStore.builder().tryBuild(applicationContext).getRightOrThrow();

            synchronized (this.waitStoreSyncObject) {
                this.waitStoreSyncObject.notify();
            }
        }

        this.store.refresh();
    }

    private Optional<ConfigurationStore> tryGetStore(int waitMilliseconds) {
        var value = this.store;
        if (value != null)
            return Optional.of(value);

        long startTime = System.currentTimeMillis();
        long stopTime = startTime;

        while ((value = this.store) == null && (stopTime - startTime) < waitMilliseconds) {
            synchronized (this.waitStoreSyncObject) {
                try {
                    this.waitStoreSyncObject.wait(30);
                } catch (InterruptedException ignored) {
                }
            }
            stopTime = System.currentTimeMillis();
        }

        return Optional.ofNullable(value);
    }

    public boolean waitAgent() {
        return waitAgent(Integer.MAX_VALUE);
    }

    public boolean waitAgent(int waitMilliseconds) {
        ConfigurationStore tmpStore = null;
        tmpStore = this.tryGetStore(waitMilliseconds).orElse(null);
        return this.store == tmpStore && tmpStore != null && tmpStore.getApplicationContext() != null;
    }

    public boolean waitStore() {
        return waitStore(Integer.MAX_VALUE);
    }

    public boolean waitStore(int waitMilliseconds) {
        long startTime = System.currentTimeMillis();
        long stopTime = startTime;

        while ((stopTime - startTime) < waitMilliseconds) {
            if (this.store != null && this.store.getVersion() > 0)
                return true;

            synchronized (this.waitStoreSyncObject) {
                try {
                    this.waitStoreSyncObject.wait(30);
                } catch (InterruptedException ignored) {
                }
            }
            stopTime = System.currentTimeMillis();
        }
        return this.store != null && this.store.getVersion() > 0;
    }


    public @NotNull IScopedConfigurationService<IConfigurationVersionSnapshot> getApplicationConfigurationService() {
        return new ScopedServiceCashingDecorator<>(this,
                new ApplicationScopedConfigurationService<>(this, new StrictNameVersionTuple(this.applicationSettings.getName(), this.applicationSettings.getVersion()), a -> a));
    }

    public <T> @NotNull IScopedConfigurationService<T> getApplicationConfigurationService(@NotNull Function<IConfigurationVersionSnapshot, T> builder) {
        return new ScopedServiceCashingDecorator<>(this,
                new ApplicationScopedConfigurationService<>(this, new StrictNameVersionTuple(this.applicationSettings.getName(), this.applicationSettings.getVersion()), builder));
    }

    public @NotNull IScopedConfigurationService<IConfigurationVersionSnapshot> getBlockConfigurationService(@NotNull String blockName) {
        return new ScopedServiceCashingDecorator<>(this,
                new BlockScopedConfigurationService<>(this, blockName, null, a -> a));
    }

    public <T> @NotNull IScopedConfigurationService<T> getBlockConfigurationService(@NotNull String blockName, @NotNull Function<IConfigurationVersionSnapshot, T> builder) {
        return new ScopedServiceCashingDecorator<>(this,
                new BlockScopedConfigurationService<>(this, blockName, null, builder));
    }

    public @NotNull IScopedConfigurationService<IConfigurationVersionSnapshot> getBlockConfigurationService(@NotNull String blockName, @NotNull String blockVersion) {
        return new ScopedServiceCashingDecorator<>(this,
                new BlockScopedConfigurationService<>(this, blockName, blockVersion, a -> a));
    }

    public <T> @NotNull IScopedConfigurationService<T> getBlockConfigurationService(@NotNull String blockName, @NotNull String blockVersion, @NotNull Function<IConfigurationVersionSnapshot, T> builder) {
        return new ScopedServiceCashingDecorator<>(this,
                new BlockScopedConfigurationService<>(this, blockName, blockVersion, builder));
    }

    @Override
    public void close() {
        worker.remove(this.applicationSettings);
    }

    /**
     * Класс, инкапсулирующий логику фонового хранения и наблюдения за конфигурациями
     */
    @Log4j2
    private static class BackgroundWorker implements AutoCloseable {
        private final @NotNull ConcurrentMap<NameVersionTuple, ConfigurationEngine> engines = new ConcurrentHashMap<>();
        private final @NotNull ScheduledExecutorService backgroundWorker;

        public BackgroundWorker() {
            this.backgroundWorker = Executors.newSingleThreadScheduledExecutor();
            this.backgroundWorker.scheduleAtFixedRate(() -> {
                for (var keyValue : engines.entrySet()) {
                    var key = keyValue.getKey();
                    var engine = keyValue.getValue();
                    if (key == null || engine == null)
                        continue;

                    try {
                        engine.tryUpdate();
                    } catch (Exception e) {
                        log.warn("Error on updating configuration store for app '{}' version '{}'. {}", key.getName(), key.getVersion(), e);
                    }
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }

        public ConfigurationEngine put(@NotNull ApplicationSettings applicationSettings, @NotNull File agentDiscoveryPath) throws ConfigurationEngineCreationException {
            var key = new NameVersionTuple(applicationSettings.getName(), applicationSettings.getVersion());
            var instance = engines.computeIfAbsent(key, (__) -> new ConfigurationEngine(applicationSettings, agentDiscoveryPath));

            // проверка на то, что instance создался новый (у старого будет другой applicationSettings)
            if (instance.applicationSettings != applicationSettings)
                throw new ConfigurationEngineCreationException(String.format("Приложение с именем: [%s] и версией [%s] уже ранее зарегистрировано", key.getName(), key.getVersion()));

            return instance;
        }

        public boolean remove(@Nullable ApplicationSettings applicationSettings) {
            if (applicationSettings == null)
                return false;

            var key = new NameVersionTuple(applicationSettings.getName(), applicationSettings.getVersion());
            return engines.remove(key) != null;
        }

        public void shutdown() {
            this.backgroundWorker.shutdown();
            this.engines.clear();
        }

        @Override
        public void close() {
            this.shutdown();
        }
    }
}
