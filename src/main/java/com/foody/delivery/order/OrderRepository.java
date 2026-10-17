package com.foody.delivery.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@EntityGraph(attributePaths = { "items", "createdBy" })
	List<Order> findAllByOrderByCreatedAtDescIdDesc();

	@EntityGraph(attributePaths = { "items", "createdBy" })
	List<Order> findAllByStatusOrderByCreatedAtDescIdDesc(OrderStatus status);

	@EntityGraph(attributePaths = { "items", "createdBy" })
	Optional<Order> findWithItemsById(Long id);

}
