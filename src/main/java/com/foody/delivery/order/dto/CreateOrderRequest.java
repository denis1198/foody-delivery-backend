package com.foody.delivery.order.dto;

import java.util.List;

import com.foody.delivery.order.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
		@NotNull Long restaurantId,
		@NotEmpty @Size(max = 50) List<@NotNull @Valid OrderItemRequest> items,
		@NotNull @Valid AddressRequest deliveryAddress,
		@NotNull PaymentMethod paymentMethod,
		@Size(max = 300) String notes) {
}
