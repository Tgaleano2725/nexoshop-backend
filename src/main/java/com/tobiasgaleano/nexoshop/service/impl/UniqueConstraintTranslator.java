package com.tobiasgaleano.nexoshop.service.impl;

import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;

final class UniqueConstraintTranslator {

	private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

	private UniqueConstraintTranslator() {
	}

	static boolean isUniqueViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof SQLException sqlException
					&& UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
				return true;
			}
			cause = cause.getCause();
		}
		return false;
	}
}
