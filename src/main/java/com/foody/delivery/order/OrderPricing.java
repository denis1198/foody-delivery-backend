package com.foody.delivery.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Valores financeiros de um pedido no modelo de marketplace de delivery.
 *
 * <ul>
 * <li><b>Faturamento (subtotal)</b> = Σ preço × quantidade dos itens (sem a taxa de entrega)</li>
 * <li><b>Comissão</b> = faturamento × % de comissão da plataforma</li>
 * <li><b>Taxa de pagamento</b> = faturamento × % da taxa, só para pagamento online</li>
 * <li><b>Líquido</b> = faturamento − comissão − taxa de pagamento</li>
 * <li><b>CMV</b> = Σ custo × quantidade dos itens</li>
 * <li><b>Imposto</b> = faturamento × % de imposto</li>
 * <li><b>Lucro bruto</b> = líquido − CMV − imposto</li>
 * </ul>
 *
 * A taxa de entrega é repassada ao entregador, por isso fica fora do faturamento e do lucro.
 */
public record OrderPricing(
		BigDecimal subtotal,
		BigDecimal deliveryFee,
		BigDecimal total,
		BigDecimal commission,
		BigDecimal paymentFee,
		BigDecimal tax,
		BigDecimal cost,
		BigDecimal net,
		BigDecimal profit) {

	public record Line(BigDecimal unitPrice, BigDecimal unitCost, int quantity) {
	}

	public static OrderPricing calculate(List<Line> lines, BigDecimal deliveryFee, PaymentMethod paymentMethod,
			BigDecimal commissionPercent, BigDecimal paymentFeePercent, BigDecimal taxPercent) {
		BigDecimal subtotal = money(lines.stream()
				.map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add));
		BigDecimal cost = money(lines.stream()
				.map(l -> l.unitCost().multiply(BigDecimal.valueOf(l.quantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add));
		BigDecimal commission = percent(subtotal, commissionPercent);
		BigDecimal paymentFee = paymentMethod.isOnline() ? percent(subtotal, paymentFeePercent) : money(BigDecimal.ZERO);
		BigDecimal tax = percent(subtotal, taxPercent);
		BigDecimal net = subtotal.subtract(commission).subtract(paymentFee);
		BigDecimal profit = net.subtract(cost).subtract(tax);
		BigDecimal fee = money(deliveryFee);
		return new OrderPricing(subtotal, fee, subtotal.add(fee), commission, paymentFee, tax, cost, net, profit);
	}

	private static BigDecimal percent(BigDecimal amount, BigDecimal percent) {
		return amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

}
