package com.tobiasgaleano.nexoshop.model.entity;

import java.math.BigDecimal;
import java.util.Objects;

import com.tobiasgaleano.nexoshop.validation.MonetaryAmount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(
		name = "uk_cart_items_cart_product", columnNames = { "cart_id", "product_id" }))
public class CartItem extends BaseEntity {

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Positive
	@Column(name = "quantity", nullable = false)
	private int quantity;

	protected CartItem() {
	}

	CartItem(Cart cart, Product product, int quantity) {
		this.cart = requireNonNull(cart, "Cart");
		this.product = requireNonNull(product, "Product");
		changeQuantity(quantity);
	}

	void increaseQuantity(int quantity) {
		requirePositiveQuantity(quantity);
		this.quantity = Math.addExact(this.quantity, quantity);
	}

	void changeQuantity(int quantity) {
		requirePositiveQuantity(quantity);
		this.quantity = quantity;
	}

	void detachFromCart() {
		this.cart = null;
	}

	boolean references(Product candidate) {
		if (product == candidate) {
			return true;
		}
		return product.getId() != null && candidate.getId() != null
				&& Objects.equals(product.getId(), candidate.getId());
	}

	public Cart getCart() {
		return cart;
	}

	public Product getProduct() {
		return product;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal calculateLineTotal() {
		return MonetaryAmount.multiplyPositive(product.getPrice(), quantity, "Cart line total");
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
