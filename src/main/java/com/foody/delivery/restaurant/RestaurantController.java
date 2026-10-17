package com.foody.delivery.restaurant;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.foody.delivery.menu.MenuService;
import com.foody.delivery.menu.dto.MenuResponse;
import com.foody.delivery.restaurant.dto.RestaurantResponse;

/** Vitrine de restaurantes, disponível para qualquer usuário autenticado. */
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

	private final RestaurantService restaurantService;
	private final MenuService menuService;

	public RestaurantController(RestaurantService restaurantService, MenuService menuService) {
		this.restaurantService = restaurantService;
		this.menuService = menuService;
	}

	@GetMapping
	public List<RestaurantResponse> list(@RequestParam(required = false) String category,
			@RequestParam(required = false) String q) {
		return restaurantService.search(category, q).stream().map(RestaurantResponse::from).toList();
	}

	@GetMapping("/{id}")
	public MenuResponse menu(@PathVariable Long id) {
		return menuService.publicMenu(id);
	}

}
