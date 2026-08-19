package com.tobiasgaleano.nexoshop.validation;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddress {

	private static final int MAX_LENGTH = 150;
	private static final int MAX_LOCAL_PART_LENGTH = 64;
	private static final int MAX_DOMAIN_LABEL_LENGTH = 63;
	private static final Pattern LOCAL_PART_PATTERN = Pattern.compile("[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+");
	private static final Pattern DOMAIN_LABEL_PATTERN = Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?");

	private EmailAddress() {
	}

	public static String normalize(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Email must not be blank");
		}

		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (!isStructurallyValid(normalized)) {
			throw new IllegalArgumentException("Email must be valid");
		}
		return normalized;
	}

	public static boolean isValid(String value) {
		try {
			normalize(value);
			return true;
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private static boolean isStructurallyValid(String value) {
		if (value.length() > MAX_LENGTH || value.chars().anyMatch(Character::isWhitespace)) {
			return false;
		}

		int atIndex = value.indexOf('@');
		if (atIndex <= 0 || atIndex != value.lastIndexOf('@') || atIndex == value.length() - 1) {
			return false;
		}

		String localPart = value.substring(0, atIndex);
		String domain = value.substring(atIndex + 1);
		if (localPart.length() > MAX_LOCAL_PART_LENGTH
				|| localPart.startsWith(".") || localPart.endsWith(".") || localPart.contains("..")
				|| !LOCAL_PART_PATTERN.matcher(localPart).matches()) {
			return false;
		}

		String[] labels = domain.split("\\.", -1);
		if (labels.length < 2) {
			return false;
		}
		for (String label : labels) {
			if (label.isEmpty() || label.length() > MAX_DOMAIN_LABEL_LENGTH
					|| !DOMAIN_LABEL_PATTERN.matcher(label).matches()) {
				return false;
			}
		}
		return true;
	}
}
