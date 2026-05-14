package com.ctdi.llmtc.xtp.traininfer.util.validator;

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * @author yangla
 * @since 2025/6/14
 */
public class ValidationUtils {

    //private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private static final Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator()) // 禁用 EL
            .buildValidatorFactory()
            .getValidator();


    /**
     * 手动校验一个对象，并抛出异常（如果有校验错误）
     *
     * @param object 要校验的对象
     * @param groups 校验分组（可选）
     */
    public static void validate(Object object, Class<?>... groups) {
        Set<ConstraintViolation<Object>> violations = validator.validate(object, groups);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("数据校验失败：");
            for (ConstraintViolation<Object> violation : violations) {
                sb.append(violation.getMessage()).append("; ");
            }
            throw new ValidationException(sb.toString());
        }
    }
}
