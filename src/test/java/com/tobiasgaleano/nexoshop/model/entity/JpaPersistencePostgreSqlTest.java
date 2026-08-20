package com.tobiasgaleano.nexoshop.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.tobiasgaleano.nexoshop.model.enums.PaymentMethod;
import com.tobiasgaleano.nexoshop.dto.request.category.CreateCategoryRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.CreateProductRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.StockAdjustmentRequest;
import com.tobiasgaleano.nexoshop.dto.request.product.UpdateProductRequest;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.exception.DuplicateResourceException;
import com.tobiasgaleano.nexoshop.repository.CategoryRepository;
import com.tobiasgaleano.nexoshop.repository.ProductRepository;
import com.tobiasgaleano.nexoshop.service.CategoryService;
import com.tobiasgaleano.nexoshop.service.ProductService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, properties = {
		"spring.flyway.enabled=true",
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.jpa.open-in-view=false"
})
@Testcontainers
class JpaPersistencePostgreSqlTest {

	@Container
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.6-bookworm")
			.withDatabaseName("nexoshop_test")
			.withUsername("nexoshop_test")
			.withPassword("nexoshop_test");

	private final EntityManagerFactory entityManagerFactory;
	private final JdbcTemplate jdbcTemplate;
	private final Environment environment;
	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final CategoryService categoryService;
	private final ProductService productService;

	@Autowired
	JpaPersistencePostgreSqlTest(EntityManagerFactory entityManagerFactory, DataSource dataSource,
			Environment environment, CategoryRepository categoryRepository, ProductRepository productRepository,
			CategoryService categoryService, ProductService productService) {
		this.entityManagerFactory = entityManagerFactory;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.environment = environment;
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.categoryService = categoryService;
		this.productService = productService;
	}

	@DynamicPropertySource
	static void configurePostgreSql(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
	}

	@AfterEach
	void cleanBusinessData() {
		jdbcTemplate.update("DELETE FROM cart_items");
		jdbcTemplate.update("DELETE FROM order_items");
		jdbcTemplate.update("DELETE FROM carts");
		jdbcTemplate.update("DELETE FROM orders");
		jdbcTemplate.update("DELETE FROM products");
		jdbcTemplate.update("DELETE FROM categories");
		jdbcTemplate.update("DELETE FROM users");
	}

