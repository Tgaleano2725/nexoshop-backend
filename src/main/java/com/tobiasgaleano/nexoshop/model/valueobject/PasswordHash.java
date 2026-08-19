package com.tobiasgaleano.nexoshop.model.valueobject;

import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Embeddable
public class PasswordHash {

	private static final Pattern BCRYPT_PATTERN = Pattern.compile(
			"^\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}$");

	@NotBlank
	@Size(min = 60, max = 60)
	@Column(name = "encoded_value", nullable = false, length = 255)
	private String encodedValue;

	protected PasswordHash() {
	}

	private PasswordHash(String encodedValue) {
		this.encodedValue = encodedValue;
	}

	public static PasswordHash fromEncoded(String encodedValue) {
		return requireValid(new PasswordHash(encodedValue));
	}

	public static PasswordHash requireValid(PasswordHash passwordHash) {
		if (passwordHash == null || passwordHash.encodedValue == null
				|| !BCRYPT_PATTERN.matcher(passwordHash.encodedValue).matches()) {
			throw new IllegalArgumentException("Password hash must be a valid BCrypt encoding");
		}
		return passwordHash;
	}
}
