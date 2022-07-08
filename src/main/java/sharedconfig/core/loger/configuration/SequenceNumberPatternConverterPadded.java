package sharedconfig.core.loger.configuration;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  Converter Plugin который инкрементирует значение.
 */

@Plugin(name = "SequenceNumberPaddedPatternConverter", category = "Converter")
@ConverterKeys({"snp", "sequenceNumberPadded"})
public final class SequenceNumberPatternConverterPadded extends LogEventPatternConverter {
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private SequenceNumberPatternConverterPadded() {

        super("Sequence Number Padded", "snp");
    }

    public static SequenceNumberPatternConverterPadded newInstance() {
        return new SequenceNumberPatternConverterPadded();
    }

    /**
     * @param event событие которое содержит данные лога
     * @param toAppendTo буффер в который нужно передать сообщение для логирования
     */
    public void format(LogEvent event, StringBuilder toAppendTo) {
        toAppendTo.append(SEQUENCE.incrementAndGet());
    }
}
