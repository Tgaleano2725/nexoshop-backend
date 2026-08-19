package com.tobiasgaleano.nexoshop.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	private static User user() {
		return TestData.user();
	}

	private static Product product() {
		return TestData.product();
	}
}
