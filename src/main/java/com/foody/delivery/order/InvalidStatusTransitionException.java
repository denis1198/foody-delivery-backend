package com.foody.delivery.order;

import com.foody.delivery.common.ConflictException;

public class InvalidStatusTransitionException extends ConflictException {

	public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
		super(from.isFinal()
				? "O pedido já está " + from + " e não pode mais mudar de status."
				: "Não é possível alterar o status de " + from + " para " + to + ".");
	}

}
