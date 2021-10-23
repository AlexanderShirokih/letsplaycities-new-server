package ru.quandastudio.lpsserver.validation

import ru.quandastudio.lpsserver.core.ContextProvider
import ru.quandastudio.lpsserver.data.ForbiddenLoginManager
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class BanlistValidator : ConstraintValidator<CheckInBanlist, String?> {

    override fun initialize(constraintAnnotation: CheckInBanlist?) {
        super.initialize(constraintAnnotation)
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        val forbiddenLoginManager = ContextProvider.getBean(ForbiddenLoginManager::class.java)

        return value == null || forbiddenLoginManager.isLoginValid(value)
    }
}