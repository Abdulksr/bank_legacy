package com.banco.xyz.batch.config;

import com.banco.xyz.batch.processor.InvalidBatchDataException;
import java.time.format.DateTimeParseException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.validation.BindException;
import org.springframework.stereotype.Component;

@Component
public class InvalidDataSkipPolicy implements SkipPolicy {

    public static final int MAX_SKIP_COUNT = 10;

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {
        return skipCount < MAX_SKIP_COUNT && isInvalidData(throwable);
    }

    private boolean isInvalidData(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InvalidBatchDataException
                    || current instanceof FlatFileParseException
                    || current instanceof BindException
                    || current instanceof ConversionFailedException
                    || current instanceof NumberFormatException
                    || current instanceof DateTimeParseException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
