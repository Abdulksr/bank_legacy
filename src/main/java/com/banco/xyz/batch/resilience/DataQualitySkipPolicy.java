package com.banco.xyz.batch.resilience;

import java.time.format.DateTimeParseException;

import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.validation.BindException;

public class DataQualitySkipPolicy implements SkipPolicy {

    public static final int MAX_SKIPS = 10;

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {
        return skipCount < MAX_SKIPS && isDataQualityProblem(throwable);
    }

    private boolean isDataQualityProblem(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof DataQualityException
                    || current instanceof FlatFileParseException
                    || current instanceof BindException
                    || current instanceof ConversionFailedException
                    || current instanceof ConversionNotSupportedException
                    || current instanceof NumberFormatException
                    || current instanceof DateTimeParseException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
