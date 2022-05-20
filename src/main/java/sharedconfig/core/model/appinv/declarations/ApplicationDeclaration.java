package sharedconfig.core.model.appinv.declarations;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import sharedconfig.utils.NameVersionMap;
import sharedconfig.utils.tuples.StrictNameVersionTuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class ApplicationDeclaration {
    private final @NotNull StrictNameVersionTuple id;
    private final @NotNull List<AlertDeclaration> alerts = new ArrayList<>();
    private final @NotNull NameVersionMap<BlockDeclaration> blocks = new NameVersionMap<>();
    private final @NotNull HashMap<String, VariableDeclaration> variables = new HashMap<>();
    private final @NotNull HashMap<String, TemplateDeclaration> templates = new HashMap<>();

    public ApplicationDeclaration(@NotNull String name, @NotNull String version) {
        this.id = new StrictNameVersionTuple(name, version);
    }
}
