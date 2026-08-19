package com.tobiasgaleano.nexoshop.model.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(name = "uk_carts_user_id", columnNames = "user_id"))
public class Cart extends BaseEntity {

	@NotNull
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@OneToMany(mappedBy = "cart", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CartItem> items = new ArrayList<>();

	protected Cart() {
	}

	public Cart(User user) {
		this.user = requireNonNull(user, "User");
	}

	public CartItem addProduct(Product product, int quantity) {
		requirePositiveQuantity(quantity);
		Product requiredProduct = requireNonNull(product, "Product");
		CartItem existingItem = findItem(requiredProduct);
		if (existingItem != null) {
			existingItem.increaseQuantity(quantity);
			return existingItem;
		}

		CartItem newItem = new CartItem(this, requiredProduct, quantity);
		items.add(newItem);
		return newItem;
	}

	public void changeQuantity(Product product, int quantity) {
		requirePositiveQuantity(quantity);
		CartItem item = requireItem(product);
		item.changeQuantity(quantity);
	}

	public void removeProduct(Product product) {
		CartItem item = requireItem(product);
		items.remove(item);
		item.detachFromCart();
	}

	public User getUser() {
		return user;
	}

	public List<CartItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	private CartItem requireItem(Product product) {
		Product requiredProduct = requireNonNull(product, "Product");
		CartItem item = findItem(requiredProduct);
		if (item == null) {
			throw new IllegalArgumentException("Product is not present in the cart");
		}
		return item;
	}

	private CartItem findItem(Product product) {
		return items.stream()
				.filter(item -> item.references(product))
				.findFirst()
				.orElse(null);
	}

	private static void requirePositiveQuantity(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than zero");
		}
	}

	private static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw new IllegalArgumentException(field + " must not be null");
		}
		return value;
	}
}
