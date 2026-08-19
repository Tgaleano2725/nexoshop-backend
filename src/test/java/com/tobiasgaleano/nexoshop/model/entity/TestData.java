package com.tobiasgaleano.nexoshop.model.entity;

import java.math.BigDecimal;

import com.tobiasgaleano.nexoshop.model.enums.PaymentMethod;
import com.tobiasgaleano.nexoshop.model.enums.UserRole;
import com.tobiasgaleano.nexoshop.model.valueobject.PasswordHash;

final class TestData {

	private TestData() {
	}

	static PasswordHash passwordHash() {
		return PasswordHash.fromEncoded("$2b$12$" + "A".repeat(53));
	}

	static User user() {
		return user("user@example.com");
	}

	static User user(String email) {
		return new User("Tobias", "Galeano", email, passwordHash(), UserRole.CUSTOMER);
	}

	static Category category() {
		return new Category("Electronics", null);
	}

	static Product product() {
		return product("SKU-1", new BigDecimal("25.50"));
	}

	static Product product(String sku, BigDecimal price) {
		return new Product(category(), sku, "Keyboard", "Mechanical keyboard", price, 10, null);
	}

	static Order order(Product product, BigDecimal shippingCost) {
		return Order.create("ORD-0001", user(), PaymentMethod.CREDIT_CARD, "Tobias Galeano", "+595981000000",
				"Main Street 123", "Asuncion", null, shippingCost, product, 1);
	}
}
