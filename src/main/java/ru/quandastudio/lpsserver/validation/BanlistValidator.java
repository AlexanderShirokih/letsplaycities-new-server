package ru.quandastudio.lpsserver.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.quandastudio.lpsserver.core.Banlist;

public class BanlistValidator implements ConstraintValidator<CheckInBanlist, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return Banlist.checkLogin(value);
	}

}