	@Test
	void flywayAppliesOnlyV1AndHibernateValidatesItsSchema() {
		Integer migrationCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history", Integer.class);
		String version = jdbcTemplate.queryForObject(
				"SELECT version FROM flyway_schema_history WHERE success", String.class);
		List<String> businessTables = jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_type = 'BASE TABLE'
				  AND table_name <> 'flyway_schema_history'
				ORDER BY table_name
				""", String.class);

		assertThat(migrationCount).isEqualTo(1);
		assertThat(version).isEqualTo("1");
		assertThat(businessTables).containsExactly(
				"cart_items", "carts", "categories", "order_items", "orders", "products", "users");
		assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
	}

	@Test
	void auditingAndPasswordHashValueObjectPersistWithoutExposingTheEncodedValue() {
		Long userId = inTransaction(entityManager -> {
			User user = TestData.user("AUDIT@EXAMPLE.COM");
			entityManager.persist(user);
			entityManager.flush();

			assertThat(user.getCreatedAt()).isNotNull();
			assertThat(user.getUpdatedAt()).isNotNull();
			return user.getId();
		});

		Integer encodedLength = jdbcTemplate.queryForObject(
				"SELECT length(password_hash) FROM users WHERE id = ?", Integer.class, userId);
		String storedEmail = jdbcTemplate.queryForObject(
				"SELECT email FROM users WHERE id = ?", String.class, userId);

		assertThat(encodedLength).isEqualTo(60);
		assertThat(storedEmail).isEqualTo("audit@example.com");
	}

	@Test
	void cartPersistsDetectsDetachedProxyAndRemovesOrphan() {
		CartIds ids = inTransaction(entityManager -> {
			Catalog catalog = persistCatalog(entityManager, "cart@example.com", "SKU-CART",
					new BigDecimal("25.50"));
			Cart cart = new Cart(catalog.user());
			cart.addProduct(catalog.product(), 1);
			entityManager.persist(cart);
			entityManager.flush();
			return new CartIds(cart.getId(), catalog.product().getId());
		});

		Product detachedProxy = withEntityManager(
				entityManager -> entityManager.getReference(Product.class, ids.productId()));
		inTransaction(entityManager -> {
			Cart cart = entityManager.find(Cart.class, ids.cartId());
			CartItem item = cart.addProduct(detachedProxy, 2);
			assertThat(cart.getItems()).hasSize(1);
			assertThat(item.getQuantity()).isEqualTo(3);
			return null;
		});

		assertThat(jdbcTemplate.queryForObject(
				"SELECT quantity FROM cart_items WHERE cart_id = ?", Integer.class, ids.cartId()))
				.isEqualTo(3);

		inTransaction(entityManager -> {
			Cart cart = entityManager.find(Cart.class, ids.cartId());
			Product productReference = entityManager.getReference(Product.class, ids.productId());
			cart.removeProduct(productReference);
			return null;
		});

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM cart_items WHERE cart_id = ?", Integer.class, ids.cartId()))
				.isZero();
	}

	@Test
	void completeOrderAndExactMaximumAmountsPersistWithTheirDetails() {
		Long orderId = inTransaction(entityManager -> {
			Catalog catalog = persistCatalog(entityManager, "order@example.com", "SKU-MAX",
					new BigDecimal("9999999999.99"));
			Order order = Order.create("ORD-MAX", catalog.user(), PaymentMethod.CREDIT_CARD,
					"Tobias Galeano", "+595981000000", "Main Street 123", "Asuncion", null,
					BigDecimal.ZERO, catalog.product(), 1);
			entityManager.persist(order);
			entityManager.flush();

			assertThat(order.getCreatedAt()).isNotNull();
			assertThat(order.getItems().getFirst().getCreatedAt()).isNotNull();
			return order.getId();
		});

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM order_items WHERE order_id = ?", Integer.class, orderId))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT total FROM orders WHERE id = ?", BigDecimal.class, orderId))
				.isEqualByComparingTo("9999999999.99");
	}

	@Test
	void prePersistRejectsAnInternallyForcedEmptyOrder() {
		Throwable thrown = catchThrowable(() -> inTransaction(entityManager -> {
			entityManager.persist(new Order());
			entityManager.flush();
			return null;
		}));

		assertThat(rootCause(thrown))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least one item");
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
	}

	@Test
	void deletingOrderDoesNotCascadeToHistoricalDetails() {
		Long orderId = inTransaction(entityManager -> {
			Catalog catalog = persistCatalog(entityManager, "history@example.com", "SKU-HISTORY",
					new BigDecimal("10.00"));
			Order order = Order.create("ORD-HISTORY", catalog.user(), PaymentMethod.BANK_TRANSFER,
					"Tobias Galeano", "+595981000000", "Main Street 123", "Asuncion", null,
					BigDecimal.ZERO, catalog.product(), 1);
			entityManager.persist(order);
			return order.getId();
		});

		Throwable thrown = catchThrowable(() -> inTransaction(entityManager -> {
			entityManager.remove(entityManager.find(Order.class, orderId));
			return null;
		}));

		assertThat(thrown).isNotNull();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM orders WHERE id = ?", Integer.class, orderId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM order_items WHERE order_id = ?", Integer.class, orderId)).isEqualTo(1);
	}

	@Test
	void categoryRepositoryFindsNamesIgnoringCaseAndPostgreSqlEnforcesRealUniqueness() {
		Category persisted = categoryRepository.saveAndFlush(new Category("Electronics", null));

		assertThat(categoryRepository.findByNameIgnoreCase("eLeCtRoNiCs"))
				.hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(persisted.getId()));
		assertThat(catchThrowable(() -> categoryRepository.saveAndFlush(new Category("ELECTRONICS", null))))
				.isInstanceOf(RuntimeException.class);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class)).isEqualTo(1);
	}

	@Test
	void productRepositoryPersistsCategoryAndPostgreSqlEnforcesSkuUniqueness() {
		Category category = categoryRepository.saveAndFlush(new Category("Electronics", null));
		Product product = productRepository.saveAndFlush(new Product(category, "SKU-1", "Keyboard", null,
				new BigDecimal("25.50"), 10, null));

		assertThat(productRepository.findBySku("SKU-1"))
				.hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(product.getId()));
		assertThat(productRepository.findById(product.getId()))
				.get()
				.extracting(Product::getCategory)
				.extracting(Category::getId)
				.isEqualTo(category.getId());
		assertThat(catchThrowable(() -> productRepository.saveAndFlush(new Product(category, "SKU-1", "Mouse", null,
				new BigDecimal("10.00"), 1, null))))
				.isInstanceOf(RuntimeException.class);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class)).isEqualTo(1);
	}

	@Test
	void catalogServicesPersistAuditAndReturnStablePagesTransactionally() throws InterruptedException {
		var category = categoryService.create(new CreateCategoryRequest("Electronics", null));
		var first = productService.create(productRequest(category.id(), "SKU-1", "Keyboard"));
		var second = productService.create(productRequest(category.id(), "SKU-2", "Mouse"));
		var third = productService.create(productRequest(category.id(), "SKU-3", "Monitor"));

		Thread.sleep(5);
		var updated = productService.update(first.id(), new UpdateProductRequest(category.id(), first.sku(),
				"Mechanical keyboard", null, first.price(), null));
		productService.deactivate(third.id());

		var firstPage = productService.getAll(PageRequest.of(0, 2));
		var secondPage = productService.getAll(PageRequest.of(1, 2));
		var activePage = productService.getActive(PageRequest.of(0, 10));

		assertThat(first.createdAt()).isNotNull();
		assertThat(first.updatedAt()).isNotNull();
		assertThat(updated.updatedAt()).isAfter(first.updatedAt());
		assertThat(firstPage.content()).extracting(response -> response.id())
				.containsExactly(first.id(), second.id());
		assertThat(secondPage.content()).extracting(response -> response.id()).containsExactly(third.id());
		assertThat(activePage.content()).extracting(response -> response.id())
				.containsExactly(first.id(), second.id());
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class)).isEqualTo(3);
	}

	@Test
	void serviceRollsBackPartialUpdateAndReportsDuplicateWithoutLeakingSql() {
		var category = categoryService.create(new CreateCategoryRequest("Electronics", null));
		var original = productService.create(productRequest(category.id(), "SKU-1", "Keyboard"));

		Throwable invalidUpdate = catchThrowable(() -> productService.update(original.id(),
				new UpdateProductRequest(category.id(), "SKU-CHANGED", "Changed",
						null, new BigDecimal("1.001"), null)));

		assertThat(invalidUpdate).isInstanceOf(BusinessRuleException.class);
		assertThat(productRepository.findBySku("SKU-1")).isPresent();
		assertThat(productRepository.findBySku("SKU-CHANGED")).isEmpty();
		assertThat(productService.getById(original.id()).name()).isEqualTo("Keyboard");

		Throwable duplicate = catchThrowable(() -> productService.create(
				productRequest(category.id(), "SKU-1", "Another product")));
		assertThat(duplicate)
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("A product with that SKU already exists");
		assertThat(duplicate.getMessage()).doesNotContain("INSERT", "constraint", "SQL");
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class)).isEqualTo(1);
	}

	@Test
	void concurrentStockDecreasesAreSerializedAndCannotOversell() throws Exception {
		var category = categoryService.create(new CreateCategoryRequest("Electronics", null));
		var product = productService.create(productRequest(category.id(), "SKU-STOCK", "Keyboard"));
		CountDownLatch start = new CountDownLatch(1);

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			List<Future<Throwable>> outcomes = List.of(
					executor.submit(() -> decreaseStockAfterSignal(start, product.id())),
					executor.submit(() -> decreaseStockAfterSignal(start, product.id())));
			start.countDown();

			assertThat(outcomes)
					.extracting(this::awaitOutcome)
					.satisfiesExactlyInAnyOrder(
							outcome -> assertThat(outcome).isNull(),
							outcome -> assertThat(outcome).isInstanceOf(BusinessRuleException.class));
		}

		assertThat(productService.getById(product.id()).stock()).isEqualTo(3);
	}

	private Throwable decreaseStockAfterSignal(CountDownLatch start, Long productId) {
		try {
			start.await();
			productService.decreaseStock(productId, new StockAdjustmentRequest(7));
			return null;
		} catch (Throwable throwable) {
			return throwable;
		}
	}

	private Throwable awaitOutcome(Future<Throwable> outcome) {
		try {
			return outcome.get();
		} catch (Exception exception) {
			throw new AssertionError("Concurrent stock operation did not complete", exception);
		}
	}

	private static CreateProductRequest productRequest(Long categoryId, String sku, String name) {
		return new CreateProductRequest(categoryId, sku, name, null, new BigDecimal("25.50"), 10, null);
	}

	private Catalog persistCatalog(EntityManager entityManager, String email, String sku, BigDecimal price) {
		User user = TestData.user(email);
		Category category = new Category("Category " + sku, null);
		Product product = new Product(category, sku, "Product " + sku, null, price, 10, null);
		entityManager.persist(user);
		entityManager.persist(category);
		entityManager.persist(product);
		return new Catalog(user, product);
	}

	private <T> T inTransaction(Function<EntityManager, T> work) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();
			T result = work.apply(entityManager);
			transaction.commit();
			return result;
		} catch (RuntimeException | Error exception) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			throw exception;
		} finally {
			entityManager.close();
		}
	}

	private <T> T withEntityManager(Function<EntityManager, T> work) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			return work.apply(entityManager);
		} finally {
			entityManager.close();
		}
	}

	private static Throwable rootCause(Throwable throwable) {
		Throwable result = throwable;
		while (result != null && result.getCause() != null) {
			result = result.getCause();
		}
		return result;
	}

	private record Catalog(User user, Product product) {
	}

	private record CartIds(Long cartId, Long productId) {
	}
}
