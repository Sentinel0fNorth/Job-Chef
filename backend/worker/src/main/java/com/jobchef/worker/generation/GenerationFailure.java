package com.jobchef.worker.generation;

public class GenerationFailure extends RuntimeException {
    private final String code;
    private final boolean retryable;
    private final boolean quota;

    public GenerationFailure(String code, boolean retryable, boolean quota, String message) {
        super(message);
        this.code = code;
        this.retryable = retryable;
        this.quota = quota;
    }

    public String code() { return code; }
    public boolean retryable() { return retryable; }
    public boolean quota() { return quota; }
}
