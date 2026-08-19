package com.tobiasgaleano.nexoshop.model.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class PasswordHashTest {

	@Test
	void acceptsSyntacticallyValidBcryptEncoding() {
		assertThat(PasswordHash.fromEncoded("$2b$12$" + "A".repeat(53))).isNotNull();
	}

	@Test
	void rejectsPlainIncompleteOrInvalidBcryptEncoding() {
		assertThatThrownBy(() -> PasswordHash.fromEncoded("plain-text-password"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> PasswordHash.fromEncoded("$2b$12$short"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> PasswordHash.fromEncoded("$2b$03$" + "A".repeat(53)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void exposesNoDeclaredStringReturningMethod() {
		assertThat(PasswordHash.class.getDeclaredMethods())
				.filteredOn(method -> method.getReturnType().equals(String.class))
				.extracting(Method::getName)
				.isEmpty();
	}
}
