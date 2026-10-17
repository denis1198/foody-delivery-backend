package com.foody.delivery.order;

import java.math.BigDecimal;

import com.foody.delivery.menu.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Item do pedido. Nome, preço e custo são copiados do produto no momento da compra, para que
 * mudanças posteriores no cardápio não alterem pedidos já feitos nem as métricas passadas.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(columnDefinition = "integer") // no SQLite, só INTEGER PRIMARY KEY vira autoincremento (rowid)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false)
	private String productName;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal unitCost;

	@Column(length = 300)
	private String notes;

	protected OrderItem() {
	}

	public OrderItem(Product product, int quantity, String notes) {
		this(product, product.getName(), quantity, product.getPrice(), product.getCostPrice(), notes);
	}

	public OrderItem(Product product, String productName, int quantity, BigDecimal unitPrice, BigDecimal unitCost,
			String notes) {
		this.product = product;
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.unitCost = unitCost;
		this.notes = notes;
	}

	void setOrder(Order order) {
		this.order = order;
	}

	OrderPricing.Line toPricingLine() {
		return new OrderPricing.Line(unitPrice, unitCost, quantity);
	}

	public BigDecimal getSubtotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public BigDecimal getCostTotal() {
		return unitCost.multiply(BigDecimal.valueOf(quantity));
	}

	public Long getId() {
		return id;
	}

	/** Id do produto de origem, sem inicializar o proxy. */
	public Long getProductId() {
		return product == null ? null : product.getId();
	}

	public String getProductName() {
		return productName;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public BigDecimal getUnitCost() {
		return unitCost;
	}

	public String getNotes() {
		return notes;
	}

}
