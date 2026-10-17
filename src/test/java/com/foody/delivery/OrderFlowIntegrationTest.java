package com.foody.delivery;

import static org.hamcrest.Matchers.contains;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

	private static final String ORDER_JSON = """
			{
			  "customerName": "João Silva",
			  "deliveryAddress": {
			    "street": "Rua das Flores", "number": "123", "complement": "Apto 4",
			    "neighborhood": "Centro", "city": "São Paulo", "zipCode": "01000-000"
			  },
			  "items": [
			    { "productName": "Pizza Margherita", "quantity": 2, "unitPrice": 45.90 },
			    { "productName": "Refrigerante", "quantity": 1, "unitPrice": 8.50 }
			  ]
			}
			""";

	@Autowired
	private MockMvc mvc;

	private String token;

	@BeforeEach
	void registerAndLogin() throws Exception {
		String email = "user-" + UUID.randomUUID() + "@foody.com";
		mvc.perform(json(post("/api/auth/register"), """
				{"name": "Maria", "email": "%s", "password": "segredo123"}""".formatted(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty());

		String body = mvc.perform(json(post("/api/auth/login"), """
				{"email": "%s", "password": "segredo123"}""".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.name").value("Maria"))
				.andReturn().getResponse().getContentAsString();
		token = JsonPath.read(body, "$.token");
	}

	@Test
	void fullOrderLifecycle() throws Exception {
		String created = mvc.perform(auth(json(post("/api/orders"), ORDER_JSON)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("RECEBIDO"))
				.andExpect(jsonPath("$.total").value(100.30))
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.nextStatuses", contains("EM_PREPARO", "CANCELADO")))
				.andExpect(jsonPath("$.createdBy").value("Maria"))
				.andReturn().getResponse().getContentAsString();
		int id = JsonPath.read(created, "$.id");

		mvc.perform(auth(get("/api/orders")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

		mvc.perform(auth(get("/api/orders/" + id)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerName").value("João Silva"))
				.andExpect(jsonPath("$.deliveryAddress.city").value("São Paulo"));

		mvc.perform(auth(json(patch("/api/orders/" + id + "/status"), """
				{"status": "ENTREGUE"}""")))
				.andExpect(status().isConflict());

		for (String next : new String[] { "EM_PREPARO", "SAIU_PARA_ENTREGA", "ENTREGUE" }) {
			mvc.perform(auth(json(patch("/api/orders/" + id + "/status"), """
					{"status": "%s"}""".formatted(next))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value(next));
		}

		mvc.perform(auth(json(patch("/api/orders/" + id + "/status"), """
				{"status": "CANCELADO"}""")))
				.andExpect(status().isConflict());

		mvc.perform(auth(get("/api/orders").param("status", "ENTREGUE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
	}

	@Test
	void requiresAuthentication() throws Exception {
		mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
				.andExpect(status().isUnauthorized());
		mvc.perform(json(post("/api/orders"), ORDER_JSON)).andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsWrongPasswordAndDuplicateEmail() throws Exception {
		mvc.perform(json(post("/api/auth/login"), """
				{"email": "ninguem@foody.com", "password": "qualquer"}"""))
				.andExpect(status().isUnauthorized());

		String email = "dup-" + UUID.randomUUID() + "@foody.com";
		String register = """
				{"name": "Ana", "email": "%s", "password": "segredo123"}""".formatted(email);
		mvc.perform(json(post("/api/auth/register"), register)).andExpect(status().isCreated());
		mvc.perform(json(post("/api/auth/register"), register)).andExpect(status().isConflict());
	}

	@Test
	void validatesOrderPayload() throws Exception {
		mvc.perform(auth(json(post("/api/orders"), """
				{"customerName": "", "deliveryAddress": {"street": "Rua"}, "items": []}""")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.customerName").exists())
				.andExpect(jsonPath("$.errors.items").exists())
				.andExpect(jsonPath("$.errors['deliveryAddress.city']").exists());

		mvc.perform(auth(json(patch("/api/orders/1/status"), """
				{"status": "VOANDO"}""")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsNotFoundForUnknownOrder() throws Exception {
		mvc.perform(auth(get("/api/orders/999999"))).andExpect(status().isNotFound());
	}

	private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

}
