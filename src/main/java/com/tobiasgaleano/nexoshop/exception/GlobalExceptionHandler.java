package com.tobiasgaleano.nexoshop.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.tobiasgaleano.nexoshop.dto.response.error.ErrorResponse;
import com.tobiasgaleano.nexoshop.dto.response.error.FieldErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException e, HttpServletRequest r) { return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage(), r, List.of()); }
	@ExceptionHandler(DuplicateResourceException.class) ResponseEntity<ErrorResponse> duplicate(DuplicateResourceException e, HttpServletRequest r) { return error(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", e.getMessage(), r, List.of()); }
	@ExceptionHandler(BusinessRuleException.class) ResponseEntity<ErrorResponse> business(BusinessRuleException e, HttpServletRequest r) { return error(HttpStatus.CONFLICT, "BUSINESS_RULE", e.getMessage(), r, List.of()); }
	@ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e, HttpServletRequest r) {
		List<FieldErrorResponse> fields = e.getBindingResult().getFieldErrors().stream().map(x -> new FieldErrorResponse(x.getField(), x.getDefaultMessage())).toList();
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", r, fields);
	}
	@ExceptionHandler({ HandlerMethodValidationException.class, ConstraintViolationException.class }) ResponseEntity<ErrorResponse> constraint(Exception e, HttpServletRequest r) { return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", r, List.of()); }
	@ExceptionHandler({ HttpMessageNotReadableException.class, IllegalArgumentException.class }) ResponseEntity<ErrorResponse> malformed(Exception e, HttpServletRequest r) { return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body or parameter is invalid", r, List.of()); }
	@ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ErrorResponse> integrity(HttpServletRequest r) { return error(HttpStatus.CONFLICT, "INTEGRITY_VIOLATION", "Request conflicts with existing data", r, List.of()); }
	@ExceptionHandler(Exception.class) ResponseEntity<ErrorResponse> unexpected(HttpServletRequest r) { return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", r, List.of()); }

	private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, HttpServletRequest request, List<FieldErrorResponse> fields) {
		return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), code, message, request.getRequestURI(), fields));
	}
}
