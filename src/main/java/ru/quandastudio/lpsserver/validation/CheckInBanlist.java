package ru.quandastudio.lpsserver.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Retention(RUNTIME)
@Target(FIELD)
@Constraint(validatedBy = BanlistValidator.class)
public @interface CheckInBanlist {
	String message() default "{value.bad_login}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
