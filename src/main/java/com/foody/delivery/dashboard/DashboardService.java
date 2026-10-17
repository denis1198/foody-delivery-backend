package com.foody.delivery.dashboard;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foody.delivery.common.BadRequestException;
import com.foody.delivery.common.BusinessTime;
import com.foody.delivery.dashboard.DashboardResponse.DayPoint;
import com.foody.delivery.dashboard.DashboardResponse.HourPoint;
import com.foody.delivery.dashboard.DashboardResponse.Period;
import com.foody.delivery.dashboard.DashboardResponse.StatusCount;
import com.foody.delivery.dashboard.DashboardResponse.Summary;
import com.foody.delivery.dashboard.DashboardResponse.TopProduct;
import com.foody.delivery.dashboard.DashboardResponse.WeekdayPoint;
import com.foody.delivery.order.Order;
import com.foody.delivery.order.OrderItem;
import com.foody.delivery.order.OrderRepository;
import com.foody.delivery.order.OrderStatus;
import com.foody.delivery.restaurant.Restaurant;
import com.foody.delivery.restaurant.RestaurantService;
import com.foody.delivery.user.User;

/**
 * Métricas do restaurante num intervalo de dias (inclusive), agregadas a partir do snapshot financeiro
 * gravado em cada pedido. Dias, horas e dias da semana são calculados no fuso de Brasília.
 */
@Service
public class DashboardService {

	private static final int MAX_DAYS = 366;
	private static final int TOP_PRODUCTS = 15;
	private static final int WEAKEST_DAYS = 3;
	private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

	private final OrderRepository orders;
	private final RestaurantService restaurantService;

	public DashboardService(OrderRepository orders, RestaurantService restaurantService) {
		this.orders = orders;
		this.restaurantService = restaurantService;
	}

	@Transactional(readOnly = true)
	public DashboardResponse build(User seller, LocalDate from, LocalDate to) {
		LocalDate today = BusinessTime.today();
		LocalDate end = to != null ? to : today;
		LocalDate start = from != null ? from : end.minusDays(29);
		if (start.isAfter(end)) {
			throw new BadRequestException("from", "A data inicial deve ser anterior ou igual à final.");
		}
		int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
		if (days > MAX_DAYS) {
			throw new BadRequestException("from", "O período pode ter no máximo " + MAX_DAYS + " dias.");
		}

		Restaurant restaurant = restaurantService.ofSeller(seller);
		List<Order> current = load(restaurant, start, end);
		// Se o período inclui hoje, ele ainda está pela metade: compara com o anterior só até o mesmo horário
		// (ex.: hoje até 10h × ontem até 10h), senão a variação sairia sempre negativa durante o dia.
		boolean inProgress = !end.isBefore(today);
		Instant previousStart = BusinessTime.startOf(start.minusDays(days));
		Instant previousEnd = inProgress
				? Instant.now().minus(days, ChronoUnit.DAYS)
				: BusinessTime.startOf(start);
		List<Order> previous = orders.findAllByRestaurantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
				restaurant.getId(), previousStart, previousEnd);
		List<Order> sales = current.stream().filter(DashboardService::isSale).toList();

		// Dias futuros não existem ainda; e o dia de hoje, incompleto, não entra nas comparações entre dias.
		LocalDate lastDay = end.isAfter(today) ? today : end;
		List<LocalDate> chartDays = start.datesUntil(lastDay.plusDays(1)).toList();
		List<LocalDate> completeDays = chartDays.stream()
				.filter(d -> !d.equals(today) || chartDays.size() == 1)
				.toList();

