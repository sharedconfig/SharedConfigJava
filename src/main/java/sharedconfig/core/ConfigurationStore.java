package sharedconfig.core;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import sharedconfig.core.exceptions.ApplicationException;
import sharedconfig.core.model.appinv.declarations.*;
import sharedconfig.helpers.FileHelper;
import sharedconfig.helpers.HashHelper;
import sharedconfig.helpers.StringHelper;
import sharedconfig.helpers.XmlHelper;
import sharedconfig.utils.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Инкапсулирует логику работы с хранилищем changeset'ов
 */
@Log4j2
/* package */ class ConfigurationStore {
    @Getter
    private final @NotNull ApplicationContext applicationContext;

    private @NotNull File preparedXmlFile;

    @Getter
    private final @NotNull ApplicationInvDeclaration applicationInvDeclaration;

    /** элементы конфигурации в необработанном виде, ключ - путь */
    private final TreeMap<String, ConfigurationStoreItem> rawStoreItems = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    /** элементы конфигурации в обработанном виде */
    private final HashMap<Long, ConfigurationVersion> preparedVersions = new HashMap<>();
    /** список актульных changeset'ов
    /* package */ @NotNull SortedSet<Long> actualChangesetIds = new TreeSet<>();
    /** текущий максимальный id changeset'ов */
    /* package */ @Nullable Long maxChangesetId = null;


    /** дата изменения последнего загруженного prepared.xml */
    private Long preparedFileLastModified;
    /** хэш последнего загруженного prepared.xml */
    private Long preparedFileLastHash;
    /** текущая версия хранилища */
    @Getter private long version = 0;

    private static final Version defaultVersion = new Version(0, 0, 0);

    private ConfigurationStore(ApplicationContext applicationContext,
                               File preparedXmlFile,
                               ApplicationInvDeclaration applicationInvDeclaration) {
        this.applicationContext = Objects.requireNonNull(applicationContext);
        this.preparedXmlFile = Objects.requireNonNull(preparedXmlFile);
        this.applicationInvDeclaration = Objects.requireNonNull(applicationInvDeclaration);
    }

    /**
     * Обновляем состояние хранилища
     */
    /* package */ void refresh() {
        try {
            var isLoaded = this.loadPreparedXml();
            if (isLoaded)
                log.trace("Configuration store was refreshed successfully");
        } catch (Exception ex) {
            log.warn("Couldn't load prepared.xml", ex);
        }
    }


    /* package */ HashMap<Long, ConfigurationVersion> getVersions() {
        return this.preparedVersions;
    }

    /**
     * Пытаемся обновить prepared.dml
     * @return true если хранилище было обновлено, false если нет
     */
    private boolean loadPreparedXml() throws ApplicationException {
        try {
            log.trace("Trying to ensure prepared.xml status");

            preparedXmlFile = new File(preparedXmlFile.getAbsolutePath());

            // prepared.xml отсутствует?
            if (!preparedXmlFile.exists()) {
                log.trace("Couldn't find prepared.xml file");
                return false;
            }

            // prepared.xml не ьыл изменен
            var prepareTime = preparedXmlFile.lastModified();
            var prepareHash = HashHelper.getSimpleFileHash(preparedXmlFile);
            if (this.preparedFileLastModified != null && this.preparedFileLastHash != null &&
                prepareTime == this.preparedFileLastModified && prepareHash == this.preparedFileLastHash) {
                log.trace("No changes detected for prepared.xml");
                return false;
            }

            log.trace("Trying to load prepared.xml");
            var documentEither = XmlHelper.tryLoadDocument(preparedXmlFile.toPath());
            if (documentEither.isLeft()) {
                log.trace("Loading failed for prepared.xml", documentEither.getLeft());
                return false;
            }
            var document = documentEither.getRight();

            var root = document.getDocumentElement();
            if (root == null || !root.getNodeName().equalsIgnoreCase(Constants.Prepared.RootTagName)) {
                log.trace("Couldn't find root tag. Exprected: {}", Constants.Prepared.RootTagName);
                return false;
            }

            var formatAttributeValue = XmlHelper.tryGetAttributeValue(root, "format").orElse(null);
            var format = Version.tryParseVersion(formatAttributeValue).orElse(defaultVersion);
            // now we supports onlt format 1.0.0
            if (format.getMajor() != 1 && format.getMinor() > 0 && format.getMicro() > 0) {
                log.trace("Couldn't parse format attribute. Only format=\"1.0.0\" is supported");
                return false;
            }

            var zitValue = XmlHelper.tryGetAttributeValue(root, a -> StringHelper.equalsIgnoreCase(a.getNodeName(), "zit")).orElse(null);
            var zvValue = XmlHelper.tryGetAttributeValue(root, a -> StringHelper.equalsIgnoreCase(a.getNodeName(), "zv")).orElse(null);
            long zit = StringHelper.tryToLong(zitValue).orElse(0L);
            long zv = StringHelper.tryToLong(zvValue).orElse(0L);
            log.trace("Parsed Zit: {}, Zv: {}", zit, zv);

            var changes = XmlHelper.getChildNodes(root);
            log.trace("Found {} changesets", changes.size());

            var currentChangesetToPathMapping = new TreeMap<Long, String>();           // текущий маппинг changesetId -> changesetPath
            var currentPathToChangesetsMapping = new HashMap<String, TreeSet<Long>>(); // текущий маппинг changesetPath -> changesetIds
            var changesetsToRemove = new HashSet<Long>();                    // ченджсеты, помеченные на удаление
            var changesetsToUpdate = new HashSet<Long>();                    // ченджсеты, которые нужно обновить

            log.trace("Trying to load changesets from xml");
            for(var changesetNode : changes) {
                if (!StringHelper.equalsIgnoreCase(changesetNode.getNodeName(), Constants.Prepared.ChangesetTagName)) {
                    log.trace("Skipping changeset with tagname '{}', expected tagname '{}'", changesetNode.getNodeName(), Constants.Prepared.ChangesetTagName);
                    continue;
                }

                var id = XmlHelper.tryGetAttributeValue(changesetNode, a -> StringHelper.equalsIgnoreCase(a.getNodeName(), "id"))
                        .flatMap(StringHelper::tryToLong).orElse(null);
                var status = XmlHelper.tryGetAttributeValue(changesetNode, a -> StringHelper.equalsIgnoreCase(a.getNodeName(), "status")).orElse(null);
                var source = XmlHelper.tryGetAttributeValue(changesetNode, a -> StringHelper.equalsIgnoreCase(a.getNodeName(), "source")).orElse(null);
                var directoryValue = XmlHelper.tryGetAttributeValue(changesetNode, a -> StringHelper.equalsIgnoreCase(a.getNodeName(), "directory"))
                        .flatMap(FileHelper::tryGetPath).orElse(null);
                var templateDirectoryValue = XmlHelper.tryGetAttributeValue(changesetNode, a -> StringHelper.equalsIgnoreCase(a.getNodeName(), "templateDirectory"))
                        .flatMap(FileHelper::tryGetPath).orElse(null);

                log.trace("Processing changeset with attributes id='{}', status='{}', source='{}', directoryValue='{}', templateDirectoryValue='{}'",
                        id, status, source, directoryValue, templateDirectoryValue);

                if (id == null || status == null || source == null || directoryValue == null || templateDirectoryValue == null) {
                    log.trace("All attributes must have a value! Skipping..");
                    continue;
                }

                if (id < -2L) {
                    log.trace("id must be >= -2, current id: {}. Skipping..", id);
                    continue;
                }

                var preparedVarsFileInfo = Optional.of(directoryValue)
                        .flatMap(v -> !v.isAbsolute() ? FileHelper.tryCombinePaths(this.preparedXmlFile.getParent(), v.toString()) : Optional.of(v))
                        .flatMap(v -> FileHelper.tryCombinePaths(v.toString(), Constants.PreparedVars.FileName))
                        .map(Path::toFile)
                        .orElse(null);
                if (preparedVarsFileInfo == null || !preparedVarsFileInfo.exists()) {
                    log.trace("Couldn't build prepared.vars.xml path or file doesn't exist");
                    continue;
                }

                var templateDirectoryInfo = Optional.of(templateDirectoryValue)
                        .flatMap(v -> !v.isAbsolute() ? FileHelper.tryCombinePaths(this.preparedXmlFile.getParent(), v.toString()) : Optional.of(v))
                        .map(Path::toFile)
                        .orElse(null);
                if (templateDirectoryInfo == null) {
                    log.trace("Couldn't build templateDirectoryInfo path, templateDirectoryValue={}", templateDirectoryValue);
                    continue;
                }


                // Регистрируем источник в коллекции известных источников
                var storeItemGroupId = preparedVarsFileInfo.getParent();
                this.rawStoreItems.computeIfAbsent(storeItemGroupId,
                        (k) -> new ConfigurationStoreItem(k, this, preparedVarsFileInfo, templateDirectoryInfo));
                currentChangesetToPathMapping.put(id, storeItemGroupId);
                currentPathToChangesetsMapping.computeIfAbsent(storeItemGroupId, (__) -> new TreeSet<>()).add(id);

                // проверяем стаатус
                if (StringHelper.equalsIgnoreCase(status, "Error")) {
                    log.trace("Changeset with error status. Skipping..");
                    continue;
                }
                if (StringHelper.equalsIgnoreCase(status, "Removed")) {
                    log.trace("Changeset with Removed status. Skipping..");
                    changesetsToRemove.add(id);
                    continue;
                }
                if (!StringHelper.equalsIgnoreCase(status, "Success")) {
                    log.trace("Undefined changeset type {}. Skipping..", status);
                    continue;
                }

                changesetsToUpdate.add(id);
                log.trace("Chageset '{}' with path '{}' was successfully queued to update", id, storeItemGroupId);
            }
            log.trace("Finish loading changesets from xml");
            log.trace("Found {} changesets to update, {} chagesets to remove", changesetsToUpdate.size(), changesetsToRemove.size());

            // обновляем все ченджсеты со статусом "Success"
            log.trace("Trying to update changesets");
            var updatedChangesets = new HashSet<Long>(changesetsToUpdate.size());
            var processedStoreItems = new HashSet<String>(changesetsToUpdate.size());
            var failedChangesets = new HashMap<Long, Exception>();
            for (var changesetToUpdate : changesetsToUpdate) {
                log.trace("Trying to load/update changeset '{}'", changesetToUpdate);
                var storeItemId = currentChangesetToPathMapping.get(changesetToUpdate); assert storeItemId != null;
                if (processedStoreItems.contains(storeItemId)) {
                    log.trace("Changeset's target store has been already loaded");
                    continue;
                }
                var storeItem = rawStoreItems.get(storeItemId);
                var storeItemUpdateResult = storeItem.loadOrUpdate();
                if (storeItemUpdateResult.isLeft()) {
                    log.warn("Updating failed. Error - {}", changesetToUpdate, storeItemUpdateResult.getLeft());
                    failedChangesets.put(changesetToUpdate, storeItemUpdateResult.getLeft());
                } else {
                    log.trace("Updating was successfull");
                    updatedChangesets.add(changesetToUpdate);
                }
                processedStoreItems.add(storeItemId);
            }
            log.trace("Finish updating configurations");
            log.trace("Successfully updated {}/{} changesets", updatedChangesets.size(), changesetsToUpdate.size());

            // помечаем удалененные ченджсеты как deprecated
            log.trace("Trying to deprecate removed changesets from active configuration");
            for (var changesetToRemove : changesetsToRemove) {
                var existingConfiguration = preparedVersions.get(changesetToRemove);
                if (existingConfiguration != null) {
                    log.trace("Existing changeset {} made deprecated", changesetToRemove);
                    existingConfiguration.setDeprecated(true);
                }
            }
            log.trace("Finish deprecating removed changesets");

            // добавляем успешно загруженные ченджсеты в хранилище
            log.trace("Trying to add new changeset to active configuration");
            for (var updatedChangeset : updatedChangesets) {
                var storeItemId = currentChangesetToPathMapping.get(updatedChangeset); assert storeItemId != null;
                preparedVersions.put(updatedChangeset, new ConfigurationVersion(rawStoreItems.get(storeItemId), updatedChangeset));
                log.trace("Added changeset {}", updatedChangeset);
            }

            this.actualChangesetIds = preparedVersions.values()
                    .stream().filter(x -> !x.isDeprecated())
                    .map(ConfigurationVersion::getVersion)
                    .collect(Collectors.toCollection(TreeSet::new));
            this.maxChangesetId = this.actualChangesetIds.size() > 0 ? this.actualChangesetIds.last() : null;
            this.preparedFileLastHash = prepareHash;
            this.preparedFileLastModified = prepareTime;
            ++this.version;

            return true;
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }

    public static ConfigurationStoreBuilder builder() {
        return new ConfigurationStoreBuilder();
    }

    /**
     * Загружаем файл результат инвентаризации и создаем контроллер хранилища подготовленных конфигурационных элементов
     */
    @Log4j2
    public static class ConfigurationStoreBuilder {

        public Either<Exception, ConfigurationStore> tryBuild(@NotNull ApplicationContext applicationContext) {
            try {
                log.trace("Trying to build ConfigurationStore");
                var buildResult = buildInternal(applicationContext);
                log.trace("Finish building ConfigurationStore");
                return Either.right(buildResult);
            } catch (Exception e) {
                log.trace("Failed to build ConfigurationStore - {}", e.getMessage());
                return Either.left(e);
            }
        }

        private ConfigurationStore buildInternal(@NotNull ApplicationContext applicationContext) throws Exception {
            val packDir = applicationContext.getStorageDirectoryInfo();
            if (packDir == null || !packDir.exists()) {
                throw new NotDirectoryException(String.format("Директория: [%s] для взаимодействия с агентом не существует", Optional.ofNullable(packDir).map(File::getAbsolutePath)));
            }

            val applicationsInvPath = FileHelper
                    .tryCombinePaths(packDir.getAbsolutePath(), Constants.ApplicationInv.FileName)
                    .orElse(null);
            if (applicationsInvPath == null || !Files.exists(applicationsInvPath)) {
                throw new FileNotFoundException(String.format("Файл инвентаризации не найден [%s]", applicationsInvPath));
            }

            log.trace("Found inventory file with path '{}'", applicationsInvPath);

            val doc = XmlHelper.tryLoadDocument(applicationsInvPath).getRightOrThrow();

            val root = doc.getDocumentElement();
            if (root == null) {
                throw new ApplicationException(String.format("Файла инвентаризации: [%s] не содержит корневого элемента", applicationsInvPath));
            }

            log.trace("Trying to load application element");
            var application = loadApplication(root);
            log.trace("Application element loaded successfully");

            log.trace("Trying to load alerts");
            loadAlerts(root, application.getAlerts());
            log.trace("Loaded {} alerts", application.getAlerts().size());

            var totalVariables = extractAllVariables(application);
            var preparedXmlFile = new File(FileHelper.combinePaths(packDir.getAbsolutePath(), "prepared.xml").toString());

            return new ConfigurationStore(applicationContext, preparedXmlFile, new ApplicationInvDeclaration(application, application.getAlerts(), application.getBlocks(), totalVariables));
        }

        private static void loadAlerts(@NotNull Element parentElement, @NotNull List<AlertDeclaration> accumulator) {
            var alertElements = Optional.ofNullable(XmlHelper.getElementByTagName(parentElement, "alerts"))
                    .map(as -> XmlHelper.getElementsByTagName(as, "alert"))
                    .orElse(null);
            if (alertElements == null)
                return;

            for (val alertElement : alertElements) {
                val alertAttributes = loadAttributes(alertElement);
                val type = alertAttributes.tryGet("type").orElse(null);
                val message = alertAttributes.tryGet("message").orElse(null);

                if (type == null || message == null) {
                    log.trace("Invalid attribute format. Skipping");
                    continue;
                }

                accumulator.add(new AlertDeclaration(type, message));
            }
        }

        private static ApplicationDeclaration loadApplication(Element parentElement) throws ApplicationException {
            if (parentElement == null)
                throw new IllegalArgumentException("parentElement tag is null");

            var applicationElement = XmlHelper.getElementByTagName(parentElement, Constants.ApplicationInv.ApplicationElement.Name);
            if (applicationElement == null)
                throw new ApplicationException("Couldn't find application element");

            var appAttributes = loadAttributes(applicationElement);
            var appName = appAttributes.tryGet("name").orElse(null);
            var appVersion = appAttributes.tryGet("version").orElse(null);
            if (appName == null || appVersion == null) {
                throw new ApplicationException("Файла инвентаризации не содержит обязательных атрибутов name и version");
            }
            var application = new ApplicationDeclaration(appName, appVersion);

            log.trace("Trying to load blocks");
            loadBlocks(applicationElement, application.getBlocks());
            log.trace("Blocks loaded succesfully");

            log.trace("Trying to load application variables");
            loadVariablesElement(applicationElement, application.getVariables());
            log.trace("Application variables loaded successfully");

            log.trace("Trying to load application templates");
            loadTemplates(applicationElement, application.getTemplates());
            log.trace("Application templates loaded successfully");

            return application;
        }

        private static NameHashMap<VariableDeclaration> extractAllVariables(ApplicationDeclaration application) {
            var totalVariables = new NameHashMap<VariableDeclaration>();
            for (var appVar : application.getVariables().values()) {
                totalVariables.put(appVar.getId(), appVar);
            }
            for (var block : application.getBlocks().values()) {
                for (var blockVar : block.getVariables().values()) {
                    totalVariables.put(blockVar.getId(), blockVar);
                }
            }
            return totalVariables;
        }


        private static void loadBlocks(@NotNull Element parentElement, @NotNull NameVersionMap<BlockDeclaration> accumulator) throws ApplicationException {
            var blockElements = XmlHelper.getElementsByTagName(parentElement, "block");

            for (val blockElement : blockElements) {

                val blockAttributes = loadAttributes(blockElement);
                var blockName = blockAttributes.tryGet("name").orElse(null);
                var blockVersion = blockAttributes.tryGet("version").orElse(null);

                log.trace("Trying to load block '{}:{}'", blockName, blockVersion);
                if (blockName == null || blockVersion == null) {
                    throw new ApplicationException("Определение блока не содержит обязательных атрибутов name и version");
                }
                var block = new BlockDeclaration(blockName, blockVersion);
                accumulator.put(block.getId(), block);
                log.trace("Block attributes loaded successfully");

                log.trace("Trying to load block '{}:{}' variables", blockName, blockVersion);
                loadVariablesElement(blockElement, block.getVariables());
                log.trace("Block variables loaded successfully");

                log.trace("Trying to load block '{}:{}' templates", blockName, blockVersion);
                loadTemplates(blockElement, block.getTemplates());
                log.trace("Block templates loaded successfully");

                log.trace("Block '{}:{}' loaded successfully", blockName, blockVersion);
            }
        }


        private static void loadTemplates(@NotNull Element parentElement, @NotNull HashMap<String, TemplateDeclaration> accumulator) {
            var templateElements = Optional.ofNullable(XmlHelper.getElementByTagName(parentElement, "templates"))
                    .map(ts -> XmlHelper.getElementsByTagName(ts, "template"))
                    .orElse(null);
            if (templateElements == null)
                return;

            for (var templateElement : templateElements) {
                val templateAttributes = loadAttributes(templateElement);

                val id = templateAttributes.tryGet("id").orElse(null);
                val name = templateAttributes.tryGet("name").orElse(null);
                val path = templateAttributes.tryGet("path").orElse(null);
                val hash = templateAttributes.tryGet("hk").orElse(null);

                if (id == null || name == null || path == null || hash == null) {
                    log.trace("Incorrect template attributes, skipping..");
                    continue;
                }

                log.trace("Found template '{}:{}'", name, hash);
                accumulator.put(name, new TemplateDeclaration(id, name, path, hash));
            }
        }

        private static void loadVariablesElement(@NotNull Element parentElement, @NotNull HashMap<String, VariableDeclaration> accumulator) throws ApplicationException {
            var variableElements = Optional.ofNullable(XmlHelper.getElementByTagName(parentElement, "variables"))
                    .map(vars -> XmlHelper.getElementsByTagName(vars, Constants.ApplicationInv.VariablesElement.VarElement.Name))
                    .orElse(null);
            if (variableElements == null)
                return;

            for (val element : variableElements) {
                val varAttributes = loadAttributes(element);

                var varName = varAttributes.tryGet("name").orElse(null);
                var varHash = varAttributes.tryGet("hk").orElse(null);
                var varDescription = varAttributes.tryGet("i:description").orElse(null);
                var varDefaultValue = element.getTextContent();

                if (varName == null || varHash == null) {
                    throw new ApplicationException("В определении переменной должны быть обязательные атрибуты name hk");
                }

                log.trace("Found variable '{}:{}'", varName, varHash);
                var variable = new VariableDeclaration(varName, varHash, varDescription, varDefaultValue);
                accumulator.put(varName, variable);
            }
        }


        private static @NotNull NodeItemAttributes loadAttributes(@Nullable Element node) {
            val result = new NodeItemAttributes();
            if (node == null) {
                return result;
            }

            val attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); ++i) {
                val att = attributes.item(i);
                val attNamespace = att.getNamespaceURI();
                val attName = att.getNodeName();
                val attValue = att.getNodeValue();
                result.put(attNamespace, attName, attValue);
            }
            return result;
        }
    }
}
