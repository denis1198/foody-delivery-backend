package com.foody.delivery.order;

import java.util.Set;

/**
 * Ciclo de vida do pedido: RECEBIDO → EM_PREPARO → SAIU_PARA_ENTREGA → ENTREGUE.
 * Pode ser CANCELADO em qualquer etapa antes da entrega. ENTREGUE e CANCELADO são finais.
 */
public enum OrderStatus {
	RECEBIDO,
	EM_PREPARO,
	SAIU_PARA_ENTREGA,
	ENTREGUE,
	CANCELADO;

	public Set<OrderStatus> nextStatuses() {
		return switch (this) {
			case RECEBIDO -> Set.of(EM_PREPARO, CANCELADO);
			case EM_PREPARO -> Set.of(SAIU_PARA_ENTREGA, CANCELADO);
			case SAIU_PARA_ENTREGA -> Set.of(ENTREGUE, CANCELADO);
			case ENTREGUE, CANCELADO -> Set.of();
		};
	}

	public boolean canTransitionTo(OrderStatus target) {
		return nextStatuses().contains(target);
	}

	public boolean isFinal() {
		return nextStatuses().isEmpty();
	}

}
