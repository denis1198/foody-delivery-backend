package com.foody.delivery.order;

public enum PaymentMethod {
	PIX(true),
	CARTAO(true),
	/** Pago ao entregador: não passa pelo processamento de pagamento online. */
	DINHEIRO(false);

	private final boolean online;

	PaymentMethod(boolean online) {
		this.online = online;
	}

	public boolean isOnline() {
		return online;
	}
}
