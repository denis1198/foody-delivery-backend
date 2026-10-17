package com.foody.delivery.restaurant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.foody.delivery.restaurant.dto.OpenRequest;
import com.foody.delivery.restaurant.dto.RestaurantSettingsRequest;
import com.foody.delivery.restaurant.dto.RestaurantSettingsResponse;
import com.foody.delivery.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seller/restaurant")
public class SellerRestaurantController {

	private final RestaurantService restaurantService;

	public SellerRestaurantController(RestaurantService restaurantService) {
		this.restaurantService = restaurantService;
	}

	@GetMapping
	public RestaurantSettingsResponse get(@AuthenticationPrincipal User seller) {
		return RestaurantSettingsResponse.from(restaurantService.ofSeller(seller));
	}

	@PutMapping
	public RestaurantSettingsResponse update(@AuthenticationPrincipal User seller,
			@Valid @RequestBody RestaurantSettingsRequest request) {
		return RestaurantSettingsResponse.from(restaurantService.updateSettings(seller, request));
	}

	@PatchMapping("/open")
	public RestaurantSettingsResponse setOpen(@AuthenticationPrincipal User seller,
			@Valid @RequestBody OpenRequest request) {
		return RestaurantSettingsResponse.from(restaurantService.setOpen(seller, request.open()));
	}

}
