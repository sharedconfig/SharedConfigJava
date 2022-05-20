package sharedconfig.core.model.appinv.declarations;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import sharedconfig.utils.tuples.StrictNameVersionTuple;

import java.util.HashMap;

@Getter
public class BlockDeclaration {
    private final @NotNull StrictNameVersionTuple id;
    private final @NotNull HashMap<String, VariableDeclaration> variables = new HashMap<>();
    private final @NotNull HashMap<String, TemplateDeclaration> templates = new HashMap<>();

    public BlockDeclaration(@NotNull String name, @NotNull String version) {
        this.id = new StrictNameVersionTuple(name, version);
    }
}
