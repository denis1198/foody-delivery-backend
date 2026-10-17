package com.foody.delivery.common;

/** Erro de validação de regra de negócio ligado a um campo. Vira HTTP 400 com o mesmo formato da Bean Validation. */
public class BadRequestException extends RuntimeException {

	private final String field;

	public BadRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}

}
