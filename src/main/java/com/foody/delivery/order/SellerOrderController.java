package com.foody.delivery.order;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.foody.delivery.order.dto.OrderResponse;
import com.foody.delivery.order.dto.UpdateStatusRequest;
import com.foody.delivery.user.User;

import jakarta.validation.Valid;

/** Pedidos do restaurante do vendedor autenticado. */
@RestController
@RequestMapping("/api/seller/orders")
public class SellerOrderController {

	private final OrderService orderService;

	public SellerOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	/** Pedidos em andamento + finalizados hoje. */
	@GetMapping
	public List<OrderResponse> board(@AuthenticationPrincipal User seller) {
		return orderService.board(seller);
	}

	@GetMapping("/{id}")
	public OrderResponse get(@PathVariable Long id, @AuthenticationPrincipal User seller) {
		return orderService.getForSeller(id, seller);
	}

	@PatchMapping("/{id}/status")
	public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request,
			@AuthenticationPrincipal User seller) {
		return orderService.updateStatusBySeller(id, request.status(), seller);
	}

}
