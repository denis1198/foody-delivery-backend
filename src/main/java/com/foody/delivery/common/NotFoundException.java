package com.foody.delivery.common;

/** Recurso inexistente. Vira HTTP 404. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

}
