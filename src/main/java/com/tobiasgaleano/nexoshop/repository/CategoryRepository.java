package com.tobiasgaleano.nexoshop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tobiasgaleano.nexoshop.model.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Optional<Category> findByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	List<Category> findAllByOrderByNameAscIdAsc();

	List<Category> findByActiveTrueOrderByNameAscIdAsc();
}
