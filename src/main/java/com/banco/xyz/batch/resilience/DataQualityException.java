package com.banco.xyz.batch.resilience;

public class DataQualityException extends RuntimeException {

    public DataQualityException(String message) {
        super(message);
    }
}
