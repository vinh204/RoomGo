package com.example.homestay.utils;

import android.util.Patterns;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class InputValidator {
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private InputValidator() {}

    public enum PasswordStrength { WEAK, MEDIUM, STRONG, VERY_STRONG }

    public static PasswordValidation validatePassword(String password) {
        if (password.length() < 8) return new PasswordValidation(false, PasswordStrength.WEAK);
        int strength = 1;
        if (password.length() >= 12) strength++;
        if (password.chars().anyMatch(Character::isUpperCase)) strength++;
        if (password.chars().anyMatch(Character::isLowerCase)) strength++;
        if (password.chars().anyMatch(Character::isDigit)) strength++;
        if (containsSpecial(password)) strength++;
        String lower = password.toLowerCase(Locale.ROOT);
        if (!(lower.contains("password") || lower.contains("123456") || lower.contains("qwerty") || lower.contains("admin") || lower.contains("user"))) strength++;
        PasswordStrength result = strength <= 3 ? PasswordStrength.WEAK : strength <= 4 ? PasswordStrength.MEDIUM : strength <= 5 ? PasswordStrength.STRONG : PasswordStrength.VERY_STRONG;
        return new PasswordValidation(strength > 3, result);
    }

    public static boolean isPasswordValid(String password) {
        return validatePassword(password).isValid()
            && password.chars().anyMatch(Character::isUpperCase)
            && password.chars().anyMatch(Character::isLowerCase)
            && password.chars().anyMatch(Character::isDigit) && containsSpecial(password);
    }

    public static String getPasswordErrorMessage(String password) {
        if (!validatePassword(password).isValid()) return "Mật khẩu phải có ít nhất 8 ký tự";
        List<String> missing = new ArrayList<>();
        if (!password.chars().anyMatch(Character::isUpperCase)) missing.add("chữ hoa");
        if (!password.chars().anyMatch(Character::isLowerCase)) missing.add("chữ thường");
        if (!password.chars().anyMatch(Character::isDigit)) missing.add("số");
        if (!containsSpecial(password)) missing.add("ký tự đặc biệt (!@#$%^&*()_+-=[]{}|;:,.<>?)");
        return missing.isEmpty() ? "Mật khẩu hợp lệ" : "Mật khẩu cần có: " + String.join(", ", missing);
    }

    public static boolean validateEmail(String email) {
        if (email.isEmpty() || email.length() > 254 || email.contains(" ") || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false;
        String[] parts = email.split("@", -1);
        if (parts.length != 2 || parts[0].isEmpty() || parts[0].length() > 64) return false;
        return parts[1].contains(".") && !parts[1].startsWith(".") && !parts[1].endsWith(".");
    }

    public static boolean validatePhoneNumber(String phone) {
        if (phone.isEmpty()) return false;
        String cleaned = phone.replace(" ", "").replace("-", "");
        return cleaned.matches("^0[0-9]{9}$") || cleaned.matches("^0[0-9]{10}$") || cleaned.matches("^\\+84[0-9]{9,10}$");
    }
    public static String normalizePhoneNumber(String phone) {
        String cleaned = phone.replace(" ", "").replace("-", "");
        return cleaned.startsWith("+84") ? "0" + cleaned.substring(3) : cleaned;
    }
    public static boolean validateFullName(String name) {
        return name.length() >= 2 && name.length() <= 50 && Pattern.compile("^[\\p{L}\\s'\\-]+$").matcher(name.trim()).matches();
    }
    public static String sanitizeInput(String input) {
        return input.trim().replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;").replace("/", "&#x2F;");
    }
    public static final class PasswordValidation {
        private final boolean valid; private final PasswordStrength strength;
        public PasswordValidation(boolean valid, PasswordStrength strength) { this.valid = valid; this.strength = strength; }
        public boolean isValid() { return valid; } public PasswordStrength getStrength() { return strength; }
    }
    private static boolean containsSpecial(String value) {
        for (int i = 0; i < value.length(); i++) if (SPECIAL.indexOf(value.charAt(i)) >= 0) return true;
        return false;
    }
}
