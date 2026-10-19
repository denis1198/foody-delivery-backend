package com.foody.delivery.order;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@EntityGraph(attributePaths = { "items", "restaurant", "customer" })
	List<Order> findAllByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

	@EntityGraph(attributePaths = { "items", "restaurant", "customer" })
	Optional<Order> findWithDetailsById(Long id);

	/** Quadro do vendedor: pedidos em andamento + tudo o que foi criado desde {@code since}. */
	@EntityGraph(attributePaths = { "items", "restaurant", "customer" })
	@Query("""
			select o from Order o
			where o.restaurant.id = :restaurantId
			  and (o.status in :activeStatuses or o.createdAt >= :since)
			order by o.createdAt desc, o.id desc""")
	List<Order> findBoard(@Param("restaurantId") Long restaurantId,
			@Param("activeStatuses") Collection<OrderStatus> activeStatuses, @Param("since") Instant since);

	/** Histórico do vendedor: todos os pedidos criados em [{@code from}, {@code to}), mais recentes primeiro. */
	@EntityGraph(attributePaths = { "items", "restaurant", "customer" })
	@Query("""
			select o from Order o
			where o.restaurant.id = :restaurantId
			  and o.createdAt >= :from and o.createdAt < :to
			order by o.createdAt desc, o.id desc""")
	List<Order> findHistory(@Param("restaurantId") Long restaurantId, @Param("from") Instant from,
			@Param("to") Instant to);

	@EntityGraph(attributePaths = { "items" })
	List<Order> findAllByRestaurantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long restaurantId,
			Instant from, Instant to);

}
