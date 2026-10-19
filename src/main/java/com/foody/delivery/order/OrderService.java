package com.foody.delivery.order;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foody.delivery.common.BadRequestException;
import com.foody.delivery.common.BusinessTime;
import com.foody.delivery.common.ConflictException;
import com.foody.delivery.common.NotFoundException;
import com.foody.delivery.menu.Product;
import com.foody.delivery.menu.ProductRepository;
import com.foody.delivery.order.dto.AddressRequest;
import com.foody.delivery.order.dto.CreateOrderRequest;
import com.foody.delivery.order.dto.OrderResponse;
import com.foody.delivery.restaurant.Restaurant;
import com.foody.delivery.restaurant.RestaurantService;
import com.foody.delivery.user.User;

@Service
public class OrderService {

	private static final List<OrderStatus> ACTIVE_STATUSES = Arrays.stream(OrderStatus.values())
			.filter(s -> !s.isFinal())
			.toList();

	private static final int MAX_HISTORY_DAYS = 366;

	private final OrderRepository orders;
	private final ProductRepository products;
	private final RestaurantService restaurantService;

	public OrderService(OrderRepository orders, ProductRepository products, RestaurantService restaurantService) {
		this.orders = orders;
		this.products = products;
		this.restaurantService = restaurantService;
	}

	// ---- Cliente -------------------------------------------------------------------------------

	@Transactional
	public OrderResponse create(CreateOrderRequest request, User customer) {
		Restaurant restaurant = restaurantService.get(request.restaurantId());
		if (!restaurant.isOpen()) {
			throw new ConflictException("O restaurante " + restaurant.getName() + " está fechado no momento.");
		}

		Map<Long, Product> catalog = products
				.findAllByIdIn(request.items().stream().map(i -> i.productId()).distinct().toList()).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));

		var items = request.items().stream().map(i -> {
			Product product = catalog.get(i.productId());
			if (product == null || !product.getRestaurant().getId().equals(restaurant.getId())) {
				throw new ConflictException("Produto " + i.productId() + " não pertence a este restaurante.");
			}
			if (!product.isOrderable()) {
				throw new ConflictException(product.getName() + " está indisponível no momento.");
			}
			return new OrderItem(product, i.quantity(), blankToNull(i.notes()));
		}).toList();

		var order = new Order(customer, restaurant, toAddress(request.deliveryAddress()), items,
				request.paymentMethod(), blankToNull(request.notes()));
		return OrderResponse.forCustomer(orders.save(order));
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> listForCustomer(User customer) {
		return orders.findAllByCustomerIdOrderByCreatedAtDescIdDesc(customer.getId()).stream()
				.map(OrderResponse::forCustomer)
				.toList();
	}

	@Transactional(readOnly = true)
	public OrderResponse getForCustomer(Long id, User customer) {
		return OrderResponse.forCustomer(findForCustomer(id, customer));
	}

	@Transactional
	public OrderResponse cancelByCustomer(Long id, User customer) {
		Order order = findForCustomer(id, customer);
		if (order.getStatus() != OrderStatus.RECEBIDO) {
			throw new ConflictException(order.getStatus().isFinal()
					? "Este pedido já foi finalizado."
					: "O restaurante já começou a preparar o pedido; ele não pode mais ser cancelado pelo app.");
		}
		order.changeStatus(OrderStatus.CANCELADO);
		return OrderResponse.forCustomer(orders.saveAndFlush(order));
	}

	// ---- Vendedor ------------------------------------------------------------------------------

	/** Pedidos em andamento do restaurante mais os finalizados hoje, para o quadro de pedidos. */
	@Transactional(readOnly = true)
	public List<OrderResponse> board(User seller) {
		Restaurant restaurant = restaurantService.ofSeller(seller);
		return orders.findBoard(restaurant.getId(), ACTIVE_STATUSES, BusinessTime.startOf(BusinessTime.today()))
				.stream()
				.map(OrderResponse::forSeller)
				.toList();
	}

	/**
	 * Todos os pedidos do restaurante criados entre {@code from} e {@code to} (inclusivos, dias de Brasília),
	 * em qualquer status. Sem {@code from}, vale só o dia {@code to}; sem {@code to}, até hoje.
	 */
	@Transactional(readOnly = true)
	public List<OrderResponse> history(User seller, LocalDate from, LocalDate to) {
		LocalDate end = to != null ? to : BusinessTime.today();
		LocalDate start = from != null ? from : end;
		if (start.isAfter(end)) {
			throw new BadRequestException("from", "A data inicial deve ser anterior ou igual à final.");
		}
		if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_HISTORY_DAYS) {
			throw new BadRequestException("from", "O período pode ter no máximo " + MAX_HISTORY_DAYS + " dias.");
		}
		Restaurant restaurant = restaurantService.ofSeller(seller);
		return orders.findHistory(restaurant.getId(), BusinessTime.startOf(start), BusinessTime.startOf(end.plusDays(1)))
				.stream()
				.map(OrderResponse::forSeller)
				.toList();
	}

	@Transactional(readOnly = true)
	public OrderResponse getForSeller(Long id, User seller) {
		return OrderResponse.forSeller(findForSeller(id, seller));
	}

	@Transactional
	public OrderResponse updateStatusBySeller(Long id, OrderStatus newStatus, User seller) {
		Order order = findForSeller(id, seller);
		order.changeStatus(newStatus);
		return OrderResponse.forSeller(orders.saveAndFlush(order));
	}

	// ---- Auxiliares ----------------------------------------------------------------------------

	/** Pedido de outra pessoa responde 404, para não revelar que o id existe. */
	private Order findForCustomer(Long id, User customer) {
		return orders.findWithDetailsById(id)
				.filter(o -> o.belongsTo(customer))
				.orElseThrow(() -> notFound(id));
	}

	private Order findForSeller(Long id, User seller) {
		return orders.findWithDetailsById(id)
				.filter(o -> o.getRestaurant().isOwnedBy(seller))
				.orElseThrow(() -> notFound(id));
	}

	private static NotFoundException notFound(Long id) {
		return new NotFoundException("Pedido " + id + " não encontrado.");
	}

	private static DeliveryAddress toAddress(AddressRequest a) {
		return new DeliveryAddress(a.street().trim(), a.number().trim(), blankToNull(a.complement()),
				a.neighborhood().trim(), a.city().trim(), a.zipCode().trim());
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
