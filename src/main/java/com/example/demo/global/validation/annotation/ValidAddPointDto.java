package com.example.demo.global.validation.annotation;

import com.example.demo.global.validation.validator.TrustScoreUpdateRequestValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TrustScoreUpdateRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAddPointDto {
    String message() default "Invalid AddPointDto";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
