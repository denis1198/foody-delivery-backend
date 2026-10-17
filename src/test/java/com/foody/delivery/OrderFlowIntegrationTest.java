package com.foody.delivery;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/** Fluxo ponta a ponta: vendedor monta o cardápio, cliente pede, vendedor atende. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

	private static final String ADDRESS = """
			{"street": "Rua das Flores", "number": "123", "complement": "Apto 4",
			 "neighborhood": "Centro", "city": "São Paulo", "zipCode": "01000-000"}""";

	@Autowired
	private MockMvc mvc;

	private String seller;
	private String customer;
	private long restaurantId;
	private long pizzaId;
	private long sodaId;

	@BeforeEach
	void setUp() throws Exception {
		seller = register("SELLER", ", \"restaurantName\": \"Cantina Teste\", \"restaurantCategory\": \"Pizzaria\"");
		customer = register("CUSTOMER", "");
		restaurantId = JsonPath.<Integer>read(body(as(seller, get("/api/seller/restaurant"))), "$.id");

		int categoryId = JsonPath.read(body(as(seller, json(post("/api/seller/menu/categories"), """
				{"name": "Pizzas"}"""))), "$.id");
		pizzaId = createProduct(categoryId, "Margherita", "50.00", "15.00");
		sodaId = createProduct(categoryId, "Refrigerante", "8.00", "3.00");
	}

	@Test
	void customerOrdersAndSellerFulfills() throws Exception {
		// unitPrice enviado pelo cliente é ignorado: vale o preço do cardápio
		String created = mvc.perform(as(customer, json(post("/api/orders"), """
				{"restaurantId": %d, "paymentMethod": "PIX", "deliveryAddress": %s,
				 "items": [{"productId": %d, "quantity": 2, "notes": "sem cebola", "unitPrice": 0.01},
				           {"productId": %d, "quantity": 1}]}""".formatted(restaurantId, ADDRESS, pizzaId, sodaId))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("RECEBIDO"))
				.andExpect(jsonPath("$.subtotal").value(108.00))
				.andExpect(jsonPath("$.deliveryFee").value(5.99))
				.andExpect(jsonPath("$.total").value(113.99))
				.andExpect(jsonPath("$.items[0].notes").value("sem cebola"))
				.andExpect(jsonPath("$.nextStatuses", contains("CANCELADO")))
				.andExpect(jsonPath("$.financials").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		int id = JsonPath.read(created, "$.id");

		mvc.perform(as(customer, get("/api/orders/mine")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].restaurant.name").value("Cantina Teste"));

		// Vendedor vê o pedido no quadro, com o snapshot financeiro
		mvc.perform(as(seller, get("/api/seller/orders")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
		mvc.perform(as(seller, get("/api/seller/orders/" + id)))
				.andExpect(jsonPath("$.nextStatuses", contains("EM_PREPARO", "CANCELADO")))
				// 108 − 12% comissão (12,96) − 3,2% pagamento (3,46) = 91,58 · lucro = 91,58 − CMV 33 − imposto 6,48
				.andExpect(jsonPath("$.financials.net").value(91.58))
				.andExpect(jsonPath("$.financials.cost").value(33.00))
				.andExpect(jsonPath("$.financials.profit").value(52.10));

		mvc.perform(as(seller, json(patch("/api/seller/orders/" + id + "/status"), """
				{"status": "EM_PREPARO"}""")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EM_PREPARO"));

		// Em preparo: o cliente não pode mais cancelar
		mvc.perform(as(customer, get("/api/orders/" + id)))
				.andExpect(jsonPath("$.status").value("EM_PREPARO"))
				.andExpect(jsonPath("$.nextStatuses", empty()));
		mvc.perform(as(customer, patch("/api/orders/" + id + "/cancel"))).andExpect(status().isConflict());

		for (String next : new String[] { "SAIU_PARA_ENTREGA", "ENTREGUE" }) {
			mvc.perform(as(seller, json(patch("/api/seller/orders/" + id + "/status"), """
					{"status": "%s"}""".formatted(next))))
					.andExpect(status().isOk());
		}
		mvc.perform(as(seller, json(patch("/api/seller/orders/" + id + "/status"), """
				{"status": "CANCELADO"}""")))
				.andExpect(status().isConflict());
	}

	@Test
	void customerCanCancelWhileReceived() throws Exception {
		int id = placeOrder(customer, pizzaId);
		mvc.perform(as(customer, patch("/api/orders/" + id + "/cancel")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"));
	}

	@Test
	void enforcesRolesAndOwnership() throws Exception {
		int id = placeOrder(customer, pizzaId);

		mvc.perform(as(customer, get("/api/seller/orders"))).andExpect(status().isForbidden());
		mvc.perform(as(seller, get("/api/orders/mine"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/restaurants")).andExpect(status().isUnauthorized());

		String otherSeller = register("SELLER", ", \"restaurantName\": \"Outro\", \"restaurantCategory\": \"Lanches\"");
		mvc.perform(as(otherSeller, get("/api/seller/orders/" + id))).andExpect(status().isNotFound());
		mvc.perform(as(otherSeller, json(patch("/api/seller/orders/" + id + "/status"), """
				{"status": "EM_PREPARO"}"""))).andExpect(status().isNotFound());

		String otherCustomer = register("CUSTOMER", "");
		mvc.perform(as(otherCustomer, get("/api/orders/" + id))).andExpect(status().isNotFound());
	}

	@Test
	void rejectsClosedRestaurantUnavailableAndForeignProducts() throws Exception {
		mvc.perform(as(seller, json(patch("/api/seller/menu/products/" + sodaId + "/availability"), """
				{"available": false}"""))).andExpect(status().isOk());
		mvc.perform(orderRequest(customer, sodaId)).andExpect(status().isConflict());

		// Cardápio público não mostra o item indisponível
		mvc.perform(as(customer, get("/api/restaurants/" + restaurantId)))
				.andExpect(jsonPath("$.categories[0].products", hasSize(1)));

		String otherSeller = register("SELLER", ", \"restaurantName\": \"Outro\", \"restaurantCategory\": \"Lanches\"");
		int otherCategory = JsonPath.read(body(as(otherSeller, json(post("/api/seller/menu/categories"), """
				{"name": "Lanches"}"""))), "$.id");
		int foreignProduct = JsonPath.read(body(as(otherSeller, json(post("/api/seller/menu/products"), """
				{"categoryId": %d, "name": "X-Burguer", "price": 30, "costPrice": 10}""".formatted(otherCategory)))),
				"$.id");
		mvc.perform(orderRequest(customer, foreignProduct)).andExpect(status().isConflict());

		mvc.perform(as(seller, json(patch("/api/seller/restaurant/open"), """
				{"open": false}"""))).andExpect(status().isOk());
		mvc.perform(orderRequest(customer, pizzaId)).andExpect(status().isConflict());
	}

	@Test
	void validatesRegistrationAndOrderPayload() throws Exception {
		mvc.perform(json(post("/api/auth/register"), """
				{"name": "V", "email": "v-%s@foody.com", "password": "segredo123", "role": "SELLER"}"""
				.formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.restaurantName").exists());

		mvc.perform(as(customer, json(post("/api/orders"), """
				{"restaurantId": %d, "items": [], "deliveryAddress": {"street": "Rua"}}""".formatted(restaurantId))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.items").exists())
				.andExpect(jsonPath("$.errors.paymentMethod").exists())
				.andExpect(jsonPath("$.errors['deliveryAddress.city']").exists());

		mvc.perform(json(post("/api/auth/login"), """
				{"email": "ninguem@foody.com", "password": "qualquer"}"""))
				.andExpect(status().isUnauthorized());
	}

	// ---- auxiliares ----------------------------------------------------------------------------

	private String register(String role, String extra) throws Exception {
		String response = body(json(post("/api/auth/register"), """
				{"name": "Usuário", "email": "u-%s@foody.com", "password": "segredo123", "role": "%s"%s}"""
				.formatted(UUID.randomUUID(), role, extra)));
		return JsonPath.read(response, "$.token");
	}

	private long createProduct(int categoryId, String name, String price, String cost) throws Exception {
		return JsonPath.<Integer>read(body(as(seller, json(post("/api/seller/menu/products"), """
				{"categoryId": %d, "name": "%s", "price": %s, "costPrice": %s}"""
				.formatted(categoryId, name, price, cost)))), "$.id");
	}

	private int placeOrder(String token, long productId) throws Exception {
		return JsonPath.read(mvc.perform(orderRequest(token, productId))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	private MockHttpServletRequestBuilder orderRequest(String token, long productId) {
		return as(token, json(post("/api/orders"), """
				{"restaurantId": %d, "paymentMethod": "CARTAO", "deliveryAddress": %s,
				 "items": [{"productId": %d, "quantity": 1}]}""".formatted(restaurantId, ADDRESS, productId)));
	}

	private String body(MockHttpServletRequestBuilder request) throws Exception {
		return mvc.perform(request).andReturn().getResponse().getContentAsString();
	}

	private static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

}
