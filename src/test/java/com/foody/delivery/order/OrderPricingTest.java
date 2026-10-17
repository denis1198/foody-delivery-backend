package com.foody.delivery.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrderPricingTest {

	private static final BigDecimal COMMISSION = new BigDecimal("12");
	private static final BigDecimal PAYMENT_FEE = new BigDecimal("3.2");
	private static final BigDecimal TAX = new BigDecimal("6");

	@Test
	void calculatesMarketplaceFinancials() {
		var pricing = OrderPricing.calculate(
				List.of(line("50.00", "15.00", 2), line("8.00", "3.00", 1)),
				new BigDecimal("5.99"), PaymentMethod.PIX, COMMISSION, PAYMENT_FEE, TAX);

		assertThat(pricing.subtotal()).isEqualByComparingTo("108.00");
		assertThat(pricing.deliveryFee()).isEqualByComparingTo("5.99");
		assertThat(pricing.total()).isEqualByComparingTo("113.99");
		assertThat(pricing.commission()).isEqualByComparingTo("12.96");
		assertThat(pricing.paymentFee()).isEqualByComparingTo("3.46"); // 3,456 arredondado
		assertThat(pricing.net()).isEqualByComparingTo("91.58");
		assertThat(pricing.cost()).isEqualByComparingTo("33.00");
		assertThat(pricing.tax()).isEqualByComparingTo("6.48");
		assertThat(pricing.profit()).isEqualByComparingTo("52.10");
	}

	@Test
	void cashPaymentHasNoProcessingFee() {
		var pricing = OrderPricing.calculate(List.of(line("30.00", "12.00", 1)), BigDecimal.ZERO,
				PaymentMethod.DINHEIRO, COMMISSION, PAYMENT_FEE, TAX);

		assertThat(pricing.paymentFee()).isEqualByComparingTo("0.00");
		assertThat(pricing.net()).isEqualByComparingTo("26.40");
		assertThat(pricing.profit()).isEqualByComparingTo("12.60");
	}

	@Test
	void deliveryFeeDoesNotAffectRevenueOrProfit() {
		var withoutFee = OrderPricing.calculate(List.of(line("40.00", "10.00", 1)), BigDecimal.ZERO,
				PaymentMethod.CARTAO, COMMISSION, PAYMENT_FEE, TAX);
		var withFee = OrderPricing.calculate(List.of(line("40.00", "10.00", 1)), new BigDecimal("9.90"),
				PaymentMethod.CARTAO, COMMISSION, PAYMENT_FEE, TAX);

		assertThat(withFee.subtotal()).isEqualByComparingTo(withoutFee.subtotal());
		assertThat(withFee.profit()).isEqualByComparingTo(withoutFee.profit());
		assertThat(withFee.total()).isEqualByComparingTo("49.90");
	}

	@Test
	void sellingBelowCostGivesNegativeProfit() {
		var pricing = OrderPricing.calculate(List.of(line("10.00", "9.50", 1)), BigDecimal.ZERO, PaymentMethod.PIX,
				COMMISSION, PAYMENT_FEE, TAX);

		assertThat(pricing.profit()).isNegative();
	}

	private static OrderPricing.Line line(String price, String cost, int quantity) {
		return new OrderPricing.Line(new BigDecimal(price), new BigDecimal(cost), quantity);
	}

}
