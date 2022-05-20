package sharedconfig.core.model.appinv.declarations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import sharedconfig.utils.NameHashMap;
import sharedconfig.utils.NameVersionMap;

import java.util.List;

@AllArgsConstructor @Getter
public class ApplicationInvDeclaration {
    private final @NotNull ApplicationDeclaration application;
    private final @NotNull List<AlertDeclaration> alerts;
    private final @NotNull NameVersionMap<BlockDeclaration> blocks;
    private final @NotNull NameHashMap<VariableDeclaration> variables;
}
