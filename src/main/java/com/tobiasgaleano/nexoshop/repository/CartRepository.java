package com.tobiasgaleano.nexoshop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.tobiasgaleano.nexoshop.model.entity.Cart;

import jakarta.persistence.LockModeType;

public interface CartRepository extends JpaRepository<Cart, Long> {

	boolean existsByUserId(Long userId);

	Optional<Cart> findByUserId(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Cart> findLockedByUserId(Long userId);

	@EntityGraph(attributePaths = { "user", "items", "items.product", "items.product.category" })
	Optional<Cart> findDetailedById(Long id);

	@EntityGraph(attributePaths = { "user", "items", "items.product", "items.product.category" })
	Optional<Cart> findDetailedByUserId(Long userId);
}
