package com.tobiasgaleano.nexoshop.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.tobiasgaleano.nexoshop.dto.request.order.CreateOrderRequest;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.mapper.OrderMapper;
import com.tobiasgaleano.nexoshop.model.entity.BaseEntity;
import com.tobiasgaleano.nexoshop.model.entity.Cart;
import com.tobiasgaleano.nexoshop.model.entity.Category;
import com.tobiasgaleano.nexoshop.model.entity.Order;
import com.tobiasgaleano.nexoshop.model.entity.Product;
import com.tobiasgaleano.nexoshop.model.entity.User;
import com.tobiasgaleano.nexoshop.model.enums.PaymentMethod;
import com.tobiasgaleano.nexoshop.model.enums.UserRole;
import com.tobiasgaleano.nexoshop.model.valueobject.PasswordHash;
import com.tobiasgaleano.nexoshop.repository.CartRepository;
import com.tobiasgaleano.nexoshop.repository.OrderRepository;
import com.tobiasgaleano.nexoshop.repository.ProductRepository;
import com.tobiasgaleano.nexoshop.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
	@Mock UserRepository userRepository;
	@Mock CartRepository cartRepository;
	@Mock ProductRepository productRepository;
	@Mock OrderRepository orderRepository;
	@Mock OrderMapper orderMapper;
	private OrderServiceImpl service;

	@BeforeEach void setUp() {
		service = new OrderServiceImpl(userRepository, cartRepository, productRepository, orderRepository, orderMapper);
	}

	@Test void checkoutSnapshotsAndConsumesCartAndStockAtomically() {
		User user = entity(new User("Tobias", "Galeano", "order@example.com",
				PasswordHash.fromEncoded("$2b$12$" + "A".repeat(53)), UserRole.CUSTOMER), 1L);
		Product product = product(2L, 8);
		Cart cart = entity(new Cart(user), 3L);
		cart.addProduct(product, 2);
		when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
		when(cartRepository.findLockedByUserId(1L)).thenReturn(Optional.of(cart));
		when(cartRepository.findDetailedById(3L)).thenReturn(Optional.of(cart));
		when(productRepository.findLockedById(2L)).thenReturn(Optional.of(product));
		when(productRepository.decrementStockIfAvailable(2L, 2)).thenReturn(1);
		when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> entity(inv.getArgument(0), 9L));

		service.checkout(1L, new CreateOrderRequest(PaymentMethod.CREDIT_CARD, new BigDecimal("2.00"),
				"Tobias Galeano", "+595981000000", "Main 123", "Asuncion", null));

		assertThat(product.getStock()).isEqualTo(6);
		assertThat(cart.getItems()).isEmpty();
		verify(orderRepository).saveAndFlush(any(Order.class));
	}

	@Test void rejectsEmptyCartBeforeInventoryAccess() {
		User user = entity(new User("Tobias", "Galeano", "empty@example.com",
				PasswordHash.fromEncoded("$2b$12$" + "A".repeat(53)), UserRole.CUSTOMER), 1L);
		Cart cart = entity(new Cart(user), 3L);
		when(userRepository.findLockedById(1L)).thenReturn(Optional.of(user));
		when(cartRepository.findLockedByUserId(1L)).thenReturn(Optional.of(cart));
		when(cartRepository.findDetailedById(3L)).thenReturn(Optional.of(cart));
		assertThatThrownBy(() -> service.checkout(1L, new CreateOrderRequest(PaymentMethod.CREDIT_CARD,
				BigDecimal.ZERO, "Tobias", "1", "A", "B", null))).isInstanceOf(BusinessRuleException.class);
	}

	@Test void cancellationRestoresStockOnlyOnce() {
		User user = entity(new User("Tobias", "Galeano", "cancel@example.com",
				PasswordHash.fromEncoded("$2b$12$" + "A".repeat(53)), UserRole.CUSTOMER), 1L);
		Product product = product(2L, 8);
		Order order = entity(Order.create("NX-TEST", user, PaymentMethod.CREDIT_CARD, "Tobias", "1", "A", "B",
				null, BigDecimal.ZERO, product, 2), 9L);
		when(orderRepository.findLockedByIdAndUserId(9L, 1L)).thenReturn(Optional.of(order));
		when(orderRepository.findDetailedById(9L)).thenReturn(Optional.of(order));
		when(productRepository.findLockedById(2L)).thenReturn(Optional.of(product));
		service.cancel(1L, 9L);
		assertThat(product.getStock()).isEqualTo(10);
		assertThatThrownBy(() -> service.cancel(1L, 9L)).isInstanceOf(BusinessRuleException.class);
		assertThat(product.getStock()).isEqualTo(10);
	}

	private static Product product(Long id, int stock) {
		return entity(new Product(new Category("Cat-" + id, null), "SKU-" + id, "Product", null,
				new BigDecimal("10.00"), stock, null), id);
	}
	private static <T extends BaseEntity> T entity(T value, Long id) {
		ReflectionTestUtils.setField(value, "id", id);
		return value;
	}
}
