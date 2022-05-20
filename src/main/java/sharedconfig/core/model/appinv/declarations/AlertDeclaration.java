package sharedconfig.core.model.appinv.declarations;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class AlertDeclaration {
    @NotNull String type;
    @NotNull String message;
}
