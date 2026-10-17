package com.foody.delivery.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
		@NotBlank @Size(max = 120) String customerName,
		@NotNull @Valid AddressRequest deliveryAddress,
		@NotEmpty @Size(max = 50) List<@NotNull @Valid OrderItemRequest> items) {
}
