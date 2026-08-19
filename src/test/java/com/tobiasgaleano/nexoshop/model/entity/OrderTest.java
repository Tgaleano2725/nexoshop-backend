package com.tobiasgaleano.nexoshop.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tobiasgaleano.nexoshop.model.enums.OrderStatus;
import com.tobiasgaleano.nexoshop.model.enums.PaymentMethod;
import com.tobiasgaleano.nexoshop.model.enums.PaymentStatus;

class OrderTest {

	@Test
	void capturesImmutableProductSnapshotAndCalculatesTotals() {
		Product product = product();
		Order order = order(product, new BigDecimal("10.00"));

		OrderItem item = order.getItems().getFirst();
		order.addItem(product, 1);
		product.updateDescriptiveData("Updated name", null, null);
		product.changePrice(new BigDecimal("99.00"));

		assertThat(item.getProductSku()).isEqualTo("SKU-1");
		assertThat(item.getProductName()).isEqualTo("Keyboard");
		assertThat(item.getUnitPrice()).isEqualByComparingTo("25.50");
		assertThat(item.getLineTotal()).isEqualByComparingTo("25.50");
		assertThat(order.getSubtotal()).isEqualByComparingTo("51.00");
		assertThat(order.getTotal()).isEqualByComparingTo("61.00");
	}

	@Test
	void followsCompleteValidOrderTransitionSequence() {
		Order order = order(product(), BigDecimal.ZERO);

		order.confirm();
		order.startPreparing();
		order.ship();
		order.deliver();

		assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
	}

	@Test
	void permitsCancellationOnlyFromPendingOrConfirmed() {
		Order pending = order(product(), BigDecimal.ZERO);
		pending.cancel();
		assertThat(pending.getStatus()).isEqualTo(OrderStatus.CANCELLED);

		Order confirmed = order(product(), BigDecimal.ZERO);
		confirmed.confirm();
		confirmed.cancel();
		assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void rejectsInvalidOrderTransitionsAndChangesAfterConfirmation() {
		Order empty = new Order();
		assertThatThrownBy(empty::confirm).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(empty::cancel).isInstanceOf(IllegalStateException.class);

		Order confirmed = order(product(), BigDecimal.ZERO);
		confirmed.confirm();
		assertThatThrownBy(() -> confirmed.addItem(product(), 1))
				.isInstanceOf(IllegalStateException.class);
		confirmed.startPreparing();
		assertThatThrownBy(confirmed::cancel).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void controlsPaymentStatusTransitions() {
		Order paid = order(product(), BigDecimal.ZERO);
		paid.markPaymentPaid();
		paid.refundPayment();
		assertThat(paid.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

		Order recovered = order(product(), BigDecimal.ZERO);
		recovered.markPaymentFailed();
		recovered.markPaymentPaid();
		assertThat(recovered.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
	}

	@Test
	void rejectsInvalidPaymentTransitions() {
		Order order = order(product(), BigDecimal.ZERO);

		assertThatThrownBy(order::refundPayment).isInstanceOf(IllegalStateException.class);
		order.markPaymentPaid();
		assertThatThrownBy(order::markPaymentFailed).isInstanceOf(IllegalStateException.class);
		order.refundPayment();
		assertThatThrownBy(order::markPaymentPaid).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void rejectsInvalidAmountsAndItemQuantities() {
		assertThatThrownBy(() -> order(product(), new BigDecimal("-0.01")))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> Order.create("ORD-0001", TestData.user(),
				PaymentMethod.CREDIT_CARD, "Tobias Galeano",
				"+595981000000", "Main Street 123", "Asuncion", null, BigDecimal.ZERO, product(), 0))
				.isInstanceOf(IllegalArgumentException.class);

		Order order = order(product(), BigDecimal.ZERO);
		assertThatThrownBy(() -> order.getItems().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void hasNoPublicApiThatCreatesAnEmptyOrder() {
		assertThat(Order.class.getConstructors()).isEmpty();

		Order order = order(product(), BigDecimal.ZERO);
		assertThat(order.getItems()).hasSize(1);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
	}

	@Test
	void rejectsLineTotalSubtotalAndTotalOverflowAtomically() {
		Product maximum = TestData.product("SKU-MAX", new BigDecimal("9999999999.99"));
		assertThatThrownBy(() -> Order.create("ORD-LINE", TestData.user(),
				PaymentMethod.CREDIT_CARD, "Tobias Galeano",
				"+595981000000", "Main Street 123", "Asuncion", null, BigDecimal.ZERO, maximum, 2))
				.isInstanceOf(IllegalArgumentException.class);

		Product half = TestData.product("SKU-HALF", new BigDecimal("5000000000.00"));
		Order order = order(half, BigDecimal.ZERO);
		assertThatThrownBy(() -> order.addItem(half, 1))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(order.getItems()).hasSize(1);
		assertThat(order.getSubtotal()).isEqualByComparingTo("5000000000.00");

		assertThatThrownBy(() -> order(maximum, new BigDecimal("0.01")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void calculatesExactRepresentableAmounts() {
		Product product = TestData.product("SKU-EXACT", new BigDecimal("0.10"));
		Order order = Order.create("ORD-EXACT", TestData.user(),
				PaymentMethod.CREDIT_CARD, "Tobias Galeano",
				"+595981000000", "Main Street 123", "Asuncion", null, new BigDecimal("0.20"), product, 3);

		assertThat(order.getItems().getFirst().getLineTotal()).isEqualTo(new BigDecimal("0.30"));
		assertThat(order.getSubtotal()).isEqualTo(new BigDecimal("0.30"));
		assertThat(order.getTotal()).isEqualTo(new BigDecimal("0.50"));
	}

	private static Order order(Product product, BigDecimal shippingCost) {
		return TestData.order(product, shippingCost);
	}

	private static Product product() {
		return TestData.product();
	}
}
