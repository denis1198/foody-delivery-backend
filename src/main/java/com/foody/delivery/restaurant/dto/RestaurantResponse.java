package com.foody.delivery.restaurant.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.foody.delivery.restaurant.Restaurant;

/** Dados públicos do restaurante, vistos pelos clientes. */
public record RestaurantResponse(
		Long id,
		String name,
		String category,
		String description,
		String logoEmoji,
		BigDecimal deliveryFee,
		int prepTimeMin,
		int prepTimeMax,
		boolean open) {

	public static RestaurantResponse from(Restaurant r) {
		return new RestaurantResponse(r.getId(), r.getName(), r.getCategory(), r.getDescription(), r.getLogoEmoji(),
				r.getDeliveryFee().setScale(2, RoundingMode.HALF_UP), r.getPrepTimeMin(), r.getPrepTimeMax(),
				r.isOpen());
	}

}
