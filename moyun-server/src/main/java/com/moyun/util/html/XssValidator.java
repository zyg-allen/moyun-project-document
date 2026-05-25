package com.moyun.util.html;

import com.moyun.util.string.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class XssValidator implements ConstraintValidator<Xss, String> {
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*?>[\\s\\S]*?</script>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_END_PATTERN = Pattern.compile("<script[^>]*?/>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_PATTERN = Pattern.compile("<style[^>]*?>[\\s\\S]*?</style>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_END_PATTERN = Pattern.compile("<style[^>]*?/>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IFRAME_PATTERN = Pattern.compile("<iframe[^>]*?>[\\s\\S]*?</iframe>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IFRAME_END_PATTERN = Pattern.compile("<iframe[^>]*?/>",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void initialize(Xss xss) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        value = value.replaceAll("[<>]", "");
        return !containsScriptOrStyle(value);
    }

    private boolean containsScriptOrStyle(String value) {
        return SCRIPT_PATTERN.matcher(value).find() || SCRIPT_END_PATTERN.matcher(value).find()
                || STYLE_PATTERN.matcher(value).find() || STYLE_END_PATTERN.matcher(value).find()
                || IFRAME_PATTERN.matcher(value).find() || IFRAME_END_PATTERN.matcher(value).find();
    }
}
