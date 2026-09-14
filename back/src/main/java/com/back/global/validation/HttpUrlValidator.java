package com.back.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class HttpUrlValidator implements ConstraintValidator<ValidHttpUrl, String> {

    private static final Pattern HTTP_URL = Pattern.compile("(?i)^https?://.+");

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {
        // null과 빈 값은 @NotBlank가 별도로 처리한다. 선택 필드라 @NotBlank가 없으면 그대로 통과.
        return url == null || url.isBlank() || HTTP_URL.matcher(url).matches();
    }
}
