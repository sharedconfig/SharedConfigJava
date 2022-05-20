package sharedconfig.core.model.appinv.definitions;

import lombok.Value;
import sharedconfig.core.model.appinv.declarations.VariableDeclaration;

import java.util.List;

@Value
public class VariableValue {
    List<VariableDeclaration> declarations;
    String value;
}
