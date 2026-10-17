package com.foody.delivery.order;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.foody.delivery.order.dto.CreateOrderRequest;
import com.foody.delivery.order.dto.OrderResponse;
import com.foody.delivery.user.User;

import jakarta.validation.Valid;

/** Pedidos do ponto de vista do cliente. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse create(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal User user) {
		return orderService.create(request, user);
	}

	@GetMapping("/mine")
	public List<OrderResponse> mine(@AuthenticationPrincipal User user) {
		return orderService.listForCustomer(user);
	}

	@GetMapping("/{id}")
	public OrderResponse get(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return orderService.getForCustomer(id, user);
	}

	@PatchMapping("/{id}/cancel")
	public OrderResponse cancel(@PathVariable Long id, @AuthenticationPrincipal User user) {
		return orderService.cancelByCustomer(id, user);
	}

}
