package com.foody.delivery.seed;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
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
import com.foody.delivery.user.Role;
import com.foody.delivery.user.User;
import com.foody.delivery.user.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * Popula um banco vazio com restaurantes, cardápios, clientes e ~90 dias de pedidos, para que a
 * dashboard tenha histórico desde o primeiro acesso. Os volumes seguem padrões reais de delivery:
 * picos no almoço e no jantar, segunda e terça fracas, sexta e sábado fortes.
 *
 * <p>Logins de demonstração (senha {@value #DEMO_PASSWORD}): {@code cliente@foody.com} e
 * {@code vendedor@foody.com} (dono da Cantina da Nonna).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {

	public static final String DEMO_PASSWORD = "demo123";

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
	private static final int HISTORY_DAYS = 90;

	/** Multiplicador de volume por dia da semana (segunda … domingo). */
	private static final double[] WEEKDAY_FACTOR = { 0.55, 0.65, 0.85, 0.95, 1.30, 1.45, 1.25 };

	// Pesos de pedidos por hora do dia (0h … 23h) para cada perfil de restaurante.
	private static final double[] DINNER = { 1, 0.5, 0.2, 0, 0, 0, 0, 0, 0, 0, 0.3, 2, 5, 5, 3, 1.5, 1.5, 2, 6, 11, 12, 9, 5, 2.5 };
	private static final double[] LUNCH = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.3, 1.5, 8, 13, 11, 5, 1.5, 1, 1.5, 3, 4, 3.5, 2, 0.8, 0.2 };
	private static final double[] AFTERNOON = { 0.3, 0, 0, 0, 0, 0, 0, 0, 0.5, 1, 1.5, 2, 3, 4, 6, 8, 9, 7, 5, 6, 7, 5, 3, 1 };

	private static final String[] FIRST_NAMES = { "Lucas", "Mariana", "Pedro", "Juliana", "Gabriel", "Camila",
			"Rafael", "Beatriz", "Thiago", "Larissa", "Felipe", "Amanda", "Bruno", "Letícia", "Gustavo", "Fernanda",
			"Diego", "Patrícia", "Rodrigo", "Aline", "Mateus", "Carolina", "Vinícius", "Renata", "André", "Isabela",
			"Leonardo", "Natália", "Eduardo" };
	private static final String[] LAST_NAMES = { "Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira",
			"Alves", "Pereira", "Lima", "Gomes", "Costa", "Ribeiro", "Martins", "Carvalho", "Almeida" };
	private static final String[][] NEIGHBORHOODS = { { "Pinheiros", "05422-000" }, { "Vila Madalena", "05433-000" },
			{ "Moema", "04077-000" }, { "Itaim Bibi", "04530-000" }, { "Perdizes", "05014-000" },
			{ "Vila Mariana", "04101-000" }, { "Tatuapé", "03310-000" }, { "Santana", "02012-000" },
			{ "Bela Vista", "01310-000" }, { "Brooklin", "04571-000" } };
	private static final String[] STREETS = { "Rua dos Pinheiros", "Rua Harmonia", "Av. Ibirapuera",
			"Rua Joaquim Floriano", "Rua Cardoso de Almeida", "Rua Domingos de Morais", "Rua Tuiuti",
			"Rua Voluntários da Pátria", "Rua Augusta", "Av. Santo Amaro" };
	private static final String[] ITEM_NOTES = { "sem cebola", "capricha no molho", "bem passado", "sem gelo",
			"molho à parte", "sem tomate" };

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final TransactionTemplate transaction;
	private final EntityManager em;
	private final Random random = new Random(42);

	public DemoDataSeeder(UserRepository users, PasswordEncoder passwordEncoder, TransactionTemplate transaction,
			EntityManager em) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.transaction = transaction;
		this.em = em;
	}

	@Override
	public void run(String... args) {
		if (users.count() > 0) {
			return;
		}
		long started = System.currentTimeMillis();
		String passwordHash = passwordEncoder.encode(DEMO_PASSWORD);

		List<User> customers = transaction.execute(tx -> createCustomers(passwordHash));
		List<RestaurantSpec> specs = restaurants();
		int totalOrders = 0;
		for (RestaurantSpec spec : specs) {
			totalOrders += transaction.execute(tx -> seedRestaurant(spec, passwordHash, customers));
		}
		transaction.executeWithoutResult(tx -> {
			seedDemoCustomerHistory(customers.getFirst());
			// Um restaurante fechado, para a vitrine mostrar esse estado.
			em.createQuery("update Restaurant r set r.open = false where r.name = 'Verde Vida'").executeUpdate();
		});

		log.info("Dados de demonstração criados: {} restaurantes, {} clientes, {} pedidos em {} ms. "
				+ "Logins: cliente@foody.com / vendedor@foody.com (senha {})", specs.size(), customers.size(),
				totalOrders, System.currentTimeMillis() - started, DEMO_PASSWORD);
	}

	// ---- Cadastros ----------------------------------------------------------------------------

	private List<User> createCustomers(String passwordHash) {
		List<User> result = new ArrayList<>();
		result.add(persist(new User("Ana Beatriz", "cliente@foody.com", passwordHash, Role.CUSTOMER)));
		for (int i = 0; i < 40; i++) {
			String first = FIRST_NAMES[i % FIRST_NAMES.length];
			String last = LAST_NAMES[(i * 7) % LAST_NAMES.length];
			String email = (first + "." + last + i).toLowerCase()
					.replaceAll("[áàã]", "a").replaceAll("[éê]", "e").replaceAll("í", "i").replaceAll("[óô]", "o")
					+ "@exemplo.com";
			result.add(persist(new User(first + " " + last, email, passwordHash, Role.CUSTOMER)));
		}
		return result;
	}

	private int seedRestaurant(RestaurantSpec spec, String passwordHash, List<User> customers) {
		User owner = persist(new User(spec.ownerName(), spec.ownerEmail(), passwordHash, Role.SELLER));
		Restaurant restaurant = new Restaurant(owner, spec.name(), spec.category());
		restaurant.updateProfile(spec.name(), spec.category(), spec.description(), spec.emoji(),
				new BigDecimal(spec.deliveryFee()), spec.prepMin(), spec.prepMax());
		persist(restaurant);

		List<Product> products = new ArrayList<>();
		List<Double> popularity = new ArrayList<>();
		int categoryPosition = 0;
		for (MenuSection section : spec.menu()) {
			MenuCategory category = persist(new MenuCategory(restaurant, section.name(), categoryPosition));
			int position = 0;
			for (String[] p : section.items()) {
				products.add(persist(new Product(restaurant, category, p[0], p[1], new BigDecimal(p[2]),
						new BigDecimal(p[3]), p[4], null, true, position++)));
				// O carro-chefe (primeira seção) vende mais; dentro dela, os primeiros itens mais ainda.
				double base = categoryPosition == 0 ? 3.0 : 1.2;
				popularity.add(base * (1.0 + random.nextDouble()) / (1 + position * 0.15));
			}
			categoryPosition++;
		}

		LocalDate today = BusinessTime.today();
		ZonedDateTime now = ZonedDateTime.now(BusinessTime.ZONE);
		int count = 0;
		for (int daysAgo = HISTORY_DAYS; daysAgo >= 0; daysAgo--) {
			LocalDate day = today.minusDays(daysAgo);
			double trend = 0.85 + 0.25 * (HISTORY_DAYS - daysAgo) / HISTORY_DAYS;
			double expected = spec.dailyOrders() * weekdayFactor(spec, day.getDayOfWeek()) * trend
					* (0.85 + random.nextDouble() * 0.3);
			int ordersToday = (int) Math.round(expected);
			for (int i = 0; i < ordersToday; i++) {
				ZonedDateTime placed = day.atStartOfDay(BusinessTime.ZONE)
						.plusHours(pick(spec.hours()))
						.plusMinutes(random.nextInt(60))
						.plusSeconds(random.nextInt(60));
				if (placed.isAfter(now)) {
					continue;
				}
				User customer = customers.get(1 + random.nextInt(customers.size() - 1));
				persistOrder(restaurant, customer, products, popularity, placed, now);
				count++;
			}
			em.flush();
			em.clear();
		}
		return count;
	}

	/** Alguns pedidos recentes, já entregues, para a conta de demonstração do cliente. */
	private void seedDemoCustomerHistory(User demoCustomer) {
		List<Restaurant> all = em.createQuery("select r from Restaurant r order by r.id", Restaurant.class)
				.getResultList();
		ZonedDateTime now = ZonedDateTime.now(BusinessTime.ZONE);
		int[] daysAgo = { 2, 5, 9, 13, 18, 24 };
		for (int i = 0; i < daysAgo.length; i++) {
			Restaurant restaurant = all.get(i % all.size());
			List<Product> products = em
					.createQuery("select p from Product p where p.restaurant = :r order by p.id", Product.class)
					.setParameter("r", restaurant).getResultList();
			List<Double> popularity = products.stream().map(p -> 1.0).toList();
			ZonedDateTime placed = now.minusDays(daysAgo[i]).withHour(i % 2 == 0 ? 20 : 12).withMinute(15 + i * 7);
			persistOrder(restaurant, demoCustomer, products, popularity, placed, now);
		}
	}

	private void persistOrder(Restaurant restaurant, User customer, List<Product> products, List<Double> popularity,
			ZonedDateTime placed, ZonedDateTime now) {
		int lines = 1 + pick(new double[] { 45, 35, 20 });
		List<OrderItem> items = new ArrayList<>();
		for (int l = 0; l < lines; l++) {
			Product product = products.get(pick(popularity));
			int quantity = 1 + pick(new double[] { 75, 20, 5 });
			String notes = random.nextDouble() < 0.12 ? ITEM_NOTES[random.nextInt(ITEM_NOTES.length)] : null;
			items.add(new OrderItem(product, quantity, notes));
		}
		PaymentMethod payment = PaymentMethod.values()[pick(new double[] { 45, 45, 10 })];
		Order order = new Order(customer, restaurant, randomAddress(), items, payment,
				random.nextDouble() < 0.08 ? "Interfone com defeito, me ligue ao chegar" : null);

		long minutesAgo = ChronoUnit.MINUTES.between(placed, now);
		Instant updated = placed.toInstant();
		if (random.nextDouble() < 0.065 && minutesAgo > 20) {
			order.changeStatus(OrderStatus.CANCELADO);
			updated = placed.plusMinutes(3 + random.nextInt(10)).toInstant();
		} else {
			// Pedidos de hoje ainda podem estar em andamento, conforme há quanto tempo foram feitos.
			OrderStatus[] flow = { OrderStatus.EM_PREPARO, OrderStatus.SAIU_PARA_ENTREGA, OrderStatus.ENTREGUE };
			long[] reachedAfter = { 4, 28, 55 };
			for (int s = 0; s < flow.length && minutesAgo >= reachedAfter[s]; s++) {
				order.changeStatus(flow[s]);
				updated = placed.plusMinutes(reachedAfter[s] + random.nextInt(8)).toInstant();
			}
		}
		order.placedAt(placed.toInstant(), updated.isAfter(now.toInstant()) ? now.toInstant() : updated);
		em.persist(order);
	}

	private DeliveryAddress randomAddress() {
		String[] hood = NEIGHBORHOODS[random.nextInt(NEIGHBORHOODS.length)];
		return new DeliveryAddress(STREETS[random.nextInt(STREETS.length)], String.valueOf(10 + random.nextInt(1900)),
				random.nextDouble() < 0.4 ? "Apto " + (11 + random.nextInt(180)) : null, hood[0], "São Paulo", hood[1]);
	}

	private double weekdayFactor(RestaurantSpec spec, DayOfWeek dow) {
		double factor = WEEKDAY_FACTOR[dow.getValue() - 1];
		// Comida caseira vende bem no almoço de domingo; doceria cresce no fim de semana.
		if (spec.hours() == LUNCH && dow == DayOfWeek.SUNDAY) {
			factor *= 1.15;
		}
		return factor;
	}

	private int pick(double[] weights) {
		double total = 0;
		for (double w : weights) {
			total += w;
		}
		double r = random.nextDouble() * total;
		for (int i = 0; i < weights.length; i++) {
			r -= weights[i];
			if (r < 0) {
				return i;
			}
		}
		return weights.length - 1;
	}

	private int pick(List<Double> weights) {
		return pick(weights.stream().mapToDouble(Double::doubleValue).toArray());
	}

	private <T> T persist(T entity) {
		em.persist(entity);
		return entity;
	}

	// ---- Catálogo de demonstração -------------------------------------------------------------

	private record MenuSection(String name, String[][] items) {
	}

	private record RestaurantSpec(String name, String category, String emoji, String description, String ownerName,
			String ownerEmail, String deliveryFee, int prepMin, int prepMax, int dailyOrders, double[] hours,
			List<MenuSection> menu) {
	}

	/** Itens: { nome, descrição, preço, custo, emoji }. */
	private static List<RestaurantSpec> restaurants() {
		return List.of(
				new RestaurantSpec("Cantina da Nonna", "Pizzaria", "🍕",
						"Pizzas de fermentação natural e massas caseiras, como na casa da nonna.",
						"Marco Rossi", "vendedor@foody.com", "6.99", 35, 50, 26, DINNER, List.of(
								new MenuSection("Pizzas salgadas", new String[][] {
										{ "Margherita", "Molho de tomate, muçarela de búfala e manjericão", "49.90", "14.20", "🍕" },
										{ "Calabresa", "Calabresa artesanal, cebola roxa e azeitonas", "52.90", "15.80", "🍕" },
										{ "Quatro queijos", "Muçarela, gorgonzola, parmesão e catupiry", "58.90", "19.90", "🧀" },
										{ "Portuguesa", "Presunto, ovos, cebola, ervilha e azeitonas", "56.90", "18.40", "🍕" },
										{ "Frango com catupiry", "Frango desfiado temperado e catupiry original", "55.90", "17.30", "🍗" },
										{ "Pepperoni", "Pepperoni fatiado e muçarela", "59.90", "20.10", "🍕" } }),
								new MenuSection("Pizzas doces", new String[][] {
										{ "Chocolate com morango", "Chocolate ao leite e morangos frescos", "54.90", "18.90", "🍫" },
										{ "Romeu e Julieta", "Goiabada cremosa e muçarela", "49.90", "15.20", "🍓" } }),
								new MenuSection("Massas", new String[][] {
										{ "Lasanha à bolonhesa", "Ragu de carne cozido por 6 horas", "42.90", "13.50", "🍝" },
										{ "Nhoque ao sugo", "Nhoque de batata com molho de tomate fresco", "36.90", "9.80", "🍝" } }),
								new MenuSection("Bebidas", new String[][] {
										{ "Refrigerante 2L", "Coca-Cola, Guaraná ou Fanta", "14.90", "7.20", "🥤" },
										{ "Suco natural 500ml", "Laranja, limão ou maracujá", "11.90", "3.90", "🧃" },
										{ "Água mineral", "500ml, com ou sem gás", "4.90", "1.60", "💧" } }))),
				new RestaurantSpec("Sushi Kai", "Japonesa", "🍣",
						"Culinária japonesa com peixe fresco todos os dias.",
						"Kenji Tanaka", "sushi@foody.com", "7.99", 40, 55, 16, DINNER, List.of(
								new MenuSection("Combinados", new String[][] {
										{ "Combinado 20 peças", "Sashimis, uramakis, niguiris e hossomakis", "69.90", "26.50", "🍣" },
										{ "Combinado 40 peças", "Para dividir: seleção completa do chef", "119.90", "45.00", "🍱" },
										{ "Combinado salmão 30 peças", "Só salmão, em várias versões", "98.90", "41.00", "🍣" } }),
								new MenuSection("Pratos quentes", new String[][] {
										{ "Hot roll 10 unidades", "Empanado e crocante, com tarê", "29.90", "9.20", "🔥" },
										{ "Yakisoba", "Macarrão oriental com legumes e carne", "39.90", "12.40", "🍜" },
										{ "Guioza 6 unidades", "Pastel japonês grelhado", "24.90", "7.10", "🥟" } }),
								new MenuSection("Temakis", new String[][] {
										{ "Temaki de salmão", "Salmão fresco, arroz e cebolinha", "32.90", "12.80", "🍙" },
										{ "Temaki skin", "Pele de salmão grelhada com cream cheese", "26.90", "8.50", "🍙" } }),
								new MenuSection("Bebidas", new String[][] {
										{ "Chá verde gelado", "Feito na casa", "8.90", "2.40", "🍵" },
										{ "Refrigerante lata", "350ml", "6.90", "3.10", "🥤" } }))),
				new RestaurantSpec("Burger Bros", "Lanches", "🍔",
						"Smash burgers com blend da casa e pão brioche.",
						"Rafael Lima", "burger@foody.com", "4.99", 25, 40, 22, DINNER, List.of(
								new MenuSection("Burgers", new String[][] {
										{ "Classic burger", "Blend 160g, queijo, alface, tomate e molho da casa", "32.90", "10.40", "🍔" },
										{ "Cheddar bacon", "Blend 160g, cheddar cremoso e bacon crocante", "38.90", "13.20", "🥓" },
										{ "Duplo smash", "Dois smash de 90g com queijo americano", "42.90", "15.10", "🍔" },
										{ "Veggie burger", "Hambúrguer de grão-de-bico e legumes", "34.90", "11.60", "🥬" } }),
								new MenuSection("Combos", new String[][] {
										{ "Combo classic", "Classic burger + batata + refrigerante", "49.90", "17.30", "🍟" } }),
								new MenuSection("Acompanhamentos", new String[][] {
										{ "Batata frita", "Porção individual com páprica", "18.90", "4.30", "🍟" },
										{ "Onion rings", "Anéis de cebola empanados", "21.90", "5.40", "🧅" },
										{ "Nuggets 10 unidades", "Com molho barbecue", "22.90", "7.20", "🍗" } }),
								new MenuSection("Bebidas", new String[][] {
										{ "Milkshake", "Chocolate, morango ou baunilha", "19.90", "6.10", "🥤" },
										{ "Refrigerante lata", "350ml", "6.90", "3.10", "🥤" } }))),
				new RestaurantSpec("Sabor Mineiro", "Brasileira", "🍲",
						"Comida mineira feita no fogão a lenha.",
						"Lúcia Andrade", "mineiro@foody.com", "5.49", 30, 45, 18, LUNCH, List.of(
								new MenuSection("Pratos", new String[][] {
										{ "PF do dia", "Arroz, feijão, salada, farofa e a carne do dia", "27.90", "8.70", "🍛" },
										{ "Feijão tropeiro", "Com torresmo, linguiça e couve", "34.90", "10.90", "🍲" },
										{ "Frango com quiabo", "Acompanha arroz e angu", "36.90", "11.80", "🍗" },
										{ "Tutu com linguiça", "Tutu de feijão, linguiça e couve", "33.90", "10.10", "🍲" },
										{ "Picanha na chapa", "400g, com arroz, farofa e vinagrete", "64.90", "28.40", "🥩" } }),
								new MenuSection("Porções", new String[][] {
										{ "Pão de queijo 12 unidades", "Feito com queijo canastra", "19.90", "5.20", "🧀" },
										{ "Mandioca frita", "Crocante por fora, macia por dentro", "22.90", "5.90", "🍠" } }),
								new MenuSection("Sobremesas", new String[][] {
										{ "Doce de leite com queijo", "Doce de leite caseiro e queijo minas", "14.90", "4.30", "🍮" } }),
								new MenuSection("Bebidas", new String[][] {
										{ "Suco de laranja", "500ml, natural", "9.90", "3.20", "🍊" },
										{ "Refrigerante lata", "350ml", "6.90", "3.10", "🥤" } }))),
				new RestaurantSpec("Doce Encanto", "Doces", "🍰",
						"Confeitaria artesanal: bolos, tortas e doces finos.",
						"Bianca Souza", "doces@foody.com", "5.99", 20, 35, 12, AFTERNOON, List.of(
								new MenuSection("Bolos e tortas", new String[][] {
										{ "Fatia de bolo de cenoura", "Com cobertura de chocolate", "14.90", "3.80", "🥕" },
										{ "Fatia de red velvet", "Com creme de cream cheese", "18.90", "5.60", "🍰" },
										{ "Torta de limão (fatia)", "Base crocante e merengue maçaricado", "15.90", "4.70", "🍋" },
										{ "Bolo de chocolate inteiro", "Serve 12 pessoas", "89.90", "32.00", "🎂" } }),
								new MenuSection("Doces", new String[][] {
										{ "Brigadeiros gourmet (6 un.)", "Sabores sortidos", "24.90", "7.40", "🍫" },
										{ "Brownie", "Com nozes", "12.90", "3.60", "🍫" } }),
								new MenuSection("Cafés", new String[][] {
										{ "Café espresso", "Grãos especiais do sul de Minas", "6.90", "1.40", "☕" },
										{ "Cappuccino", "Com canela", "12.90", "3.20", "☕" } }))),
				new RestaurantSpec("Verde Vida", "Saudável", "🥗",
						"Saladas, bowls e wraps com ingredientes orgânicos.",
						"Carla Mendes", "verde@foody.com", "6.49", 25, 40, 11, LUNCH, List.of(
								new MenuSection("Saladas e bowls", new String[][] {
										{ "Bowl de quinoa", "Quinoa, grão-de-bico, legumes assados e tahine", "39.90", "13.40", "🥗" },
										{ "Salada caesar", "Alface romana, frango grelhado, parmesão e croutons", "34.90", "11.20", "🥗" },
										{ "Salada tropical", "Folhas, manga, palmito e castanhas", "31.90", "9.80", "🥭" } }),
								new MenuSection("Wraps", new String[][] {
										{ "Wrap de frango", "Frango, cream cheese e folhas", "29.90", "9.60", "🌯" },
										{ "Wrap vegano", "Homus, falafel e vegetais", "28.90", "8.90", "🌯" } }),
								new MenuSection("Bebidas", new String[][] {
										{ "Suco verde", "Couve, limão, gengibre e maçã", "14.90", "4.10", "🥬" },
										{ "Kombucha", "300ml, sabores da estação", "16.90", "5.80", "🍹" } }))));
	}

}
