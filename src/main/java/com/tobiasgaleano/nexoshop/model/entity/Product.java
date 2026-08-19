package com.tobiasgaleano.nexoshop.model.entity;

import java.math.BigDecimal;

import com.tobiasgaleano.nexoshop.validation.MonetaryAmount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uk_products_sku", columnNames = "sku"))
public class Product extends BaseEntity {

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@NotBlank
	@Size(max = 50)
	@Column(name = "sku", nullable = false, length = 50, unique = true)
	private String sku;

	@NotBlank
	@Size(max = 150)
	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@NotNull
	@DecimalMin(value = "0.00", inclusive = false)
	@Digits(integer = 10, fraction = 2)
	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@PositiveOrZero
	@Column(name = "stock", nullable = false)
	private int stock;

	@Size(max = 500)
	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "active", nullable = false)
	private boolean active;

	protected Product() {
	}

	public Product(Category category, String sku, String name, String description, BigDecimal price, int stock,
			String imageUrl) {
		this.category = requireNonNull(category, "Category");
		this.sku = requireText(sku, "SKU", 50);
		updateDescriptiveData(name, description, imageUrl);
		changePrice(price);
		if (stock < 0) {
			throw new IllegalArgumentException("Stock must not be negative");
		}
		this.stock = stock;
		this.active = true;
	}

	public void updateDescriptiveData(String name, String description, String imageUrl) {
		this.name = requireText(name, "Product name", 150);
		this.description = normalizeOptional(description, null, "Description");
		this.imageUrl = normalizeOptional(imageUrl, 500, "Image URL");
	}

	public void changePrice(BigDecimal price) {
		this.price = MonetaryAmount.requirePositive(price, "Price");
	}

	public void increaseStock(int quantity) {
		requirePositiveQuantity(quantity);
		this.stock = Math.addExact(this.stock, quantity);
	}

	public void decreaseStock(int quantity) {
		requirePositiveQuantity(quantity);
		if (quantity > stock) {
			throw new IllegalArgumentException("Insufficient stock");
		}
		this.stock -= quantity;
	}

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
	}

	public Category getCategory() {
		return category;
	}

	public String getSku() {
		return sku;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getStock() {
		return stock;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public boolean isActive() {
		return active;
	}

	private static void requirePositiveQuantity(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than zero");
		}
	}

	private static String requireText(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	private static String normalizeOptional(String value, Integer maxLength, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (maxLength != null && normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	private static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw new IllegalArgumentException(field + " must not be null");
		}
		return value;
	}
}
