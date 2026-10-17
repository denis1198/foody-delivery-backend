package com.foody.delivery.restaurant;

import java.math.BigDecimal;
import java.time.Instant;

import com.foody.delivery.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurants")
public class Restaurant {

	public static final BigDecimal DEFAULT_COMMISSION_PERCENT = new BigDecimal("12.00");
	public static final BigDecimal DEFAULT_PAYMENT_FEE_PERCENT = new BigDecimal("3.20");
	public static final BigDecimal DEFAULT_TAX_PERCENT = new BigDecimal("6.00");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", unique = true)
	private User owner;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 60)
	private String category;

	@Column(length = 500)
	private String description;

	@Column(nullable = false, length = 16)
	private String logoEmoji = "🍽️";

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal deliveryFee = new BigDecimal("5.99");

	@Column(nullable = false)
	private int prepTimeMin = 30;

	@Column(nullable = false)
	private int prepTimeMax = 45;

	@Column(nullable = false)
	private boolean open = true;

	/** Comissão cobrada pela plataforma sobre o valor dos itens. */
	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal commissionPercent = DEFAULT_COMMISSION_PERCENT;

	/** Taxa de processamento do pagamento online. */
	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal paymentFeePercent = DEFAULT_PAYMENT_FEE_PERCENT;

	/** Imposto sobre a venda (ex.: Simples Nacional). */
	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal taxPercent = DEFAULT_TAX_PERCENT;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Restaurant() {
	}

	public Restaurant(User owner, String name, String category) {
		this.owner = owner;
		this.name = name;
		this.category = category;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void updateProfile(String name, String category, String description, String logoEmoji,
			BigDecimal deliveryFee, int prepTimeMin, int prepTimeMax) {
		this.name = name;
		this.category = category;
		this.description = description;
		this.logoEmoji = logoEmoji;
		this.deliveryFee = deliveryFee;
		this.prepTimeMin = prepTimeMin;
		this.prepTimeMax = prepTimeMax;
	}

	public void updateFees(BigDecimal commissionPercent, BigDecimal paymentFeePercent, BigDecimal taxPercent) {
		this.commissionPercent = commissionPercent;
		this.paymentFeePercent = paymentFeePercent;
		this.taxPercent = taxPercent;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public boolean isOwnedBy(User user) {
		return user != null && owner.getId().equals(user.getId());
	}

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public String getLogoEmoji() {
		return logoEmoji;
	}

	public BigDecimal getDeliveryFee() {
		return deliveryFee;
	}

	public int getPrepTimeMin() {
		return prepTimeMin;
	}

	public int getPrepTimeMax() {
		return prepTimeMax;
	}

	public boolean isOpen() {
		return open;
	}

	public BigDecimal getCommissionPercent() {
		return commissionPercent;
	}

	public BigDecimal getPaymentFeePercent() {
		return paymentFeePercent;
	}

	public BigDecimal getTaxPercent() {
		return taxPercent;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
