package ru.quandastudio.lpsserver.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import ru.quandastudio.lpsserver.netty.models.LPSClientMessage;

public final class ValidationUtil {

	public static Optional<String> validateMessage(LPSClientMessage msg) {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();

		Set<ConstraintViolation<LPSClientMessage>> v = validator.validate(msg);
		Iterator<ConstraintViolation<LPSClientMessage>> validates = v.iterator();

		if (validates.hasNext())
			return Optional.of(validates.next().getMessage());
		return Optional.empty();

	}
}
