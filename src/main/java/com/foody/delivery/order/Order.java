package com.foody.delivery.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.foody.delivery.restaurant.Restaurant;
import com.foody.delivery.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders", indexes = @Index(name = "idx_orders_restaurant_created", columnList = "restaurant_id, createdAt"))
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(columnDefinition = "integer") // no SQLite, só INTEGER PRIMARY KEY vira autoincremento (rowid)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id")
	private User customer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id")
	private Restaurant restaurant;

	@Embedded
	private DeliveryAddress deliveryAddress;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<OrderItem> items = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentMethod paymentMethod;

	@Column(length = 300)
	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrderStatus status;

	// Snapshot financeiro calculado na criação (ver OrderPricing).
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal deliveryFee;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal commissionAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal paymentFeeAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal taxAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal costAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal netAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal profitAmount;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Order() {
	}

	public Order(User customer, Restaurant restaurant, DeliveryAddress deliveryAddress, List<OrderItem> items,
			PaymentMethod paymentMethod, String notes) {
		this.customer = customer;
		this.restaurant = restaurant;
		this.deliveryAddress = deliveryAddress;
		this.paymentMethod = paymentMethod;
		this.notes = notes;
		this.status = OrderStatus.RECEBIDO;
		items.forEach(this::addItem);
		applyPricing(OrderPricing.calculate(this.items.stream().map(OrderItem::toPricingLine).toList(),
				restaurant.getDeliveryFee(), paymentMethod, restaurant.getCommissionPercent(),
				restaurant.getPaymentFeePercent(), restaurant.getTaxPercent()));
	}

	private void addItem(OrderItem item) {
		item.setOrder(this);
		items.add(item);
	}

	private void applyPricing(OrderPricing pricing) {
		subtotal = pricing.subtotal();
		deliveryFee = pricing.deliveryFee();
		total = pricing.total();
		commissionAmount = pricing.commission();
		paymentFeeAmount = pricing.paymentFee();
		taxAmount = pricing.tax();
		costAmount = pricing.cost();
		netAmount = pricing.net();
		profitAmount = pricing.profit();
	}

	public void changeStatus(OrderStatus newStatus) {
		if (!status.canTransitionTo(newStatus)) {
			throw new InvalidStatusTransitionException(status, newStatus);
		}
		this.status = newStatus;
	}

	/** Define a data do pedido. Usado apenas para importar/gerar histórico (seed de demonstração). */
	public void placedAt(Instant createdAt, Instant updatedAt) {
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public boolean belongsTo(User user) {
		return user != null && customer.getId().equals(user.getId());
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (updatedAt == null) {
			updatedAt = createdAt;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getCustomer() {
		return customer;
	}

	public Restaurant getRestaurant() {
		return restaurant;
	}

	public DeliveryAddress getDeliveryAddress() {
		return deliveryAddress;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public String getNotes() {
		return notes;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getDeliveryFee() {
		return deliveryFee;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public BigDecimal getCommissionAmount() {
		return commissionAmount;
	}

	public BigDecimal getPaymentFeeAmount() {
		return paymentFeeAmount;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public BigDecimal getCostAmount() {
		return costAmount;
	}

	public BigDecimal getNetAmount() {
		return netAmount;
	}

	public BigDecimal getProfitAmount() {
		return profitAmount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
