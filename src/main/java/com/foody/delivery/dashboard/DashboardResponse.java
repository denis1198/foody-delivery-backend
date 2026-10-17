package com.foody.delivery.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.foody.delivery.order.OrderStatus;

public record DashboardResponse(
		Period period,
		Summary summary,
		/** Mesmo intervalo de dias imediatamente anterior, para calcular a variação de cada KPI. */
		Summary previous,
		List<DayPoint> revenueByDay,
		List<HourPoint> ordersByHour,
		List<WeekdayPoint> weekdays,
		List<DayPoint> weakestDays,
		List<TopProduct> topProducts,
		List<StatusCount> statusBreakdown) {

	public record Period(LocalDate from, LocalDate to, int days) {
	}

	/**
	 * Totais do período. Pedidos cancelados ficam fora de todos os valores, exceto
	 * {@code cancelledCount} e {@code cancelledRevenue}.
	 */
	public record Summary(
			BigDecimal revenue,
			BigDecimal net,
			BigDecimal profit,
			BigDecimal margin,
			long salesCount,
			long itemsSold,
			BigDecimal averageTicket,
			long cancelledCount,
			BigDecimal cancelledRevenue,
			BigDecimal commission,
			BigDecimal paymentFees,
			BigDecimal taxes,
			BigDecimal cost,
			BigDecimal deliveryFees,
			BigDecimal roi) {
	}

	public record DayPoint(LocalDate day, long orders, BigDecimal revenue, BigDecimal net, BigDecimal profit) {
	}

	public record HourPoint(int hour, long orders, BigDecimal revenue) {
	}

	/** {@code weekday} segue a ISO-8601: 1 = segunda … 7 = domingo. */
	public record WeekdayPoint(int weekday, String label, int occurrences, long orders, BigDecimal revenue,
			BigDecimal avgOrders, BigDecimal avgRevenue) {
	}

	public record TopProduct(Long productId, String productName, long unitsSold, BigDecimal revenue,
			BigDecimal share, BigDecimal avgPrice, BigDecimal avgCost, BigDecimal profit, BigDecimal margin) {
	}

	public record StatusCount(OrderStatus status, long count) {
	}

}
