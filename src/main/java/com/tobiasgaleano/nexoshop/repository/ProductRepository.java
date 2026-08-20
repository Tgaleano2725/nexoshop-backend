package com.tobiasgaleano.nexoshop.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.tobiasgaleano.nexoshop.model.entity.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@EntityGraph(attributePaths = "category")
	Optional<Product> findBySku(String sku);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "category")
	Optional<Product> findLockedById(Long id);

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, Long id);

	@Override
	@EntityGraph(attributePaths = "category")
	Optional<Product> findById(Long id);

	@Override
	@EntityGraph(attributePaths = "category")
	Page<Product> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "category")
	Page<Product> findByActiveTrue(Pageable pageable);
}
