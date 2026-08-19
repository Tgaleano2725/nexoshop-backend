package com.tobiasgaleano.nexoshop.model.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tobiasgaleano.nexoshop.model.enums.UserRole;
import com.tobiasgaleano.nexoshop.model.valueobject.PasswordHash;

class UserTest {

	@Test
	void normalizesEmailWithTrimAndLocaleIndependentLowercase() {
		User user = new User("Tobias", "Galeano", "  Tobias.Example@MAIL.COM  ", TestData.passwordHash(),
				UserRole.CUSTOMER);

		assertThat(user.getEmail()).isEqualTo("tobias.example@mail.com");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			" ", "invalid-email", "a..b@example.com", "user@example..com", ".user@example.com",
			"user.@example.com", "user@.example.com", "user@example.com.", "user example@example.com",
			"@example.com", "user@"
	})
	void rejectsInvalidEmail(String email) {
		assertThatThrownBy(() -> new User("Tobias", "Galeano", email, TestData.passwordHash(),
				UserRole.CUSTOMER))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsAnObviouslyOversizedEmail() {
		String email = "a".repeat(65) + "@example.com";

		assertThatThrownBy(() -> new User("Tobias", "Galeano", email, TestData.passwordHash(),
				UserRole.CUSTOMER))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void doesNotExposePasswordHashThroughPublicGetter() {
		assertThat(User.class.getMethods())
				.extracting(Method::getName)
				.doesNotContain("getPasswordHash");
	}

	@Test
	void requiresPasswordHashValueObject() {
		assertThatThrownBy(() -> new User("Tobias", "Galeano", "user@example.com", null, UserRole.CUSTOMER))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(new User("Tobias", "Galeano", "user@example.com",
				PasswordHash.fromEncoded("$2b$12$" + "A".repeat(53)), UserRole.CUSTOMER))
				.isNotNull();
	}

	@Test
	void updatesNameAndControlsActivation() {
		User user = new User("Tobias", "Galeano", "user@example.com", TestData.passwordHash(),
				UserRole.CUSTOMER);

		user.updateName(" Ada ", " Lovelace ");
		user.deactivate();

		assertThat(user.getFirstName()).isEqualTo("Ada");
		assertThat(user.getLastName()).isEqualTo("Lovelace");
		assertThat(user.isActive()).isFalse();

		user.activate();
		assertThat(user.isActive()).isTrue();
	}
}
