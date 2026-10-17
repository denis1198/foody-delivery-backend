package com.foody.delivery.dashboard;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.foody.delivery.common.BusinessTime;
import com.foody.delivery.menu.MenuCategory;
import com.foody.delivery.menu.Product;
import com.foody.delivery.order.DeliveryAddress;
import com.foody.delivery.order.Order;
import com.foody.delivery.order.OrderItem;
import com.foody.delivery.order.OrderStatus;
import com.foody.delivery.order.PaymentMethod;
import com.foody.delivery.restaurant.Restaurant;
import com.foody.delivery.security.JwtService;
import com.foody.delivery.user.Role;
import com.foody.delivery.user.User;

import jakarta.persistence.EntityManager;

/**
 * Dashboard sobre pedidos com datas controladas. Semana de 05/01/2026 (segunda) a 11/01/2026 (domingo),
 * percentuais padrão do restaurante: comissão 12%, pagamento 3,2%, imposto 6%.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {

	private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private EntityManager em;

	@Autowired
	private TransactionTemplate transaction;

	@Autowired
	private JwtService jwtService;

	private String sellerToken;
	private String customerToken;
	private User customer;
	private Restaurant restaurant;
	private Product burger;

	@BeforeEach
	void seedWeek() {
		transaction.executeWithoutResult(tx -> {
			User seller = persist(new User("Dono", unique("dono"), "x", Role.SELLER));
			customer = persist(new User("Cliente", unique("cliente"), "x", Role.CUSTOMER));
			restaurant = persist(new Restaurant(seller, "Teste", "Lanches"));
			MenuCategory category = persist(new MenuCategory(restaurant, "Tudo", 0));
			burger = persist(new Product(restaurant, category, "Burger", null, new BigDecimal("50.00"),
					new BigDecimal("20.00"), null, null, true, 0));
			Product juice = persist(new Product(restaurant, category, "Suco", null, new BigDecimal("10.00"),
					new BigDecimal("4.00"), null, null, true, 1));

			// Semana analisada
			order(customer, restaurant, MONDAY, "12:30", PaymentMethod.PIX, null, item(burger, 1));
			order(customer, restaurant, MONDAY.plusDays(4), "20:10", PaymentMethod.CARTAO, null,
					item(burger, 2), item(juice, 1));
			order(customer, restaurant, MONDAY.plusDays(4), "20:40", PaymentMethod.DINHEIRO, null, item(juice, 3));
			order(customer, restaurant, MONDAY.plusDays(5), "19:05", PaymentMethod.PIX, OrderStatus.CANCELADO,
					item(burger, 1));
			// Semana anterior
			order(customer, restaurant, MONDAY.minusDays(6), "13:00", PaymentMethod.PIX, null, item(burger, 1));

			sellerToken = jwtService.generateToken(seller);
			customerToken = jwtService.generateToken(customer);
		});
	}

	@Test
	void aggregatesFinancialsAndPatterns() throws Exception {
		mvc.perform(as(sellerToken, get("/api/seller/dashboard").param("from", "2026-01-05").param("to", "2026-01-11")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period.days").value(7))
				// Faturamento 50 + 110 + 30 (cancelado fora)
				.andExpect(jsonPath("$.summary.revenue").value(190.00))
				// Líquido: 42,40 + 93,28 + 26,40 (dinheiro não paga taxa de pagamento)
				.andExpect(jsonPath("$.summary.net").value(162.08))
				// Lucro: 19,40 + 42,68 + 12,60
				.andExpect(jsonPath("$.summary.profit").value(74.68))
				.andExpect(jsonPath("$.summary.margin").value(39.31))
				.andExpect(jsonPath("$.summary.salesCount").value(3))
				.andExpect(jsonPath("$.summary.itemsSold").value(7))
				.andExpect(jsonPath("$.summary.averageTicket").value(63.33))
				.andExpect(jsonPath("$.summary.cancelledCount").value(1))
				.andExpect(jsonPath("$.summary.cancelledRevenue").value(50.00))
				.andExpect(jsonPath("$.summary.cost").value(76.00))
				.andExpect(jsonPath("$.previous.revenue").value(50.00))
				.andExpect(jsonPath("$.previous.salesCount").value(1))
				// Picos: dois pedidos às 20h
				.andExpect(jsonPath("$.ordersByHour[20].orders").value(2))
				.andExpect(jsonPath("$.ordersByHour[12].orders").value(1))
				// Sexta (5) teve 2 pedidos; terça a quinta, nenhum
				.andExpect(jsonPath("$.weekdays[4].label").value("Sex"))
				.andExpect(jsonPath("$.weekdays[4].avgOrders").value(2.00))
				.andExpect(jsonPath("$.weakestDays[*].day", contains("2026-01-06", "2026-01-07", "2026-01-08")))
				.andExpect(jsonPath("$.revenueByDay.length()").value(7))
				.andExpect(jsonPath("$.revenueByDay[4].revenue").value(140.00))
				// Burger: 3 unidades vendidas (a cancelada não conta), 150 de 190 = 78,95%
				.andExpect(jsonPath("$.topProducts[0].productName").value("Burger"))
				.andExpect(jsonPath("$.topProducts[0].unitsSold").value(3))
				.andExpect(jsonPath("$.topProducts[0].share").value(78.95))
				.andExpect(jsonPath("$.statusBreakdown[?(@.status == 'CANCELADO')].count", contains(1)));
	}

	@Test
	void todayIsComparedWithYesterdayUntilTheSameTime() throws Exception {
		// Pedidos a ±30 min do horário atual precisam cair em "ontem" e "hoje": evita a virada do dia.
		int hour = BusinessTime.local(Instant.now()).getHour();
		Assumptions.assumeTrue(hour >= 1 && hour < 23, "perto da meia-noite");

		Instant now = Instant.now();
		Instant yesterday = now.minus(1, ChronoUnit.DAYS);
		transaction.executeWithoutResult(tx -> {
			orderAt(now.minus(10, ChronoUnit.MINUTES), item(burger, 1));
			orderAt(yesterday.minus(30, ChronoUnit.MINUTES), item(burger, 2)); // antes do horário atual: entra
			orderAt(yesterday.plus(30, ChronoUnit.MINUTES), item(burger, 5)); // depois do horário atual: fora
		});

		String today = BusinessTime.today().toString();
		mvc.perform(as(sellerToken, get("/api/seller/dashboard").param("from", today).param("to", today)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period.comparedUntilNow").value(true))
				.andExpect(jsonPath("$.summary.revenue").value(50.00))
				.andExpect(jsonPath("$.previous.salesCount").value(1))
				.andExpect(jsonPath("$.previous.revenue").value(100.00));

		// Período já fechado compara com o anterior inteiro.
		mvc.perform(as(sellerToken, get("/api/seller/dashboard").param("from", "2026-01-05").param("to", "2026-01-11")))
				.andExpect(jsonPath("$.period.comparedUntilNow").value(false));
	}

	@Test
	void validatesPeriodAndRole() throws Exception {
		mvc.perform(as(sellerToken, get("/api/seller/dashboard").param("from", "2026-01-11").param("to", "2026-01-05")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.from").exists());
		mvc.perform(as(customerToken, get("/api/seller/dashboard"))).andExpect(status().isForbidden());
	}

	private void order(User customer, Restaurant restaurant, LocalDate day, String time, PaymentMethod payment,
			OrderStatus finalStatus, OrderItem... items) {
		Order order = new Order(customer, restaurant, new DeliveryAddress("Rua A", "1", null, "Centro", "SP", "0"),
				List.of(items), payment, null);
		if (finalStatus == OrderStatus.CANCELADO) {
			order.changeStatus(OrderStatus.CANCELADO);
		} else {
			order.changeStatus(OrderStatus.EM_PREPARO);
			order.changeStatus(OrderStatus.SAIU_PARA_ENTREGA);
			order.changeStatus(OrderStatus.ENTREGUE);
		}
		var placed = day.atTime(LocalTime.parse(time)).atZone(BusinessTime.ZONE).toInstant();
		order.placedAt(placed, placed);
		em.persist(order);
	}

	private void orderAt(Instant placed, OrderItem... items) {
		Order order = new Order(customer, restaurant, new DeliveryAddress("Rua A", "1", null, "Centro", "SP", "0"),
				List.of(items), PaymentMethod.PIX, null);
		order.placedAt(placed, placed);
		em.persist(order);
	}

	private static OrderItem item(Product product, int quantity) {
		return new OrderItem(product, quantity, null);
	}

	private <T> T persist(T entity) {
		em.persist(entity);
		return entity;
	}

	private static String unique(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@foody.com";
	}

	private static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

}
