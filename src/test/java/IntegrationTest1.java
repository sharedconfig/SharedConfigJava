import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sharedconfig.core.ApplicationSettings;
import sharedconfig.core.ConfigurationEngine;
import sharedconfig.helpers.FileHelper;
import sharedconfig.helpers.SharedConfigHelperTwo;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class IntegrationTest1 {
    private final String agentDiscoveryAbsolutePath = "C:\\Users\\Vasypu\\Desktop\\softconsalt\\AIS3CSM\\configurations\\";
    private final String agentRunnerDllAbsolutePath = "C:\\Users\\administrator.SCSM\\Desktop\\AIS3CSM\\packages\\configuration.agent-ae36676bb3bf6d4edb2a9c615dd32dbbcb94d8bb\\Configuration.Agent.Runner.dll";

    private final String classAbsolutePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().substring(1);
    private final String storageAbsolutePath = FileHelper.combinePaths(classAbsolutePath, "IntegrationTest1_data\\storage").toString();
    private final String baseAbsolutePath = FileHelper.combinePaths(classAbsolutePath, "IntegrationTest1_data\\application").toString();

    @BeforeEach
    public void init() {
        // очищаем директории после предыдущего запуска
        val dirsToClear = List.of(agentDiscoveryAbsolutePath, storageAbsolutePath);
        for (var dir : dirsToClear) {
            Helper.deleteDirectoryContent(dir);
        }

        // запускаем эмуляцию поведения агента
        var bgThread = new Thread(this::agentEmulator);
        bgThread.start();
    }

    private Process agentProcess;


    @AfterEach
    private void postTestProcessing() throws InterruptedException {
        //agentProcess.destroyForcibly();
    }


    @SneakyThrows
    private void agentEmulator() {
        // ждем 10 сек пока все прогрузится
        Thread.sleep(5000);

        // запускаем агент и добавлям подписку на его отключение
        // agentProcess = Runtime.getRuntime().exec("dotnet " + this.agentRunnerDllAbsolutePath);
        //Runtime.getRuntime().addShutdownHook(new Thread(() -> agentProcess.destroyForcibly()));
    }

    @SneakyThrows
    @Test
    void Test1() {

        //SharedConfigHelperTwo.ensureConfigurationFilesExtracted("up-configuration");
        //LoggerConfigurer.redirectLogsToUPDefaultLocation("TestApp", "1.0.0");

        var applicationSettings = ApplicationSettings.create(
                baseAbsolutePath,
                "test-app-declaration.xml",
                storageAbsolutePath,
                "TestApp",
                "1.0.0");

        var engine = ConfigurationEngine.create(applicationSettings, agentDiscoveryAbsolutePath);

        engine.waitAgent();
        engine.waitStore();
        var store = engine.getApplicationConfigurationService();

        var versionIds = store.getVersionIds();

        Assertions.assertEquals(1, versionIds.size());
        Assertions.assertEquals(-2, versionIds.first());

        var lastSnapshot = store.getLastVersion();

        Assertions.assertNotNull(lastSnapshot.getVariable("app variable"));

        //var varData = versions.tryGetVariable("app_level_variable").orElseThrow();
        //assertEquals(varData, "===[block_1.variable].VALUE===");

        //Thread.sleep(60000);
    }
}
