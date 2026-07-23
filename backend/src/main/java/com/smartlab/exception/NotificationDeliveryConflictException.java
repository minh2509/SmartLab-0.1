package com.smartlab.exception;

public class NotificationDeliveryConflictException extends RuntimeException {

	public NotificationDeliveryConflictException(String message) {
		super(message);
	}

	public NotificationDeliveryConflictException(String message, Throwable cause) {
		super(message, cause);
	}
}
