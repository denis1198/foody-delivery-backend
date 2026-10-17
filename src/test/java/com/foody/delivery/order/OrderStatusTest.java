package com.foody.delivery.order;

import static com.foody.delivery.order.OrderStatus.CANCELADO;
import static com.foody.delivery.order.OrderStatus.EM_PREPARO;
import static com.foody.delivery.order.OrderStatus.ENTREGUE;
import static com.foody.delivery.order.OrderStatus.RECEBIDO;
import static com.foody.delivery.order.OrderStatus.SAIU_PARA_ENTREGA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderStatusTest {

	@ParameterizedTest
	@CsvSource({
			"RECEBIDO, EM_PREPARO",
			"EM_PREPARO, SAIU_PARA_ENTREGA",
			"SAIU_PARA_ENTREGA, ENTREGUE",
			"RECEBIDO, CANCELADO",
			"EM_PREPARO, CANCELADO",
			"SAIU_PARA_ENTREGA, CANCELADO" })
	void allowsForwardStepsAndCancellation(OrderStatus from, OrderStatus to) {
		assertThat(from.canTransitionTo(to)).isTrue();
	}

	@ParameterizedTest
	@CsvSource({
			"RECEBIDO, SAIU_PARA_ENTREGA",
			"RECEBIDO, ENTREGUE",
			"EM_PREPARO, RECEBIDO",
			"SAIU_PARA_ENTREGA, EM_PREPARO",
			"RECEBIDO, RECEBIDO",
			"ENTREGUE, CANCELADO",
			"CANCELADO, RECEBIDO" })
	void rejectsSkippingGoingBackAndLeavingFinalStatus(OrderStatus from, OrderStatus to) {
		assertThat(from.canTransitionTo(to)).isFalse();
	}

	@Test
	void deliveredAndCancelledAreFinal() {
		assertThat(ENTREGUE.isFinal()).isTrue();
		assertThat(CANCELADO.isFinal()).isTrue();
		assertThat(RECEBIDO.isFinal()).isFalse();
	}

	@Test
	void orderStartsReceivedAndRejectsInvalidTransition() {
		var order = new Order("Cliente", null,
				List.of(new OrderItem("Lanche", 2, new BigDecimal("10.50"))), null);

		assertThat(order.getStatus()).isEqualTo(RECEBIDO);
		assertThat(order.getTotal()).isEqualByComparingTo("21.00");

		order.changeStatus(EM_PREPARO);
		assertThat(order.getStatus()).isEqualTo(EM_PREPARO);

		assertThatThrownBy(() -> order.changeStatus(ENTREGUE))
				.isInstanceOf(InvalidStatusTransitionException.class);
		assertThat(order.getStatus()).isEqualTo(EM_PREPARO);

		order.changeStatus(SAIU_PARA_ENTREGA);
		order.changeStatus(ENTREGUE);
		assertThatThrownBy(() -> order.changeStatus(CANCELADO))
				.isInstanceOf(InvalidStatusTransitionException.class)
				.hasMessageContaining("não pode mais mudar");
	}

}
