package com.foody.delivery.menu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findAllByRestaurantIdAndArchivedFalseOrderByPositionAscIdAsc(Long restaurantId);

	Optional<Product> findByIdAndRestaurantIdAndArchivedFalse(Long id, Long restaurantId);

	List<Product> findAllByIdIn(Collection<Long> ids);

	boolean existsByCategoryIdAndArchivedFalse(Long categoryId);

	List<Product> findAllByCategoryId(Long categoryId);

}
