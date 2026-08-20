package com.tobiasgaleano.nexoshop.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CartTest {

	@Test
	void addsProductAndSynchronizesBothSides() {
		Cart cart = new Cart(user());
		Product product = product();

		CartItem item = cart.addProduct(product, 2);

		assertThat(cart.getItems()).containsExactly(item);
		assertThat(item.getCart()).isSameAs(cart);
		assertThat(item.getProduct()).isSameAs(product);
		assertThat(item.getQuantity()).isEqualTo(2);
	}

	@Test
	void incrementsQuantityWithoutDuplicatingProduct() {
		Cart cart = new Cart(user());
		Product product = product();

		CartItem original = cart.addProduct(product, 2);
		CartItem repeated = cart.addProduct(product, 3);

		assertThat(repeated).isSameAs(original);
		assertThat(cart.getItems()).hasSize(1);
		assertThat(original.getQuantity()).isEqualTo(5);
	}

	@Test
	void changesAndRemovesQuantityThroughCart() {
		Cart cart = new Cart(user());
		Product product = product();
		CartItem item = cart.addProduct(product, 1);

		cart.changeQuantity(product, 4);
		assertThat(item.getQuantity()).isEqualTo(4);

		cart.removeProduct(product);
		assertThat(cart.getItems()).isEmpty();
		assertThat(item.getCart()).isNull();
	}

	@Test
	void rejectsInvalidQuantitiesAndDoesNotExposeMutableItems() {
		Cart cart = new Cart(user());
		Product product = product();

		assertThatThrownBy(() -> cart.addProduct(product, 0))
				.isInstanceOf(IllegalArgumentException.class);
		cart.addProduct(product, 1);
		assertThatThrownBy(() -> cart.changeQuantity(product, -1))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> cart.getItems().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void clearsItemsAndSynchronizesBothSides() {
		Cart cart = new Cart(user());
		CartItem first = cart.addProduct(product(), 1);
		CartItem second = cart.addProduct(TestData.product("SKU-2", new BigDecimal("10.00")), 2);

		cart.clear();

		assertThat(cart.getItems()).isEmpty();
		assertThat(first.getCart()).isNull();
		assertThat(second.getCart()).isNull();
	}

	@Test
	void calculatesCurrentPriceLineTotalsUnitsAndExactSubtotal() {
		Cart cart = new Cart(user());
		Product first = product();
		Product second = TestData.product("SKU-2", new BigDecimal("0.10"));
		cart.addProduct(first, 2);
		cart.addProduct(second, 3);

		assertThat(cart.quantityOf(first)).isEqualTo(2);
		assertThat(cart.getTotalUnits()).isEqualTo(5);
		assertThat(cart.calculateSubtotal()).isEqualTo(new BigDecimal("51.30"));

		first.changePrice(new BigDecimal("30.00"));
		assertThat(cart.calculateSubtotal()).isEqualTo(new BigDecimal("60.30"));
	}

	@Test
	void rejectsMonetaryAndUnitOverflow() {
		Cart monetaryOverflow = new Cart(user());
		monetaryOverflow.addProduct(TestData.product("SKU-MAX", new BigDecimal("9999999999.99")), 2);
		assertThatThrownBy(monetaryOverflow::calculateSubtotal).isInstanceOf(IllegalArgumentException.class);

		Cart unitOverflow = new Cart(user());
		unitOverflow.addProduct(product(), Integer.MAX_VALUE);
		unitOverflow.addProduct(TestData.product("SKU-2", BigDecimal.ONE), 1);
		assertThatThrownBy(unitOverflow::getTotalUnits).isInstanceOf(ArithmeticException.class);
	}

	private static User user() {
		return TestData.user();
	}

	private static Product product() {
		return TestData.product();
	}
}
