package com.foody.delivery.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foody.delivery.common.NotFoundException;
import com.foody.delivery.order.dto.AddressRequest;
import com.foody.delivery.order.dto.CreateOrderRequest;
import com.foody.delivery.order.dto.OrderResponse;
import com.foody.delivery.user.User;

@Service
public class OrderService {

	private final OrderRepository orders;

	public OrderService(OrderRepository orders) {
		this.orders = orders;
	}

	@Transactional
	public OrderResponse create(CreateOrderRequest request, User author) {
		AddressRequest a = request.deliveryAddress();
		var address = new DeliveryAddress(a.street().trim(), a.number().trim(), blankToNull(a.complement()),
				a.neighborhood().trim(), a.city().trim(), a.zipCode().trim());
		var items = request.items().stream()
				.map(i -> new OrderItem(i.productName().trim(), i.quantity(), i.unitPrice()))
				.toList();
		return OrderResponse.from(orders.save(new Order(request.customerName().trim(), address, items, author)));
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> list(OrderStatus status) {
		var result = status == null
				? orders.findAllByOrderByCreatedAtDescIdDesc()
				: orders.findAllByStatusOrderByCreatedAtDescIdDesc(status);
		return result.stream().map(OrderResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public OrderResponse get(Long id) {
		return OrderResponse.from(find(id));
	}

	@Transactional
	public OrderResponse updateStatus(Long id, OrderStatus newStatus) {
		Order order = find(id);
		order.changeStatus(newStatus);
		return OrderResponse.from(orders.saveAndFlush(order));
	}

	private Order find(Long id) {
		return orders.findWithItemsById(id)
				.orElseThrow(() -> new NotFoundException("Pedido " + id + " não encontrado."));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
