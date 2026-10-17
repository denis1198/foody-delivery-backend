package com.foody.delivery.restaurant.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RestaurantSettingsRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 60) String category,
		@Size(max = 500) String description,
		@NotBlank @Size(max = 16) String logoEmoji,
		@NotNull @DecimalMin("0.00") @DecimalMax("99.99") BigDecimal deliveryFee,
		@Min(1) @Max(240) int prepTimeMin,
		@Min(1) @Max(240) int prepTimeMax,
		@NotNull @DecimalMin("0.00") @DecimalMax("50.00") BigDecimal commissionPercent,
		@NotNull @DecimalMin("0.00") @DecimalMax("20.00") BigDecimal paymentFeePercent,
		@NotNull @DecimalMin("0.00") @DecimalMax("40.00") BigDecimal taxPercent) {
}
