package sharedconfig.core.loger.configuration;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

/**
 *  Соответствует %msg с дополнительным экранированием символов '\n', '\r' и '\'
 */

@Plugin(name = "TlogMessagePatternConverter", category = "Converter")
@ConverterKeys({"tlogmsg"})
public final class TlogMessagePatternConverter extends LogEventPatternConverter {

    private TlogMessagePatternConverter() {
        super("Tlog Message Pattern Converter", "tlogmsg");
    }

    public static TlogMessagePatternConverter newInstance() {
        return new TlogMessagePatternConverter();
    }

    /**
     * @param event событие которое содержит данные лога
     * @param toAppendTo буффер в который нужно передать сообщение для логирования
     */
    public void format(LogEvent event, StringBuilder toAppendTo) {
        var msg = event.getMessage().getFormattedMessage();

        for (int i = 0; i < msg.length(); i++) {
            char curChar = msg.charAt(i);
            if (curChar == '\n')
                toAppendTo.append("\\n");
            else if (curChar == '\r')
                toAppendTo.append("\\r");
            else if (curChar == '\\')
                toAppendTo.append("\\\\");
            else
                toAppendTo.append(msg.charAt(i));
        }
    }
}
