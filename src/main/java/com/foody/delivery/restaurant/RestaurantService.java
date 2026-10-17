package com.foody.delivery.restaurant;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foody.delivery.common.NotFoundException;
import com.foody.delivery.restaurant.dto.RestaurantSettingsRequest;
import com.foody.delivery.user.User;

@Service
public class RestaurantService {

	private final RestaurantRepository restaurants;

	public RestaurantService(RestaurantRepository restaurants) {
		this.restaurants = restaurants;
	}

	@Transactional
	public Restaurant createFor(User owner, String name, String category) {
		return restaurants.save(new Restaurant(owner, name.trim(), category.trim()));
	}

	@Transactional(readOnly = true)
	public List<Restaurant> search(String category, String q) {
		return restaurants.search(blankToNull(category), blankToNull(q));
	}

	@Transactional(readOnly = true)
	public Restaurant get(Long id) {
		return restaurants.findById(id)
				.orElseThrow(() -> new NotFoundException("Restaurante " + id + " não encontrado."));
	}

	/** Restaurante do vendedor autenticado. */
	@Transactional(readOnly = true)
	public Restaurant ofSeller(User seller) {
		return restaurants.findByOwnerId(seller.getId())
				.orElseThrow(() -> new NotFoundException("Nenhum restaurante vinculado a este usuário."));
	}

	@Transactional
	public Restaurant updateSettings(User seller, RestaurantSettingsRequest request) {
		Restaurant restaurant = ofSeller(seller);
		restaurant.updateProfile(request.name().trim(), request.category().trim(), blankToNull(request.description()),
				request.logoEmoji().trim(), request.deliveryFee(), request.prepTimeMin(),
				Math.max(request.prepTimeMin(), request.prepTimeMax()));
		restaurant.updateFees(request.commissionPercent(), request.paymentFeePercent(), request.taxPercent());
		return restaurants.save(restaurant);
	}

	@Transactional
	public Restaurant setOpen(User seller, boolean open) {
		Restaurant restaurant = ofSeller(seller);
		restaurant.setOpen(open);
		return restaurants.save(restaurant);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
