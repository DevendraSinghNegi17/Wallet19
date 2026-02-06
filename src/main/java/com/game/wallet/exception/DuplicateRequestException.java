package com.game.wallet.exception;

public class DuplicateRequestException extends RuntimeException {
    public DuplicateRequestException() {
        super("Duplicate idempotent request");
    }
}
