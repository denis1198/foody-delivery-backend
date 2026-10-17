package com.foody.delivery.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
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
			return new Item(i.getId(), i.getProductName(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal());
		}

	}

	public static OrderResponse from(Order order) {
		return new OrderResponse(
				order.getId(),
				order.getCustomerName(),
				Address.from(order.getDeliveryAddress()),
				order.getItems().stream().map(Item::from).toList(),
				order.getTotal(),
				order.getStatus(),
				order.getCreatedBy().getName(),
				order.getCreatedAt(),
				order.getUpdatedAt());
	}

}