		Map<LocalDate, DayPoint> byDay = byDay(sales, chartDays);
		return new DashboardResponse(
				new Period(start, end, days, inProgress),
				summarize(current),
				summarize(previous),
				List.copyOf(byDay.values()),
				byHour(sales),
				byWeekday(byDay, completeDays),
				weakestDays(byDay, completeDays),
				topProducts(sales),
				statusBreakdown(current));
	}

	private List<Order> load(Restaurant restaurant, LocalDate from, LocalDate to) {
		return orders.findAllByRestaurantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(restaurant.getId(),
				BusinessTime.startOf(from), BusinessTime.startOf(to.plusDays(1)));
	}

	private static boolean isSale(Order order) {
		return order.getStatus() != OrderStatus.CANCELADO;
	}

	static Summary summarize(List<Order> orders) {
		List<Order> sales = orders.stream().filter(DashboardService::isSale).toList();
		List<Order> cancelled = orders.stream().filter(o -> !isSale(o)).toList();

		BigDecimal revenue = sum(sales, Order::getSubtotal);
		BigDecimal profit = sum(sales, Order::getProfitAmount);
		BigDecimal cost = sum(sales, Order::getCostAmount);
		long items = sales.stream().flatMap(o -> o.getItems().stream()).mapToLong(OrderItem::getQuantity).sum();

		return new Summary(
				revenue,
				sum(sales, Order::getNetAmount),
				profit,
				percent(profit, revenue),
				sales.size(),
				items,
				sales.isEmpty() ? money(ZERO) : revenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP),
				cancelled.size(),
				sum(cancelled, Order::getSubtotal),
				sum(sales, Order::getCommissionAmount),
				sum(sales, Order::getPaymentFeeAmount),
				sum(sales, Order::getTaxAmount),
				cost,
				sum(sales, Order::getDeliveryFee),
				percent(profit, cost));
	}

	/** Uma entrada por dia do gráfico, inclusive dias sem venda. */
	private static Map<LocalDate, DayPoint> byDay(List<Order> sales, List<LocalDate> days) {
		Map<LocalDate, List<Order>> grouped = sales.stream()
				.collect(Collectors.groupingBy(o -> BusinessTime.local(o.getCreatedAt()).toLocalDate()));
		Map<LocalDate, DayPoint> result = new LinkedHashMap<>();
		for (LocalDate day : days) {
			List<Order> list = grouped.getOrDefault(day, List.of());
			result.put(day, new DayPoint(day, list.size(), sum(list, Order::getSubtotal),
					sum(list, Order::getNetAmount), sum(list, Order::getProfitAmount)));
		}
		return result;
	}

	private static List<HourPoint> byHour(List<Order> sales) {
		long[] counts = new long[24];
		BigDecimal[] revenue = new BigDecimal[24];
		Arrays.fill(revenue, ZERO);
		for (Order o : sales) {
			int hour = BusinessTime.local(o.getCreatedAt()).getHour();
			counts[hour]++;
			revenue[hour] = revenue[hour].add(o.getSubtotal());
		}
		List<HourPoint> result = new ArrayList<>(24);
		for (int h = 0; h < 24; h++) {
			result.add(new HourPoint(h, counts[h], money(revenue[h])));
		}
		return result;
	}

	/** Média por ocorrência de cada dia da semana no período: revela os dias fracos da operação. */
	private static List<WeekdayPoint> byWeekday(Map<LocalDate, DayPoint> byDay, List<LocalDate> days) {
		Map<DayOfWeek, int[]> occurrences = new EnumMap<>(DayOfWeek.class);
		Map<DayOfWeek, long[]> orderCounts = new EnumMap<>(DayOfWeek.class);
		Map<DayOfWeek, BigDecimal> revenue = new EnumMap<>(DayOfWeek.class);
		for (DayOfWeek dow : DayOfWeek.values()) {
			occurrences.put(dow, new int[1]);
			orderCounts.put(dow, new long[1]);
			revenue.put(dow, ZERO);
		}
		for (LocalDate day : days) {
			DayPoint point = byDay.get(day);
			DayOfWeek dow = day.getDayOfWeek();
			occurrences.get(dow)[0]++;
			orderCounts.get(dow)[0] += point.orders();
			revenue.merge(dow, point.revenue(), BigDecimal::add);
		}
		List<WeekdayPoint> result = new ArrayList<>(7);
		for (DayOfWeek dow : DayOfWeek.values()) {
			int n = occurrences.get(dow)[0];
			long count = orderCounts.get(dow)[0];
			BigDecimal rev = money(revenue.get(dow));
			result.add(new WeekdayPoint(dow.getValue(), label(dow), n, count, rev,
					n == 0 ? money(ZERO) : BigDecimal.valueOf(count).divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP),
					n == 0 ? money(ZERO) : rev.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP)));
		}
		return result;
	}

	private static List<DayPoint> weakestDays(Map<LocalDate, DayPoint> byDay, List<LocalDate> days) {
		if (days.size() < 2) {
			return List.of();
		}
		return days.stream()
				.map(byDay::get)
				.sorted(Comparator.comparingLong(DayPoint::orders).thenComparing(DayPoint::revenue)
						.thenComparing(DayPoint::day))
				.limit(WEAKEST_DAYS)
				.toList();
	}

	/**
	 * Lucro por produto: receita do item − custo do item − a fatia proporcional de comissão, taxa de
	 * pagamento e imposto do pedido. Assim a soma dos produtos bate com o lucro total.
	 */
	private static List<TopProduct> topProducts(List<Order> sales) {
		record Acc(Long productId, String name, long[] units, BigDecimal[] revenue, BigDecimal[] cost,
				BigDecimal[] profit) {
		}
		Map<String, Acc> byProduct = new LinkedHashMap<>();
		BigDecimal totalRevenue = sum(sales, Order::getSubtotal);

		for (Order order : sales) {
			BigDecimal deductions = order.getCommissionAmount().add(order.getPaymentFeeAmount())
					.add(order.getTaxAmount());
			BigDecimal deductionRate = order.getSubtotal().signum() == 0 ? ZERO
					: deductions.divide(order.getSubtotal(), 10, RoundingMode.HALF_UP);
			for (OrderItem item : order.getItems()) {
				String key = item.getProductId() != null ? "id:" + item.getProductId() : "name:" + item.getProductName();
				Acc acc = byProduct.computeIfAbsent(key, k -> new Acc(item.getProductId(), item.getProductName(),
						new long[1], new BigDecimal[] { ZERO }, new BigDecimal[] { ZERO }, new BigDecimal[] { ZERO }));
				BigDecimal itemRevenue = item.getSubtotal();
				BigDecimal itemCost = item.getCostTotal();
				acc.units()[0] += item.getQuantity();
				acc.revenue()[0] = acc.revenue()[0].add(itemRevenue);
				acc.cost()[0] = acc.cost()[0].add(itemCost);
				acc.profit()[0] = acc.profit()[0]
						.add(itemRevenue.subtract(itemCost).subtract(itemRevenue.multiply(deductionRate)));
			}
		}

		return byProduct.values().stream()
				.sorted(Comparator.comparing((Acc a) -> a.revenue()[0]).reversed())
				.limit(TOP_PRODUCTS)
				.map(a -> {
					BigDecimal rev = money(a.revenue()[0]);
					BigDecimal units = BigDecimal.valueOf(a.units()[0]);
					BigDecimal profit = money(a.profit()[0]);
					return new TopProduct(a.productId(), a.name(), a.units()[0], rev, percent(rev, totalRevenue),
							rev.divide(units, 2, RoundingMode.HALF_UP),
							a.cost()[0].divide(units, 2, RoundingMode.HALF_UP), profit, percent(profit, rev));
				})
				.toList();
	}

	private static List<StatusCount> statusBreakdown(List<Order> orders) {
		Map<OrderStatus, Long> counts = orders.stream()
				.collect(Collectors.groupingBy(Order::getStatus, () -> new EnumMap<>(OrderStatus.class),
						Collectors.counting()));
		return Arrays.stream(OrderStatus.values())
				.map(s -> new StatusCount(s, counts.getOrDefault(s, 0L)))
				.toList();
	}

	private static String label(DayOfWeek dow) {
		String name = dow.getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static BigDecimal sum(List<Order> orders, Function<Order, BigDecimal> field) {
		return money(orders.stream().map(field).reduce(ZERO, BigDecimal::add));
	}

	/** {@code part / whole × 100} com 2 casas; zero quando não há base. */
	private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
		return whole.signum() == 0 ? money(ZERO)
				: part.multiply(BigDecimal.valueOf(100)).divide(whole, 2, RoundingMode.HALF_UP);
	}

	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

}
