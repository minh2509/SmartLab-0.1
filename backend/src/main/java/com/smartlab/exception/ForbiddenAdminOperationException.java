package com.smartlab.exception;

public class ForbiddenAdminOperationException extends RuntimeException {

	public ForbiddenAdminOperationException(String message) {
		super(message);
	}
}
