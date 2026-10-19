package com.foody.delivery.order;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	/**
	 * Sem datas: o quadro (pedidos em andamento + criados hoje). Com {@code from}/{@code to}: todos os pedidos
	 * criados no período, em qualquer status.
	 */
	@GetMapping
	public List<OrderResponse> list(@AuthenticationPrincipal User seller,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		if (from == null && to == null) {
			return orderService.board(seller);
		}
		return orderService.history(seller, from, to);
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
