package com.tobiasgaleano.nexoshop.model.entity;

import java.math.BigDecimal;

import com.tobiasgaleano.nexoshop.validation.MonetaryAmount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@NotBlank
	@Size(max = 50)
	@Column(name = "product_sku", nullable = false, length = 50, updatable = false)
	private String productSku;

	@NotBlank
	@Size(max = 150)
	@Column(name = "product_name", nullable = false, length = 150, updatable = false)
	private String productName;

	@NotNull
	@DecimalMin(value = "0.00", inclusive = false)
	@Digits(integer = 10, fraction = 2)
	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal unitPrice;

	@Positive
	@Column(name = "quantity", nullable = false, updatable = false)
	private int quantity;

	@NotNull
	@DecimalMin(value = "0.00", inclusive = false)
	@Digits(integer = 10, fraction = 2)
	@Column(name = "line_total", nullable = false, precision = 12, scale = 2, updatable = false)
	private BigDecimal lineTotal;

	protected OrderItem() {
	}

	OrderItem(Order order, Product product, int quantity) {
		if (order == null) {
			throw new IllegalArgumentException("Order must not be null");
		}
		if (product == null) {
			throw new IllegalArgumentException("Product must not be null");
		}
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than zero");
		}
		this.order = order;
		this.product = product;
		this.productSku = product.getSku();
		this.productName = product.getName();
		this.unitPrice = MonetaryAmount.requirePositive(product.getPrice(), "Unit price");
		this.quantity = quantity;
		this.lineTotal = MonetaryAmount.multiplyPositive(unitPrice, quantity, "Line total");
	}

	public Order getOrder() {
		return order;
	}

	public Product getProduct() {
		return product;
	}

	public String getProductSku() {
		return productSku;
	}

	public String getProductName() {
		return productName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getLineTotal() {
		return lineTotal;
	}
}
