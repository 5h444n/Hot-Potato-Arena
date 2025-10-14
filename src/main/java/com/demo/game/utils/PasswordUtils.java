package com.demo.game.utils;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PasswordUtils {
    // Password complexity patterns
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    // BCrypt work factor (higher = more secure but slower)
    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hashes a password using BCrypt
     * @param plainPassword The plain text password
     * @return The hashed password
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifies a password against a hash
     * @param plainPassword The plain text password
     * @param hashedPassword The hashed password
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    /**
     * Generates a random secure password
     * @param length The desired password length
     * @return A randomly generated password
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    /**
     * Calculates password strength score (0-100)
     * @param password The password to evaluate
     * @return Strength score from 0 to 100
     */
    public static int calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length scoring
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 20;
        if (password.length() >= 16) score += 10;

        // Complexity scoring
        if (UPPERCASE.matcher(password).find()) score += 15;
        if (LOWERCASE.matcher(password).find()) score += 15;
        if (DIGIT.matcher(password).find()) score += 15;
        if (SPECIAL.matcher(password).find()) score += 15;

        // Bonus for mixing character types
        int typeCount = 0;
        if (UPPERCASE.matcher(password).find()) typeCount++;
        if (LOWERCASE.matcher(password).find()) typeCount++;
        if (DIGIT.matcher(password).find()) typeCount++;
        if (SPECIAL.matcher(password).find()) typeCount++;

        if (typeCount >= 3) score += 10;
        if (typeCount == 4) score += 10;

        return Math.min(score, 100);
    }

    /**
     * Validates password against security requirements
     * @param password The password to validate
     * @return ValidationResult containing success status and message
     */
    public static ValidationResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password cannot be empty");
        }

        if (password.length() < 6) {
            return new ValidationResult(false, "Password must be at least 6 characters long");
        }

        if (password.length() > 128) {
            return new ValidationResult(false, "Password cannot exceed 128 characters");
        }

        // Check for common weak passwords
        if (isCommonPassword(password)) {
            return new ValidationResult(false, "This password is too common. Please choose a stronger password");
        }

        return new ValidationResult(true, "Password is valid");
    }

    /**
     * Checks if password is in the common passwords list
     * @param password The password to check
     * @return true if password is common, false otherwise
     */
    private static boolean isCommonPassword(String password) {
        String[] commonPasswords = {
                "password", "123456", "password123", "admin", "letmein",
                "qwerty", "abc123", "111111", "123123", "welcome"
        };

        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Result class for password validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    /**
     * Generates a password reset token
     * @return A secure random token
     */
    public static String generateResetToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        StringBuilder token = new StringBuilder();
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }

        return token.toString();
    }
}
