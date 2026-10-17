package com.foody.delivery.order.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.foody.delivery.order.DeliveryAddress;
import com.foody.delivery.order.Order;
import com.foody.delivery.order.OrderItem;
import com.foody.delivery.order.OrderStatus;
import com.foody.delivery.order.PaymentMethod;

/**
 * Pedido visto pelo cliente ou pelo vendedor. {@code nextStatuses} traz só as ações que quem está vendo
 * pode executar, e {@code financials} só é preenchido para o vendedor.
 */
public record OrderResponse(
		Long id,
		RestaurantRef restaurant,
		String customerName,
		Address deliveryAddress,
		List<Item> items,
		PaymentMethod paymentMethod,
		String notes,
		BigDecimal subtotal,
		BigDecimal deliveryFee,
		BigDecimal total,
		OrderStatus status,
		List<OrderStatus> nextStatuses,
		Financials financials,
		Instant createdAt,
		Instant updatedAt) {

	public record RestaurantRef(Long id, String name, String logoEmoji) {
	}

	public record Address(String street, String number, String complement, String neighborhood, String city,
			String zipCode) {

		static Address from(DeliveryAddress a) {
			return new Address(a.getStreet(), a.getNumber(), a.getComplement(), a.getNeighborhood(), a.getCity(),
					a.getZipCode());
		}

	}

	public record Item(Long id, Long productId, String productName, int quantity, BigDecimal unitPrice,
			BigDecimal subtotal, String notes) {

		static Item from(OrderItem i) {
			return new Item(i.getId(), i.getProductId(), i.getProductName(), i.getQuantity(), money(i.getUnitPrice()),
					money(i.getSubtotal()), i.getNotes());
		}

	}

	public record Financials(BigDecimal commission, BigDecimal paymentFee, BigDecimal tax, BigDecimal cost,
			BigDecimal net, BigDecimal profit) {

		static Financials from(Order o) {
			return new Financials(money(o.getCommissionAmount()), money(o.getPaymentFeeAmount()),
					money(o.getTaxAmount()), money(o.getCostAmount()), money(o.getNetAmount()),
					money(o.getProfitAmount()));
		}

	}

	/** O cliente só pode cancelar enquanto o restaurante não começou o preparo. */
	public static OrderResponse forCustomer(Order order) {
		var next = order.getStatus() == OrderStatus.RECEBIDO ? List.of(OrderStatus.CANCELADO) : List.<OrderStatus>of();
		return build(order, next, null);
	}

	public static OrderResponse forSeller(Order order) {
		var next = order.getStatus().nextStatuses().stream().sorted(Comparator.naturalOrder()).toList();
		return build(order, next, Financials.from(order));
	}

	private static OrderResponse build(Order order, List<OrderStatus> next, Financials financials) {
		var r = order.getRestaurant();
		return new OrderResponse(
				order.getId(),
				new RestaurantRef(r.getId(), r.getName(), r.getLogoEmoji()),
				order.getCustomer().getName(),
				Address.from(order.getDeliveryAddress()),
				order.getItems().stream().map(Item::from).toList(),
				order.getPaymentMethod(),
				order.getNotes(),
				money(order.getSubtotal()),
				money(order.getDeliveryFee()),
				money(order.getTotal()),
				order.getStatus(),
				next,
				financials,
				order.getCreatedAt(),
				order.getUpdatedAt());
	}

	/** SQLite não preserva a escala do NUMERIC; padroniza valores monetários com 2 casas. */
	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

}
