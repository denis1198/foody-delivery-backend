package com.foody.delivery.restaurant.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.foody.delivery.restaurant.Restaurant;

/** Visão completa do restaurante para o próprio vendedor, incluindo as taxas usadas nas métricas. */
public record RestaurantSettingsResponse(
		Long id,
		String name,
		String category,
		String description,
		String logoEmoji,
		BigDecimal deliveryFee,
		int prepTimeMin,
		int prepTimeMax,
		boolean open,
		BigDecimal commissionPercent,
		BigDecimal paymentFeePercent,
		BigDecimal taxPercent) {

	public static RestaurantSettingsResponse from(Restaurant r) {
		return new RestaurantSettingsResponse(r.getId(), r.getName(), r.getCategory(), r.getDescription(),
				r.getLogoEmoji(), scale(r.getDeliveryFee()), r.getPrepTimeMin(), r.getPrepTimeMax(), r.isOpen(),
				scale(r.getCommissionPercent()), scale(r.getPaymentFeePercent()), scale(r.getTaxPercent()));
	}

	private static BigDecimal scale(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

}
