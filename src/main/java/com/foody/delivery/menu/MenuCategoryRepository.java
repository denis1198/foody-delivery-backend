package com.foody.delivery.menu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

	List<MenuCategory> findAllByRestaurantIdOrderByPositionAscIdAsc(Long restaurantId);

	Optional<MenuCategory> findByIdAndRestaurantId(Long id, Long restaurantId);

	int countByRestaurantId(Long restaurantId);

}
