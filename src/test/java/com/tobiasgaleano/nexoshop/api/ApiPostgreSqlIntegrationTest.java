package com.tobiasgaleano.nexoshop.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;
import com.tobiasgaleano.nexoshop.dto.request.category.CreateCategoryRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.CreateProductRequest;
import com.tobiasgaleano.nexoshop.dto.request.order.CreateOrderRequest;
import com.tobiasgaleano.nexoshop.dto.response.category.CategoryResponse;
import com.tobiasgaleano.nexoshop.dto.response.product.ProductResponse;
import com.tobiasgaleano.nexoshop.dto.response.user.UserResponse;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.model.enums.PaymentMethod;
import com.tobiasgaleano.nexoshop.service.CartService;
import com.tobiasgaleano.nexoshop.service.CategoryService;
import com.tobiasgaleano.nexoshop.service.OrderService;
import com.tobiasgaleano.nexoshop.service.ProductService;
import com.tobiasgaleano.nexoshop.service.UserService;

@SpringBootTest
@Testcontainers
class ApiPostgreSqlIntegrationTest {
	@Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-bookworm")
			.withDatabaseName("nexoshop_test").withUsername("nexoshop_test").withPassword("nexoshop_test");
	@DynamicPropertySource static void postgres(DynamicPropertyRegistry r) {
		r.add("spring.datasource.url", POSTGRES::getJdbcUrl); r.add("spring.datasource.username", POSTGRES::getUsername);
		r.add("spring.datasource.password", POSTGRES::getPassword); r.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
	}
	@Autowired WebApplicationContext context; MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired UserService users; @Autowired CategoryService categories;
	@Autowired ProductService products; @Autowired CartService carts; @Autowired OrderService orders;
	@org.junit.jupiter.api.BeforeEach void setUp() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

	@AfterEach void clean() { jdbc.update("DELETE FROM cart_items"); jdbc.update("DELETE FROM order_items"); jdbc.update("DELETE FROM carts"); jdbc.update("DELETE FROM orders"); jdbc.update("DELETE FROM products"); jdbc.update("DELETE FROM categories"); jdbc.update("DELETE FROM users"); }

	@Test void completeCheckoutFlowPersistsSnapshotsAndInventory() throws Exception {
		String userJson = mvc.perform(post("/api/v1/users").contentType("application/json").content("{\"firstName\":\"Tobias\",\"lastName\":\"Galeano\",\"email\":\"e2e@example.com\",\"password\":\"secret123\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long userId = ((Number) JsonPath.read(userJson, "$.id")).longValue();
		String categoryJson = mvc.perform(post("/api/v1/categories").contentType("application/json").content("{\"name\":\"E2E Books\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long categoryId = ((Number) JsonPath.read(categoryJson, "$.id")).longValue();
		String productJson = mvc.perform(post("/api/v1/products").contentType("application/json").content("{\"categoryId\":" + categoryId + ",\"sku\":\"E2E-SKU\",\"name\":\"E2E Book\",\"price\":\"12.50\",\"stock\":3}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long productId = ((Number) JsonPath.read(productJson, "$.id")).longValue();
		mvc.perform(post("/api/v1/users/" + userId + "/cart/items").contentType("application/json").content("{\"productId\":" + productId + ",\"quantity\":2}"))
				.andExpect(status().isOk());
		String orderJson = mvc.perform(post("/api/v1/users/" + userId + "/orders/checkout").contentType("application/json").content("{\"paymentMethod\":\"CREDIT_CARD\",\"shippingCost\":\"2.00\",\"recipientName\":\"Tobias\",\"recipientPhone\":\"123\",\"shippingAddress\":\"Main 1\",\"shippingCity\":\"Asuncion\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		long orderId = ((Number) JsonPath.read(orderJson, "$.orderId")).longValue();
		mvc.perform(get("/api/v1/users/" + userId + "/orders/" + orderId)).andExpect(status().isOk());
		assertThat(jdbc.queryForObject("SELECT stock FROM products WHERE id=?", Integer.class, productId)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_items", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_items WHERE order_id=?", Integer.class, orderId)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT product_sku FROM order_items WHERE order_id=?", String.class, orderId)).isEqualTo("E2E-SKU");
		assertThat(jdbc.queryForObject("SELECT total FROM orders WHERE id=?", BigDecimal.class, orderId)).isEqualByComparingTo("27.00");
	}

	@Test void concurrentCheckoutsAllowOnlyOneBuyerAndPreserveTheLoserCart() throws Exception {
		UserResponse u1 = users.register(new com.tobiasgaleano.nexoshop.dto.request.user.RegisterUserRequest("A", "One", "concurrent1@example.com", "secret123"));
		UserResponse u2 = users.register(new com.tobiasgaleano.nexoshop.dto.request.user.RegisterUserRequest("B", "Two", "concurrent2@example.com", "secret123"));
		CategoryResponse category = categories.create(new CreateCategoryRequest("Concurrent", null));
		ProductResponse product = products.create(new CreateProductRequest(category.id(), "CONCURRENT-SKU", "Limited", null, new BigDecimal("10.00"), 1, null));
		carts.addProduct(u1.id(), new com.tobiasgaleano.nexoshop.dto.request.cart.AddCartItemRequest(product.id(), 1));
		carts.addProduct(u2.id(), new com.tobiasgaleano.nexoshop.dto.request.cart.AddCartItemRequest(product.id(), 1));
		var ready = new CountDownLatch(2); var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
		var f1 = pool.submit(() -> checkout(orders, u1.id(), ready, start)); var f2 = pool.submit(() -> checkout(orders, u2.id(), ready, start));
		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); start.countDown(); boolean one = f1.get(10, TimeUnit.SECONDS); boolean two = f2.get(10, TimeUnit.SECONDS); pool.shutdownNow();
		assertThat(one ^ two).isTrue(); assertThat(jdbc.queryForObject("SELECT stock FROM products WHERE id=?", Integer.class, product.id())).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT status FROM orders", String.class)).isEqualTo("PENDING");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_items", Integer.class)).isEqualTo(1);
	}
	private static boolean checkout(OrderService s, Long id, CountDownLatch ready, CountDownLatch start) throws Exception { ready.countDown(); start.await(5, TimeUnit.SECONDS); try { s.checkout(id, new CreateOrderRequest(PaymentMethod.CREDIT_CARD, BigDecimal.ZERO, "Buyer", "1", "A", "C", null)); return true; } catch (BusinessRuleException e) { return false; } }
}
