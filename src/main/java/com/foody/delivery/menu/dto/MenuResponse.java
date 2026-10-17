package com.foody.delivery.menu.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.foody.delivery.menu.MenuCategory;
import com.foody.delivery.menu.Product;
import com.foody.delivery.restaurant.dto.RestaurantResponse;

/** Cardápio público: só categorias com produtos disponíveis e sem dados de custo. */
public record MenuResponse(RestaurantResponse restaurant, List<Section> categories) {

	public record Section(Long id, String name, List<Item> products) {
	}

	public record Item(Long id, String name, String description, BigDecimal price, String emoji, String imageUrl) {

		public static Item from(Product p) {
			return new Item(p.getId(), p.getName(), p.getDescription(), p.getPrice().setScale(2, RoundingMode.HALF_UP),
					p.getEmoji(), p.getImageUrl());
		}

	}

	public static Section section(MenuCategory category, List<Product> products) {
		return new Section(category.getId(), category.getName(), products.stream().map(Item::from).toList());
	}

}
