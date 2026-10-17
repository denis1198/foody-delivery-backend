package com.foody.delivery.menu.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.foody.delivery.menu.MenuCategory;
import com.foody.delivery.menu.Product;
import com.foody.delivery.restaurant.Restaurant;

/**
 * Cardápio completo para o vendedor, com custos, itens indisponíveis e os percentuais do restaurante
 * (para o front estimar a margem de cada produto enquanto ele é editado).
 */
public record SellerMenuResponse(Fees fees, List<Section> categories) {

	public record Fees(BigDecimal commissionPercent, BigDecimal paymentFeePercent, BigDecimal taxPercent) {

		public static Fees from(Restaurant r) {
			return new Fees(r.getCommissionPercent(), r.getPaymentFeePercent(), r.getTaxPercent());
		}

	}

	public record Section(Long id, String name, int position, List<ProductResponse> products) {

		public static Section from(MenuCategory category, List<Product> products) {
			return new Section(category.getId(), category.getName(), category.getPosition(),
					products.stream().map(ProductResponse::from).toList());
		}

	}

	public record ProductResponse(Long id, Long categoryId, String name, String description, BigDecimal price,
			BigDecimal costPrice, String emoji, String imageUrl, boolean available, int position) {

		public static ProductResponse from(Product p) {
			return new ProductResponse(p.getId(), p.getCategory() == null ? null : p.getCategory().getId(),
					p.getName(), p.getDescription(), money(p.getPrice()), money(p.getCostPrice()), p.getEmoji(),
					p.getImageUrl(), p.isAvailable(), p.getPosition());
		}

	}

	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

}
