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

public record OrderResponse(
		Long id,
		String customerName,
		Address deliveryAddress,
		List<Item> items,
		BigDecimal total,
		OrderStatus status,
		List<OrderStatus> nextStatuses,
		String createdBy,
		Instant createdAt,
		Instant updatedAt) {

	public record Address(String street, String number, String complement, String neighborhood, String city,
			String zipCode) {

		static Address from(DeliveryAddress a) {
			return new Address(a.getStreet(), a.getNumber(), a.getComplement(), a.getNeighborhood(), a.getCity(),
					a.getZipCode());
		}

	}

	public record Item(Long id, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {

		static Item from(OrderItem i) {
			return new Item(i.getId(), i.getProductName(), i.getQuantity(), money(i.getUnitPrice()),
					money(i.getSubtotal()));
		}

	}

	public static OrderResponse from(Order order) {
		return new OrderResponse(
				order.getId(),
				order.getCustomerName(),
				Address.from(order.getDeliveryAddress()),
				order.getItems().stream().map(Item::from).toList(),
				money(order.getTotal()),
				order.getStatus(),
				order.getStatus().nextStatuses().stream().sorted(Comparator.naturalOrder()).toList(),
				order.getCreatedBy().getName(),
				order.getCreatedAt(),
				order.getUpdatedAt());
	}

	/** SQLite não preserva a escala do NUMERIC; padroniza valores monetários com 2 casas. */
	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

}
