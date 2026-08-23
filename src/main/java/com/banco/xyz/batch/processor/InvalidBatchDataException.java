package com.banco.xyz.batch.processor;

public class InvalidBatchDataException extends RuntimeException {

    public InvalidBatchDataException(String message) {
        super(message);
    }
}
