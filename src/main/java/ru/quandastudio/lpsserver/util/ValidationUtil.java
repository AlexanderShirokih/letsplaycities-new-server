package ru.quandastudio.lpsserver.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public final class ValidationUtil {

	public static <T> Optional<String> validateMessage(T msg) {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();

		Set<ConstraintViolation<T>> v = validator.validate(msg);
		Iterator<ConstraintViolation<T>> validates = v.iterator();

		if (validates.hasNext()) {
			ConstraintViolation<T> cv = validates.next();
			return Optional
					.of(String.format("%s (%s): %s", cv.getPropertyPath(), cv.getInvalidValue(), cv.getMessage()));
		}
		return Optional.empty();

	}
}
