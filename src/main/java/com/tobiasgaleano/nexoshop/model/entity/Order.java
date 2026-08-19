package com.tobiasgaleano.nexoshop.model.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tobiasgaleano.nexoshop.model.enums.OrderStatus;
import com.tobiasgaleano.nexoshop.model.enums.PaymentMethod;
import com.tobiasgaleano.nexoshop.model.enums.PaymentStatus;
import com.tobiasgaleano.nexoshop.validation.MonetaryAmount;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(
		name = "uk_orders_order_number", columnNames = "order_number"))
public class Order extends BaseEntity {

	private static final BigDecimal ZERO = new BigDecimal("0.00");

	@NotBlank
	@Size(max = 30)
	@Column(name = "order_number", nullable = false, length = 30, unique = true)
	private String orderNumber;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private OrderStatus status;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 30)
	private PaymentMethod paymentMethod;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 30)
	private PaymentStatus paymentStatus;

	@NotBlank
	@Size(max = 160)
	@Column(name = "recipient_name", nullable = false, length = 160)
	private String recipientName;

	@NotBlank
	@Size(max = 30)
	@Column(name = "recipient_phone", nullable = false, length = 30)
	private String recipientPhone;

	@NotBlank
	@Size(max = 255)
	@Column(name = "shipping_address", nullable = false, length = 255)
	private String shippingAddress;

	@NotBlank
	@Size(max = 100)
	@Column(name = "shipping_city", nullable = false, length = 100)
	private String shippingCity;

	@Size(max = 255)
	@Column(name = "shipping_reference", length = 255)
	private String shippingReference;

	@NotNull
	@DecimalMin("0.00")
	@Digits(integer = 10, fraction = 2)
	@Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@NotNull
	@DecimalMin("0.00")
	@Digits(integer = 10, fraction = 2)
	@Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
	private BigDecimal shippingCost;

	@NotNull
	@DecimalMin("0.00")
	@Digits(integer = 10, fraction = 2)
	@Column(name = "total", nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
			cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private List<OrderItem> items = new ArrayList<>();

	protected Order() {
	}

	private Order(String orderNumber, User user, PaymentMethod paymentMethod, String recipientName,
			String recipientPhone, String shippingAddress, String shippingCity, String shippingReference,
			BigDecimal shippingCost) {
		this.orderNumber = requireText(orderNumber, "Order number", 30);
		this.user = requireNonNull(user, "User");
		this.paymentMethod = requireNonNull(paymentMethod, "Payment method");
		this.recipientName = requireText(recipientName, "Recipient name", 160);
		this.recipientPhone = requireText(recipientPhone, "Recipient phone", 30);
		this.shippingAddress = requireText(shippingAddress, "Shipping address", 255);
		this.shippingCity = requireText(shippingCity, "Shipping city", 100);
		this.shippingReference = normalizeOptional(shippingReference, 255, "Shipping reference");
		this.shippingCost = MonetaryAmount.requireNonNegative(shippingCost, "Shipping cost");
		this.subtotal = ZERO;
		this.total = this.shippingCost;
		this.status = OrderStatus.PENDING;
		this.paymentStatus = PaymentStatus.PENDING;
	}

	public static Order create(String orderNumber, User user, PaymentMethod paymentMethod, String recipientName,
			String recipientPhone, String shippingAddress, String shippingCity, String shippingReference,
			BigDecimal shippingCost, Product firstProduct, int firstQuantity) {
		Order order = new Order(orderNumber, user, paymentMethod, recipientName, recipientPhone, shippingAddress,
				shippingCity, shippingReference, shippingCost);
		order.addItem(firstProduct, firstQuantity);
		return order;
	}

	public OrderItem addItem(Product product, int quantity) {
		if (status != OrderStatus.PENDING) {
			throw new IllegalStateException("Items can only be added to a pending order");
		}
		OrderItem item = new OrderItem(this, requireNonNull(product, "Product"), quantity);
		BigDecimal newSubtotal = MonetaryAmount.addNonNegative(subtotal, item.getLineTotal(), "Subtotal");
		BigDecimal newTotal = MonetaryAmount.addNonNegative(newSubtotal, shippingCost, "Total");
		items.add(item);
		subtotal = newSubtotal;
		total = newTotal;
		return item;
	}

	public void confirm() {
		ensureHasItems();
		transitionFrom(OrderStatus.PENDING, OrderStatus.CONFIRMED);
	}

	public void startPreparing() {
		transitionFrom(OrderStatus.CONFIRMED, OrderStatus.PREPARING);
	}

	public void ship() {
		transitionFrom(OrderStatus.PREPARING, OrderStatus.SHIPPED);
	}

	public void deliver() {
		transitionFrom(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
	}

	public void cancel() {
		ensureHasItems();
		if (status != OrderStatus.PENDING && status != OrderStatus.CONFIRMED) {
			throw new IllegalStateException("Only pending or confirmed orders can be cancelled");
		}
		status = OrderStatus.CANCELLED;
	}

	public void markPaymentPaid() {
		if (paymentStatus != PaymentStatus.PENDING && paymentStatus != PaymentStatus.FAILED) {
			throw new IllegalStateException("Only pending or failed payments can be marked as paid");
		}
		paymentStatus = PaymentStatus.PAID;
	}

	public void markPaymentFailed() {
		if (paymentStatus != PaymentStatus.PENDING) {
			throw new IllegalStateException("Only pending payments can be marked as failed");
		}
		paymentStatus = PaymentStatus.FAILED;
	}

	public void refundPayment() {
		if (paymentStatus != PaymentStatus.PAID) {
			throw new IllegalStateException("Only paid payments can be refunded");
		}
		paymentStatus = PaymentStatus.REFUNDED;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public User getUser() {
		return user;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getRecipientPhone() {
		return recipientPhone;
	}

	public String getShippingAddress() {
		return shippingAddress;
	}

	public String getShippingCity() {
		return shippingCity;
	}

	public String getShippingReference() {
		return shippingReference;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getShippingCost() {
		return shippingCost;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public List<OrderItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	private void transitionFrom(OrderStatus expected, OrderStatus target) {
		if (status != expected) {
			throw new IllegalStateException("Order cannot transition from " + status + " to " + target);
		}
		status = target;
	}

	@PrePersist
	@PreUpdate
	private void validateAggregateForPersistence() {
		ensureHasItems();
		BigDecimal calculatedSubtotal = ZERO;
		for (OrderItem item : items) {
			calculatedSubtotal = MonetaryAmount.addNonNegative(
					calculatedSubtotal, item.getLineTotal(), "Subtotal");
		}
		BigDecimal calculatedTotal = MonetaryAmount.addNonNegative(calculatedSubtotal, shippingCost, "Total");
		if (calculatedSubtotal.compareTo(subtotal) != 0 || calculatedTotal.compareTo(total) != 0) {
			throw new IllegalStateException("Order totals are inconsistent with its items and shipping cost");
		}
	}

	private void ensureHasItems() {
		if (items.isEmpty()) {
			throw new IllegalStateException("An order must contain at least one item");
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

	private static String normalizeOptional(String value, int maxLength, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
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
