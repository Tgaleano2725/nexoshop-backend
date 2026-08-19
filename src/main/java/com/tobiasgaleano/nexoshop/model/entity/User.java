package com.tobiasgaleano.nexoshop.model.entity;

import com.tobiasgaleano.nexoshop.model.valueobject.PasswordHash;
import com.tobiasgaleano.nexoshop.model.enums.UserRole;
import com.tobiasgaleano.nexoshop.validation.EmailAddress;
import com.tobiasgaleano.nexoshop.validation.ValidEmail;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User extends BaseEntity {

	@NotBlank
	@Size(max = 80)
	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@NotBlank
	@Size(max = 80)
	@Column(name = "last_name", nullable = false, length = 80)
	private String lastName;

	@NotBlank
	@ValidEmail
	@Size(max = 150)
	@Column(name = "email", nullable = false, length = 150, unique = true)
	private String email;

	@NotNull
	@Valid
	@Embedded
	@AttributeOverride(name = "encodedValue",
			column = @Column(name = "password_hash", nullable = false, length = 255))
	private PasswordHash passwordHash;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role;

	@Column(name = "active", nullable = false)
	private boolean active;

	protected User() {
	}

	public User(String firstName, String lastName, String email, PasswordHash passwordHash, UserRole role) {
		updateName(firstName, lastName);
		this.email = EmailAddress.normalize(email);
		this.passwordHash = PasswordHash.requireValid(passwordHash);
		this.role = requireNonNull(role, "User role");
		this.active = true;
	}

	public void updateName(String firstName, String lastName) {
		this.firstName = requireText(firstName, "First name", 80);
		this.lastName = requireText(lastName, "Last name", 80);
	}

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public UserRole getRole() {
		return role;
	}

	public boolean isActive() {
		return active;
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

	private static <T> T requireNonNull(T value, String field) {
		if (value == null) {
			throw new IllegalArgumentException(field + " must not be null");
		}
		return value;
	}
}
