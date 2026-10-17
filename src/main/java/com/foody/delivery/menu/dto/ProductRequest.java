package com.foody.delivery.menu.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
		@NotNull Long categoryId,
		@NotBlank @Size(max = 120) String name,
		@Size(max = 500) String description,
		@NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal price,
		@NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2) BigDecimal costPrice,
		@Size(max = 16) String emoji,
		@Size(max = 500) String imageUrl,
		Boolean available) {
}
