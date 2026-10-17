package com.foody.delivery.menu;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.foody.delivery.menu.dto.AvailabilityRequest;
import com.foody.delivery.menu.dto.CategoryRequest;
import com.foody.delivery.menu.dto.ProductRequest;
import com.foody.delivery.menu.dto.SellerMenuResponse;
import com.foody.delivery.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seller/menu")
public class SellerMenuController {

	private final MenuService menuService;

	public SellerMenuController(MenuService menuService) {
		this.menuService = menuService;
	}

	@GetMapping
	public SellerMenuResponse menu(@AuthenticationPrincipal User seller) {
		return menuService.sellerMenu(seller);
	}

	@PostMapping("/categories")
	@ResponseStatus(HttpStatus.CREATED)
	public SellerMenuResponse.Section createCategory(@AuthenticationPrincipal User seller,
			@Valid @RequestBody CategoryRequest request) {
		return menuService.createCategory(seller, request);
	}

	@PutMapping("/categories/{id}")
	public SellerMenuResponse.Section updateCategory(@AuthenticationPrincipal User seller, @PathVariable Long id,
			@Valid @RequestBody CategoryRequest request) {
		return menuService.updateCategory(seller, id, request);
	}

	@DeleteMapping("/categories/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCategory(@AuthenticationPrincipal User seller, @PathVariable Long id) {
		menuService.deleteCategory(seller, id);
	}

	@PostMapping("/products")
	@ResponseStatus(HttpStatus.CREATED)
	public SellerMenuResponse.ProductResponse createProduct(@AuthenticationPrincipal User seller,
			@Valid @RequestBody ProductRequest request) {
		return menuService.createProduct(seller, request);
	}

	@PutMapping("/products/{id}")
	public SellerMenuResponse.ProductResponse updateProduct(@AuthenticationPrincipal User seller,
			@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return menuService.updateProduct(seller, id, request);
	}

	@PatchMapping("/products/{id}/availability")
	public SellerMenuResponse.ProductResponse setAvailability(@AuthenticationPrincipal User seller,
			@PathVariable Long id, @Valid @RequestBody AvailabilityRequest request) {
		return menuService.setAvailability(seller, id, request.available());
	}

	/** Arquiva o produto: some do cardápio, mas continua nos pedidos antigos. */
	@DeleteMapping("/products/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void archiveProduct(@AuthenticationPrincipal User seller, @PathVariable Long id) {
		menuService.archiveProduct(seller, id);
	}

}
