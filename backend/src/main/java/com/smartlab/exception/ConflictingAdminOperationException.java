package com.smartlab.exception;

public class ConflictingAdminOperationException extends RuntimeException {

	public ConflictingAdminOperationException(String message) {
		super(message);
	}
}
