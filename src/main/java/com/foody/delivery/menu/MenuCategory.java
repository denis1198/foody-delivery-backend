package com.foody.delivery.menu;

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

/** Seção do cardápio (ex.: "Pizzas salgadas", "Bebidas"). */
@Entity
@Table(name = "menu_categories")
public class MenuCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id")
	private Restaurant restaurant;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(nullable = false)
	private int position;

	protected MenuCategory() {
	}

	public MenuCategory(Restaurant restaurant, String name, int position) {
		this.restaurant = restaurant;
		this.name = name;
		this.position = position;
	}

	public void rename(String name) {
		this.name = name;
	}

	public void moveTo(int position) {
		this.position = position;
	}

	public Long getId() {
		return id;
	}

	public Restaurant getRestaurant() {
		return restaurant;
	}

	public String getName() {
		return name;
	}

	public int getPosition() {
		return position;
	}

}
