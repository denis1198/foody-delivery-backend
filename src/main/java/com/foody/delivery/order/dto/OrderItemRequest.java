package com.foody.delivery.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Item do carrinho. Preço e custo nunca vêm do cliente: são lidos do cardápio. */
public record OrderItemRequest(
		@NotNull Long productId,
		@NotNull @Min(1) @Max(99) Integer quantity,
		@Size(max = 300) String notes) {
}
