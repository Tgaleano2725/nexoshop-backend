package com.tobiasgaleano.nexoshop.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.tobiasgaleano.nexoshop.model.entity.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@EntityGraph(attributePaths = { "user", "items", "items.product" })
	Optional<Order> findDetailedById(Long id);

	@EntityGraph(attributePaths = { "user", "items", "items.product" })
	Optional<Order> findDetailedByIdAndUserId(Long id, Long userId);

	@EntityGraph(attributePaths = { "user", "items", "items.product" })
	Page<Order> findByUserId(Long userId, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Order> findLockedByIdAndUserId(Long id, Long userId);
}
