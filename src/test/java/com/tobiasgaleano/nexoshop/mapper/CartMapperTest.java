package com.tobiasgaleano.nexoshop.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tobiasgaleano.nexoshop.model.entity.Cart;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.model.entity.Product;
import com.tobiasgaleano.nexoshop.model.entity.User;
import com.tobiasgaleano.nexoshop.model.enums.UserRole;
import com.tobiasgaleano.nexoshop.model.valueobject.PasswordHash;

class CartMapperTest {

	private final CartMapper mapper = new CartMapper();

	@Test
	void mapsCurrentProductDataInStableOrderAndImmutableList() {
		Cart cart = new Cart(user());
		cart.addProduct(product("SKU-B", "Keyboard", "25.50", 10), 2);
		cart.addProduct(product("SKU-A", "Mouse", "10.00", 4), 1);

		var response = mapper.toResponse(cart);

		assertThat(response.items()).extracting(item -> item.sku()).containsExactly("SKU-A", "SKU-B");
		assertThat(response.items()).extracting(item -> item.lineTotal())
				.containsExactly(new BigDecimal("10.00"), new BigDecimal("51.00"));
		assertThat(response.lineCount()).isEqualTo(2);
		assertThat(response.totalUnits()).isEqualTo(3);
		assertThat(response.subtotal()).isEqualTo(new BigDecimal("61.00"));
		assertThat(response.items().getFirst().availableStock()).isEqualTo(4);
		assertThatThrownBy(() -> response.items().clear()).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void doesNotExposeEntitiesInResponseRecords() {
		assertThat(com.tobiasgaleano.nexoshop.dto.response.cart.CartResponse.class.getRecordComponents())
				.noneMatch(component -> component.getType().equals(Cart.class));
		assertThat(com.tobiasgaleano.nexoshop.dto.response.cart.CartItemResponse.class.getRecordComponents())
				.noneMatch(component -> component.getType().equals(Product.class));
	}

	private static User user() {
		return new User("Tobias", "Galeano", "user@example.com",
				PasswordHash.fromEncoded("$2b$12$" + "A".repeat(53)), UserRole.CUSTOMER);
	}

	private static Product product(String sku, String name, String price, int stock) {
		return new Product(new Category("Category " + sku, null), sku, name, null,
				new BigDecimal(price), stock, null);
	}
}
