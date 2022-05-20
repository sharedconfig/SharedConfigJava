package sharedconfig.helpers;

import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sharedconfig.utils.Either;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Predicate;

public class XmlHelper {
    /**
     * Получить первый дочерний элемент узла с именем
     * @param parent родительский узел
     * @param tagName имя дочернего узла
     * @return
     */
    public static @Nullable Element getElementByTagName(@NotNull Element parent, @NotNull String tagName)
    {
        for(Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if(child instanceof Element && tagName.equals(child.getNodeName()))
                return (Element) child;
        }
        return null;
    }

    /**
     * Получить список дочерних элементов узла с именем
     * @param parent родительский узел
     * @param tagName имя дочернего узла
     * @return
     */
    public static @NotNull ArrayList<Element> getElementsByTagName(@NotNull Element parent, @NotNull String tagName)
    {
        val result = new ArrayList<Element>();
        for(Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if(child instanceof Element && tagName.equals(child.getNodeName()))
                result.add((Element) child);
        }
        return result;
    }

    public static @NotNull ArrayList<Element> getChildNodes(@NotNull Element parent) {
        val result = new ArrayList<Element>();
        for(Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if(child instanceof Element)
                result.add((Element) child);
        }
        return result;
    }


    public static boolean hasChildNodesWithName(@NotNull Element parent, @NotNull String tagName) {
        for(Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if(child instanceof Element && tagName.equals(child.getNodeName()))
                return true;
        }
        return false;
    }

    public static Optional<String> tryGetAttributeValue(@NotNull Element element, @NotNull String attributeName) {
        return element.hasAttribute(attributeName)
                ? Optional.ofNullable(element.getAttribute(attributeName))
                : Optional.empty();
    }

    public static Optional<String> tryGetAttributeValueIgnoreCase(@NotNull Element element, @NotNull String caseInsensetiveAttributeName) {
        for (int i = 0; i < element.getAttributes().getLength(); ++i) {
            val attribute = (Attr)element.getAttributes().item(i);
            if (StringHelper.equalsIgnoreCase(caseInsensetiveAttributeName, attribute.getNodeName())) {
                return Optional.ofNullable(attribute.getValue());
            }

        }
        return Optional.empty();
    }



    public static Optional<String> tryGetAttributeValue(@NotNull Element element, @NotNull Predicate<Attr> predicate) {
        for (int i = 0; i < element.getAttributes().getLength(); ++i) {
            val attribute = (Attr)element.getAttributes().item(i);
            if (predicate.test(attribute)) {
                return Optional.ofNullable(attribute.getValue());
            }
        }
        return Optional.empty();
    }

    @SneakyThrows
    public static String toXmlString(Document doc) {
        val transformer = TransformerFactory.newInstance().newTransformer();
        val writer = new StringWriter();
        val output = new StreamResult(writer);
        val input = new DOMSource(doc);
        transformer.transform(input, output);
        return writer.toString();
    }

    @SneakyThrows
    public static void writeToFile(Document doc, Path targetFile) {
        val transformer = TransformerFactory.newInstance().newTransformer();
        val file = new File(targetFile.toAbsolutePath().toString());
        val output = new StreamResult(file);
        val input = new DOMSource(doc);
        transformer.transform(input, output);
        //output.getWriter().flush();
        //output.getWriter().close();
    }

    public static Either<Exception, Document> tryLoadDocument(Path path) {
        if (path == null || !Files.exists(path)) {
            return Either.left(new FileNotFoundException(String.format("File [%s] doesnt exists", path)));
        }
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        Document doc = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(path.toString());
            return Either.right(doc);
        } catch (Exception e) {
            return Either.left(e);
        }
    }
}
