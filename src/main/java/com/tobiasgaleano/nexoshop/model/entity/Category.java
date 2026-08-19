package com.tobiasgaleano.nexoshop.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(name = "uk_categories_name", columnNames = "name"))
public class Category extends BaseEntity {

	@NotBlank
	@Size(max = 100)
	@Column(name = "name", nullable = false, length = 100, unique = true)
	private String name;

	@Size(max = 500)
	@Column(name = "description", length = 500)
	private String description;

	@Column(name = "active", nullable = false)
	private boolean active;

	protected Category() {
	}

	public Category(String name, String description) {
		updateDetails(name, description);
		this.active = true;
	}

	public void updateDetails(String name, String description) {
		this.name = requireName(name);
		this.description = normalizeOptional(description, 500, "Description");
	}

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isActive() {
		return active;
	}

	private static String requireName(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Category name must not be blank");
		}
		String normalized = value.trim();
		if (normalized.length() > 100) {
			throw new IllegalArgumentException("Category name must not exceed 100 characters");
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
}
