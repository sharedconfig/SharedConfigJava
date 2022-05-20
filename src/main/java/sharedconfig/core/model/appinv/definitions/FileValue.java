package sharedconfig.core.model.appinv.definitions;

import lombok.Value;
import sharedconfig.core.model.appinv.declarations.TemplateDeclaration;

@Value
public class FileValue {
    TemplateDeclaration definition;
    String name;
    String aPath;
    String pPath;
}
