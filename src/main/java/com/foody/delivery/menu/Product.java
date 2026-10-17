package com.foody.delivery.menu;

import java.math.BigDecimal;

import com.foody.delivery.restaurant.Restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Item do cardápio. Produtos excluídos são apenas arquivados, porque pedidos antigos continuam
 * apontando para eles.
 */
@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id")
	private Restaurant restaurant;

	/** Nulo apenas em produtos arquivados cuja categoria foi excluída. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private MenuCategory category;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	/** Custo de produção (ingredientes + embalagem), usado no CMV e no lucro. */
	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal costPrice;

	@Column(length = 16)
	private String emoji;

	@Column(length = 500)
	private String imageUrl;

	@Column(nullable = false)
	private boolean available = true;

	@Column(nullable = false)
	private boolean archived = false;

	@Column(nullable = false)
	private int position;

	protected Product() {
	}

	public Product(Restaurant restaurant, MenuCategory category, String name, String description, BigDecimal price,
			BigDecimal costPrice, String emoji, String imageUrl, boolean available, int position) {
		this.restaurant = restaurant;
		update(category, name, description, price, costPrice, emoji, imageUrl, available);
		this.position = position;
	}

	public void update(MenuCategory category, String name, String description, BigDecimal price,
			BigDecimal costPrice, String emoji, String imageUrl, boolean available) {
		this.category = category;
		this.name = name;
		this.description = description;
		this.price = price;
		this.costPrice = costPrice;
		this.emoji = emoji;
		this.imageUrl = imageUrl;
		this.available = available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public void archive() {
		this.archived = true;
		this.available = false;
	}

	void detachFromCategory() {
		this.category = null;
	}

	public boolean isOrderable() {
		return available && !archived;
	}

	public Long getId() {
		return id;
	}

	public Restaurant getRestaurant() {
		return restaurant;
	}

	public MenuCategory getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public BigDecimal getCostPrice() {
		return costPrice;
	}

	public String getEmoji() {
		return emoji;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public boolean isAvailable() {
		return available;
	}

	public boolean isArchived() {
		return archived;
	}

	public int getPosition() {
		return position;
	}

}
