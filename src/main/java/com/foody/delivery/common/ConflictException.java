package com.foody.delivery.common;

/** Requisição válida, mas conflita com o estado atual (ex.: e-mail já cadastrado). Vira HTTP 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
