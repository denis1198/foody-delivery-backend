package com.foody.delivery.menu;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foody.delivery.common.BadRequestException;
import com.foody.delivery.common.ConflictException;
import com.foody.delivery.common.NotFoundException;
import com.foody.delivery.menu.dto.CategoryRequest;
import com.foody.delivery.menu.dto.MenuResponse;
import com.foody.delivery.menu.dto.ProductRequest;
import com.foody.delivery.menu.dto.SellerMenuResponse;
import com.foody.delivery.restaurant.Restaurant;
import com.foody.delivery.restaurant.RestaurantService;
import com.foody.delivery.restaurant.dto.RestaurantResponse;
import com.foody.delivery.user.User;

@Service
public class MenuService {

	private final RestaurantService restaurantService;
	private final MenuCategoryRepository categories;
	private final ProductRepository products;

	public MenuService(RestaurantService restaurantService, MenuCategoryRepository categories,
			ProductRepository products) {
		this.restaurantService = restaurantService;
		this.categories = categories;
		this.products = products;
	}

	/** Cardápio que o cliente vê: só produtos disponíveis, agrupados nas categorias que têm algum. */
	@Transactional(readOnly = true)
	public MenuResponse publicMenu(Long restaurantId) {
		Restaurant restaurant = restaurantService.get(restaurantId);
		Map<Long, List<Product>> byCategory = activeProductsByCategory(restaurant.getId(), true);
		var sections = categories.findAllByRestaurantIdOrderByPositionAscIdAsc(restaurant.getId()).stream()
				.filter(c -> byCategory.containsKey(c.getId()))
				.map(c -> MenuResponse.section(c, byCategory.get(c.getId())))
				.toList();
		return new MenuResponse(RestaurantResponse.from(restaurant), sections);
	}

	@Transactional(readOnly = true)
	public SellerMenuResponse sellerMenu(User seller) {
		Restaurant restaurant = restaurantService.ofSeller(seller);
		Map<Long, List<Product>> byCategory = activeProductsByCategory(restaurant.getId(), false);
		var sections = categories.findAllByRestaurantIdOrderByPositionAscIdAsc(restaurant.getId()).stream()
				.map(c -> SellerMenuResponse.Section.from(c, byCategory.getOrDefault(c.getId(), List.of())))
				.toList();
		return new SellerMenuResponse(SellerMenuResponse.Fees.from(restaurant), sections);
	}

	@Transactional
	public SellerMenuResponse.Section createCategory(User seller, CategoryRequest request) {
		Restaurant restaurant = restaurantService.ofSeller(seller);
		int position = request.position() != null ? request.position()
				: categories.countByRestaurantId(restaurant.getId());
		var category = categories.save(new MenuCategory(restaurant, request.name().trim(), position));
		return SellerMenuResponse.Section.from(category, List.of());
	}

	@Transactional
	public SellerMenuResponse.Section updateCategory(User seller, Long id, CategoryRequest request) {
		Restaurant restaurant = restaurantService.ofSeller(seller);
		MenuCategory category = findCategory(restaurant, id);
		category.rename(request.name().trim());
		if (request.position() != null) {
			category.moveTo(request.position());
		}
		var activeProducts = products.findAllByCategoryId(id).stream().filter(p -> !p.isArchived()).toList();
		return SellerMenuResponse.Section.from(categories.save(category), activeProducts);
	}

	@Transactional
	public void deleteCategory(User seller, Long id) {
		Restaurant restaurant = restaurantService.ofSeller(seller);
		MenuCategory category = findCategory(restaurant, id);
		if (products.existsByCategoryIdAndArchivedFalse(id)) {
			throw new ConflictException("Mova ou exclua os produtos desta categoria antes de removê-la.");
		}
		products.findAllByCategoryId(id).forEach(Product::detachFromCategory);
		categories.delete(category);
	}

	@Transactional
	public SellerMenuResponse.ProductResponse createProduct(User seller, ProductRequest request) {
		Restaurant restaurant = restaurantService.ofSeller(seller);
		MenuCategory category = categoryForProduct(restaurant, request.categoryId());
		int position = products.findAllByCategoryId(category.getId()).size();
		var product = new Product(restaurant, category, request.name().trim(), blankToNull(request.description()),
				request.price(), request.costPrice(), blankToNull(request.emoji()), blankToNull(request.imageUrl()),
				request.available() == null || request.available(), position);
		return SellerMenuResponse.ProductResponse.from(products.save(product));
	}

	@Transactional
	public SellerMenuResponse.ProductResponse updateProduct(User seller, Long id, ProductRequest request) {
		Restaurant restaurant = restaurantService.ofSeller(seller);
		Product product = findProduct(restaurant, id);
		MenuCategory category = categoryForProduct(restaurant, request.categoryId());
		product.update(category, request.name().trim(), blankToNull(request.description()), request.price(),
				request.costPrice(), blankToNull(request.emoji()), blankToNull(request.imageUrl()),
				request.available() == null ? product.isAvailable() : request.available());
		return SellerMenuResponse.ProductResponse.from(products.save(product));
	}

	@Transactional
	public SellerMenuResponse.ProductResponse setAvailability(User seller, Long id, boolean available) {
		Product product = findProduct(restaurantService.ofSeller(seller), id);
		product.setAvailable(available);
		return SellerMenuResponse.ProductResponse.from(products.save(product));
	}

	@Transactional
	public void archiveProduct(User seller, Long id) {
		Product product = findProduct(restaurantService.ofSeller(seller), id);
		product.archive();
		products.save(product);
	}

	private Map<Long, List<Product>> activeProductsByCategory(Long restaurantId, boolean onlyAvailable) {
		return products.findAllByRestaurantIdAndArchivedFalseOrderByPositionAscIdAsc(restaurantId).stream()
				.filter(p -> p.getCategory() != null && (!onlyAvailable || p.isAvailable()))
				.collect(Collectors.groupingBy(p -> p.getCategory().getId()));
	}

	private MenuCategory findCategory(Restaurant restaurant, Long id) {
		return categories.findByIdAndRestaurantId(id, restaurant.getId())
				.orElseThrow(() -> new NotFoundException("Categoria " + id + " não encontrada."));
	}

	private MenuCategory categoryForProduct(Restaurant restaurant, Long categoryId) {
		return categories.findByIdAndRestaurantId(categoryId, restaurant.getId())
				.orElseThrow(() -> new BadRequestException("categoryId", "Categoria inválida."));
	}

	private Product findProduct(Restaurant restaurant, Long id) {
		return products.findByIdAndRestaurantIdAndArchivedFalse(id, restaurant.getId())
				.orElseThrow(() -> new NotFoundException("Produto " + id + " não encontrado."));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
