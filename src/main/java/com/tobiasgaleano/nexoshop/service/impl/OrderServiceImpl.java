package com.tobiasgaleano.nexoshop.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tobiasgaleano.nexoshop.dto.request.order.CreateOrderRequest;
import com.tobiasgaleano.nexoshop.dto.response.order.OrderResponse;
import com.tobiasgaleano.nexoshop.exception.BusinessRuleException;
import com.tobiasgaleano.nexoshop.exception.ResourceNotFoundException;
import com.tobiasgaleano.nexoshop.mapper.OrderMapper;
import com.tobiasgaleano.nexoshop.model.entity.Cart;
import com.tobiasgaleano.nexoshop.model.entity.CartItem;
import com.tobiasgaleano.nexoshop.model.entity.Order;
import com.tobiasgaleano.nexoshop.model.entity.Product;
import com.tobiasgaleano.nexoshop.model.entity.User;
import com.tobiasgaleano.nexoshop.repository.CartRepository;
import com.tobiasgaleano.nexoshop.repository.OrderRepository;
import com.tobiasgaleano.nexoshop.repository.ProductRepository;
import com.tobiasgaleano.nexoshop.repository.UserRepository;
import com.tobiasgaleano.nexoshop.service.OrderService;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
	private final UserRepository userRepository;
	private final CartRepository cartRepository;
	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;

	public OrderServiceImpl(UserRepository userRepository, CartRepository cartRepository,
			ProductRepository productRepository, OrderRepository orderRepository, OrderMapper orderMapper) {
		this.userRepository = userRepository;
		this.cartRepository = cartRepository;
		this.productRepository = productRepository;
		this.orderRepository = orderRepository;
		this.orderMapper = orderMapper;
	}

	@Override
	@Transactional
	public OrderResponse checkout(Long userId, CreateOrderRequest request) {
		if (request == null) throw new BusinessRuleException("Create order request must not be null");
		User user = requireActiveUser(userId, true);
		Cart cart = cartRepository.findLockedByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart for user", userId));
		Long cartId = cart.getId();
		cart = cartRepository.findDetailedById(cartId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart", cartId));
		if (cart.getItems().isEmpty()) throw new BusinessRuleException("Cannot checkout an empty cart");

		Map<Long, Product> lockedProducts = lockProducts(cart.getItems());
		Order order = null;
		try {
			for (CartItem item : cart.getItems()) {
				Product product = lockedProducts.get(item.getProduct().getId());
				validateProduct(product, item.getQuantity());
				if (productRepository.decrementStockIfAvailable(product.getId(), item.getQuantity()) != 1) {
					throw new BusinessRuleException("Insufficient stock");
				}
				product.decreaseStock(item.getQuantity());
				if (order == null) {
					order = Order.create(generateOrderNumber(), user, request.paymentMethod(), request.recipientName(),
							request.recipientPhone(), request.shippingAddress(), request.shippingCity(), request.shippingReference(),
							request.shippingCost(), product, item.getQuantity());
				} else {
					order.addItem(product, item.getQuantity());
				}
			}
			Order saved = orderRepository.saveAndFlush(order);
			cart.clear();
			cartRepository.flush();
			return orderMapper.toResponse(saved);
		} catch (IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
			throw new BusinessRuleException(exception.getMessage(), exception);
		}
	}

	@Override
	public OrderResponse getById(Long userId, Long orderId) {
		return orderRepository.findDetailedByIdAndUserId(orderId, userId)
				.map(orderMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
	}

	@Override
	public Page<OrderResponse> listByUser(Long userId, Pageable pageable) {
		requireActiveUser(userId, false);
		return orderRepository.findByUserId(userId, pageable).map(orderMapper::toResponse);
	}

	@Override @Transactional public OrderResponse confirm(Long u, Long o) { return transition(u, o, Order::confirm); }
	@Override @Transactional public OrderResponse startPreparing(Long u, Long o) { return transition(u, o, Order::startPreparing); }
	@Override @Transactional public OrderResponse ship(Long u, Long o) { return transition(u, o, Order::ship); }
	@Override @Transactional public OrderResponse deliver(Long u, Long o) { return transition(u, o, Order::deliver); }
	@Override @Transactional public OrderResponse markPaymentPaid(Long u, Long o) { return transition(u, o, Order::markPaymentPaid); }
	@Override @Transactional public OrderResponse markPaymentFailed(Long u, Long o) { return transition(u, o, Order::markPaymentFailed); }
	@Override @Transactional public OrderResponse refund(Long u, Long o) { return transition(u, o, Order::refundPayment); }

	@Override
	@Transactional
	public OrderResponse cancel(Long userId, Long orderId) {
		Order order = lockedOrder(userId, orderId);
		List<Long> ids = order.getItems().stream().map(item -> item.getProduct().getId()).sorted().toList();
		Map<Long, Product> products = new LinkedHashMap<>();
		for (Long id : ids) products.put(id, lockedProduct(id));
		try {
			order.cancel();
			order.getItems().forEach(item -> products.get(item.getProduct().getId()).increaseStock(item.getQuantity()));
			orderRepository.flush();
			return orderMapper.toResponse(order);
		} catch (IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
			throw new BusinessRuleException(exception.getMessage(), exception);
		}
	}

	@FunctionalInterface private interface OrderAction { void apply(Order order); }

	@Transactional
	private OrderResponse transition(Long userId, Long orderId, OrderAction action) {
		Order order = lockedOrder(userId, orderId);
		try { action.apply(order); orderRepository.flush(); return orderMapper.toResponse(order); }
		catch (IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
			throw new BusinessRuleException(exception.getMessage(), exception);
		}
	}

	private Order lockedOrder(Long userId, Long orderId) {
		Order locked = orderRepository.findLockedByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
		return orderRepository.findDetailedById(locked.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
	}

	private User requireActiveUser(Long userId, boolean lock) {
		User user = (lock ? userRepository.findLockedById(userId) : userRepository.findById(userId))
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
		if (!user.isActive()) throw new BusinessRuleException("User must be active");
		return user;
	}

	private Map<Long, Product> lockProducts(List<CartItem> items) {
		List<Long> ids = items.stream().map(item -> item.getProduct().getId()).distinct().sorted().toList();
		Map<Long, Product> result = new LinkedHashMap<>();
		for (Long id : ids) result.put(id, lockedProduct(id));
		return result;
	}

	private Product lockedProduct(Long id) {
		return productRepository.findLockedById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
	}

	private static void validateProduct(Product product, int quantity) {
		if (!product.isActive()) throw new BusinessRuleException("Product must be active");
		if (!product.getCategory().isActive()) throw new BusinessRuleException("Product category must be active");
		if (quantity <= 0 || quantity > product.getStock()) throw new BusinessRuleException("Insufficient stock");
	}

	private static String generateOrderNumber() {
		return "NX" + UUID.randomUUID().toString().replace("-", "").substring(0, 28);
	}
}
