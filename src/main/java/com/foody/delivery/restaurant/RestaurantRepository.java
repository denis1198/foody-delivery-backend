package com.foody.delivery.restaurant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

	Optional<Restaurant> findByOwnerId(Long ownerId);

	@Query("""
			select r from Restaurant r
			where (:category is null or r.category = :category)
			  and (:q is null or lower(r.name) like lower(concat('%', :q, '%'))
			       or lower(r.category) like lower(concat('%', :q, '%')))
			order by r.open desc, r.name asc""")
	List<Restaurant> search(@Param("category") String category, @Param("q") String q);

}
