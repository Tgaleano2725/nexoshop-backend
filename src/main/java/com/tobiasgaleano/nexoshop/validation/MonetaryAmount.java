package com.tobiasgaleano.nexoshop.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MonetaryAmount {

	public static final int INTEGER_DIGITS = 10;
	public static final int FRACTION_DIGITS = 2;
	public static final int PRECISION = INTEGER_DIGITS + FRACTION_DIGITS;

	private MonetaryAmount() {
	}

	public static BigDecimal requirePositive(BigDecimal value, String field) {
		BigDecimal normalized = requireRepresentable(value, field);
		if (normalized.signum() <= 0) {
			throw new IllegalArgumentException(field + " must be greater than zero");
		}
		return normalized;
	}

	public static BigDecimal requireNonNegative(BigDecimal value, String field) {
		BigDecimal normalized = requireRepresentable(value, field);
		if (normalized.signum() < 0) {
			throw new IllegalArgumentException(field + " must not be negative");
		}
		return normalized;
	}

	public static BigDecimal addNonNegative(BigDecimal left, BigDecimal right, String field) {
		if (left == null || right == null) {
			throw new IllegalArgumentException(field + " operands must not be null");
		}
		return requireNonNegative(left.add(right), field);
	}

	public static BigDecimal multiplyPositive(BigDecimal unitAmount, int quantity, String field) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be greater than zero");
		}
		BigDecimal normalizedUnitAmount = requirePositive(unitAmount, "Unit amount");
		return requirePositive(normalizedUnitAmount.multiply(BigDecimal.valueOf(quantity)), field);
	}

	private static BigDecimal requireRepresentable(BigDecimal value, String field) {
		if (value == null) {
			throw new IllegalArgumentException(field + " must not be null");
		}

		BigDecimal normalized;
		try {
			normalized = value.setScale(FRACTION_DIGITS, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException(field + " must have at most two decimal places", exception);
		}

		if (normalized.precision() > PRECISION) {
			throw new IllegalArgumentException(field + " exceeds NUMERIC(12,2)");
		}
		return normalized;
	}
}
