package com.tobiasgaleano.nexoshop.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return EmailAddress.isValid(value);
	}
}
