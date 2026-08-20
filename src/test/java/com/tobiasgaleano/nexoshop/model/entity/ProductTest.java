package com.tobiasgaleano.nexoshop.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductTest {

	@Test
	void rejectsNonPositivePrice() {
		Category category = TestData.category();

		assertThatThrownBy(() -> new Product(category, "SKU-1", "Keyboard", null, BigDecimal.ZERO, 1, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> product().changePrice(new BigDecimal("-0.01")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void acceptsAndNormalizesExactlyRepresentablePrices() {
		Product normalized = new Product(TestData.category(), "SKU-1", "Keyboard", null,
				new BigDecimal("1.000"), 1, null);
		Product maximum = new Product(TestData.category(), "SKU-2", "Mouse", null,
				new BigDecimal("9999999999.99"), 1, null);

		assertThat(normalized.getPrice()).isEqualTo(new BigDecimal("1.00"));
		assertThat(maximum.getPrice()).isEqualTo(new BigDecimal("9999999999.99"));
	}

	@Test
	void rejectsUnrepresentableScaleAndPrecision() {
		assertThatThrownBy(() -> new Product(TestData.category(), "SKU-1", "Keyboard", null,
				new BigDecimal("1.005"), 1, null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Product(TestData.category(), "SKU-2", "Mouse", null,
				new BigDecimal("10000000000.00"), 1, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void increasesAndDecreasesStock() {
		Product product = product();

		product.increaseStock(5);
		product.decreaseStock(3);

		assertThat(product.getStock()).isEqualTo(12);
	}

	@Test
	void rejectsInsufficientStockAndInvalidAdjustments() {
		Product product = product();

		assertThatThrownBy(() -> product.decreaseStock(11))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Insufficient");
		assertThatThrownBy(() -> product.increaseStock(0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> product.decreaseStock(-1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void changesCategoryAndSkuThroughExplicitDomainOperations() {
		Product product = product();
		Category category = new Category("Accessories", null);

		product.changeCategory(category);
		product.changeSku(" SKU-2 ");

		assertThat(product.getCategory()).isSameAs(category);
		assertThat(product.getSku()).isEqualTo("SKU-2");
		assertThatThrownBy(() -> product.changeCategory(null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> product.changeSku(" ")).isInstanceOf(IllegalArgumentException.class);
	}

	private static Product product() {
		return TestData.product();
	}
}
