package sharedconfig.core.model.appinv.declarations;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class TemplateDeclaration {
    @NotNull String id;
    @NotNull String name;
    @NotNull String path;
    @NotNull String hash;
}
