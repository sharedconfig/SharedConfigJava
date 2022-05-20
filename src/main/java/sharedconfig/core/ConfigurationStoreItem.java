package sharedconfig.core;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import sharedconfig.core.exceptions.ApplicationException;
import sharedconfig.core.model.appinv.declarations.VariableDeclaration;
import sharedconfig.core.model.appinv.definitions.FileValue;
import sharedconfig.core.model.appinv.definitions.VariableValue;
import sharedconfig.helpers.HashHelper;
import sharedconfig.helpers.StringHelper;
import sharedconfig.helpers.XmlHelper;
import sharedconfig.utils.Either;
import sharedconfig.utils.tuples.NameHashTuple;
import sharedconfig.utils.Version;
import sharedconfig.utils.tuples.StrictNameVersionTuple;

import java.io.File;
import java.util.*;

/**
 * Элемент конфигурационные данных, содержит переменные и шаблоны
 */
@Log4j2
/* package */ class ConfigurationStoreItem {
    private final @Getter @NotNull String id;
    private final @NotNull ConfigurationStore store;
    private final @NotNull File preparedVarsFileInfo;
    private final @NotNull File templateDirectoryInfo;
    private Long preparedVarsFileInfoHash;
    @Getter private HashMap<NameHashTuple, VariableValue> variables;
    @Getter private TreeMap<String, FileValue> templates;

    public ConfigurationStoreItem(@NotNull String id, @NotNull ConfigurationStore store, @NotNull File preparedVarsFileInfo, @NotNull File templateDirectoryInfo) {
        this.store = store;
        this.preparedVarsFileInfo = preparedVarsFileInfo;
        this.templateDirectoryInfo = templateDirectoryInfo;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationStoreItem that = (ConfigurationStoreItem) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Выполнить обновление данного хранилища
     */
    public Either<ApplicationException, Boolean> loadOrUpdate() {
        var prevVariables = this.variables;
        var prevTemplates = this.templates;
        var prevHash = this.preparedVarsFileInfoHash;

        // если файл не изменился с момента последней загрузки то ничего не делаем
        val fileHash = HashHelper.getSimpleFileHash(this.preparedVarsFileInfo);
        if (prevHash != null && fileHash == prevHash) {
            return Either.right(false);
        }
        this.preparedVarsFileInfoHash = fileHash;

        try {
            val document = XmlHelper.tryLoadDocument(this.preparedVarsFileInfo.toPath()).getRightOrThrow();
            val root = document.getDocumentElement();

            if (root == null || !root.getNodeName().equalsIgnoreCase(Constants.PreparedVars.VariablesElement.TagName)) {
                throw new Exception(String.format("Incorrect root tag, expected='%s'", Constants.PreparedVars.VariablesElement.TagName));
            }

            val formatVersion = XmlHelper.tryGetAttributeValue(root, "format").orElse("1.0");
            val version = Version.tryParseVersion(formatVersion).orElse(null);
            if (version == null) {
                throw new Exception("Некорректное значение атрибута 'format'");
            }

            if (version.getMajor() != 1 && version.getMinor() != 0) {
                throw new Exception(String.format("Версия формата файла: [%s] не поддерживается.", formatVersion));
            }

            val variables = new HashMap<NameHashTuple, VariableValue>();
            var variablesNodeChilds = Optional.ofNullable(root)
                    .map(XmlHelper::getChildNodes).orElseGet(ArrayList::new);
            for(val variableNode : variablesNodeChilds) {
                if (!variableNode.getNodeName().equalsIgnoreCase(Constants.PreparedVars.VariablesElement.VarElement.TagName))
                    continue;

                var varName  = XmlHelper.tryGetAttributeValueIgnoreCase(variableNode, "name").orElse(null);
                var hash  = XmlHelper.tryGetAttributeValueIgnoreCase(variableNode, "hk").orElse(null);
                var appName  = XmlHelper.tryGetAttributeValueIgnoreCase(variableNode, "a:n").orElse(null);
                var appVersion  = XmlHelper.tryGetAttributeValueIgnoreCase(variableNode, "a:v").orElse(null);
                var blockName  = XmlHelper.tryGetAttributeValueIgnoreCase(variableNode, "b:n").orElse(null);
                var blockVersion  = XmlHelper.tryGetAttributeValueIgnoreCase(variableNode, "b:v").orElse(null);
                var value = variableNode.getTextContent();

                if (varName == null) {
                    log.trace("Invalid 'name' attribute");
                    continue;
                }

                log.trace("Trying to load variable '{}:{}'",  varName, hash);
                Set<VariableDeclaration> declarations;
                if (StringHelper.isNullOrEmpty(appName) && StringHelper.isNullOrEmpty(blockName)) {
                    log.trace("Variable have global scope");
                    declarations = new HashSet<>(store.getApplicationInvDeclaration().getVariables().getByName(varName));
                } else {
                    declarations = new HashSet<>();
                    if (!StringHelper.isNullOrEmpty(appName)) {
                        var decl = this.store.getApplicationInvDeclaration().getApplication().getVariables().get(varName);
                        if (decl == null) {
                            log.trace("Couldn't find application var declaration for app {}:{}", appName, appVersion);
                        } else {
                            log.trace("Found declaration inside scope {}:{}", appName, appVersion);
                            declarations.add(decl);
                        }
                    }
                    if (!StringHelper.isNullOrEmpty(blockName)) {
                        var targetBlocks = !StringHelper.isNullOrEmpty(blockVersion)
                                ? List.of(this.store.getApplicationInvDeclaration().getBlocks().get(new StrictNameVersionTuple(blockName, blockVersion)))
                                : this.store.getApplicationInvDeclaration().getBlocks().getByName(blockName);
                        if (targetBlocks.size() == 0) {
                            log.trace("Couldn't find application block with name {}:{}", blockName, blockVersion);
                        } else {
                            for (var targetBlock : targetBlocks) {
                                var decl = targetBlock.getVariables().get(varName);
                                if (decl == null) {
                                    log.trace("Couldn't find block var declaration for block {}:{}", blockName, blockVersion);
                                } else {
                                    log.trace("Found declaration inside scope {}:{}", targetBlock.getId().getName(), targetBlock.getId().getVersion());
                                    declarations.add(decl);
                                }
                            }
                        }
                    }
                    if (hash != null) {
                        var varId = new NameHashTuple(varName, hash);
                        var decl = store.getApplicationInvDeclaration().getVariables().get(varId);
                        if (decl == null) {
                            log.trace("Couldn't find variable declaration for variable {}:{}", varName, hash);
                        } else {
                            log.trace("Found plain name-hash declaration");
                            declarations.add(decl);
                        }
                    }
                }

                if (declarations.size() == 0) {
                    log.warn("No declarations found for variable. Skipping..");
                    continue;
                } else {
                    log.trace("{} var declarations found", declarations.size());
                }

                var variable = new VariableValue(new ArrayList<>(declarations), value);
                for (var decl : declarations) {
                    variables.put(decl.getId(), variable);
                }
                log.trace("Variable '{}:{}' loaded successfully", varName, hash);
            }
            this.variables = variables;

            /*
            val templates = new TreeMap<String, ConfigurationFileValue>(String.CASE_INSENSITIVE_ORDER);
            var templateNode = XmlHelper.getElementByTagName(root, "templates");
            var templateNodeChilds = Optional.ofNullable(templateNode)
                    .map(XmlHelper::getChildNodes).orElseGet(ArrayList::new);
            var allTemplatesErrorMessages = new ArrayList<String>();
            for(val elem : templateNodeChilds) {
                if (!"template".equalsIgnoreCase(elem.getNodeName()))
                    continue;

                val tid = XmlHelper.tryGetAttributeValue(elem, a -> a.getNodeName().equalsIgnoreCase("tid")).orElse(null);
                var sid = XmlHelper.tryGetAttributeValue(elem, a -> a.getNodeName().equalsIgnoreCase("s.id")).orElse(null);
                var fn = XmlHelper.tryGetAttributeValue(elem, a -> a.getNodeName().equalsIgnoreCase("f.n")).orElse(null);
                var aPath = XmlHelper.tryGetAttributeValue(elem, a -> a.getNodeName().equalsIgnoreCase("absolute-path")).orElse(null);
                var pPath = XmlHelper.tryGetAttributeValue(elem, a -> a.getNodeName().equalsIgnoreCase("prepared-path")).orElse(null);

                val errorMessages = XmlHelper.getElementsByTagName(elem, "error").stream()
                        .map(e -> XmlHelper.tryGetAttributeValue(e, "message").orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                allTemplatesErrorMessages.addAll(errorMessages);

                if (StringHelper.isNullOrEmpty(aPath))
                    continue;
                if (!store.getTemplates().containsKey(tid))
                    continue;

                templates.put(aPath, new ConfigurationFileValue(store.getTemplates().get(aPath), fn, aPath, pPath));
            }

            if (allTemplatesErrorMessages.size() > 0) {
                throw new Exception(String.join("\n", allTemplatesErrorMessages));
            }
            this.templates = templates;
            */

            return Either.right(true);
        } catch (Exception e) {
            this.variables = prevVariables;
            this.templates = prevTemplates;
            this.preparedVarsFileInfoHash = prevHash;

            return Either.left(new ApplicationException(e));
        }
    }

    public boolean isLoaded() {
        return this.preparedVarsFileInfoHash != null;
    }
}
