package com.passgen;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Core password generation engine using SecureRandom for cryptographic strength.
 * <p>
 * Supports configurable character sets, ambiguous character exclusion,
 * delimiter-based chunking, batch generation, and entropy estimation.
 */
public class PasswordEngine {

    // ── Character pools ──────────────────────────────────────────────
    private static final String LOWERCASE   = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS      = "0123456789";
    private static final String SYMBOLS     = "!@#$%^&*()-_=+[]{}|;:',.<>?/`~";

    /** Characters considered ambiguous / easily confused */
    private static final String AMBIGUOUS   = "O0l1I";

    private final SecureRandom secureRandom;

    public PasswordEngine() {
        this.secureRandom = new SecureRandom();
    }

    // ── Entropy seeding ──────────────────────────────────────────────

    /**
     * Feeds additional entropy bytes into the SecureRandom instance.
     * Called by the mouse-motion entropy harvester to strengthen the PRNG seed.
     *
     * @param seed arbitrary bytes (e.g. from mouse coordinates + timestamps)
     */
    public void addEntropySeed(byte[] seed) {
        secureRandom.setSeed(seed);
    }

    // ── Password generation ──────────────────────────────────────────

    /**
     * Generates a single password with the given configuration.
     *
     * @param length           desired length (before delimiter formatting)
     * @param useLowercase     include a-z
     * @param useUppercase     include A-Z
     * @param useDigits        include 0-9
     * @param useSymbols       include special characters
     * @param excludeAmbiguous remove O, 0, l, 1, I from the pool
     * @param useDelimiter     insert delimiters between blocks
     * @param blockSize        number of characters per block (ignored if useDelimiter is false)
     * @param delimiter        delimiter string (e.g. "-")
     * @return generated password string
     */
    public String generate(int length,
                           boolean useLowercase,
                           boolean useUppercase,
                           boolean useDigits,
                           boolean useSymbols,
                           boolean excludeAmbiguous,
                           boolean useDelimiter,
                           int blockSize,
                           String delimiter) {

        String pool = buildPool(useLowercase, useUppercase, useDigits, useSymbols, excludeAmbiguous);
        if (pool.isEmpty() || length <= 0) {
            return "";
        }

        // Build the raw password
        StringBuilder raw = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            raw.append(pool.charAt(secureRandom.nextInt(pool.length())));
        }

        // Optionally insert delimiters
        if (useDelimiter && blockSize > 0 && delimiter != null && !delimiter.isEmpty()) {
            return insertDelimiters(raw.toString(), blockSize, delimiter);
        }
        return raw.toString();
    }

    /**
     * Generates a batch of passwords with identical settings.
     *
     * @param count number of passwords to generate
     * @return list of generated password strings
     */
    public List<String> generateBatch(int count,
                                      int length,
                                      boolean useLowercase,
                                      boolean useUppercase,
                                      boolean useDigits,
                                      boolean useSymbols,
                                      boolean excludeAmbiguous,
                                      boolean useDelimiter,
                                      int blockSize,
                                      String delimiter) {
        List<String> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            batch.add(generate(length, useLowercase, useUppercase, useDigits,
                    useSymbols, excludeAmbiguous, useDelimiter, blockSize, delimiter));
        }
        return batch;
    }

    // ── Entropy & strength estimation ────────────────────────────────

    /**
     * Strength tier labels for UI display.
     */
    public enum StrengthTier {
        NONE("None"),
        WEAK("Weak"),
        FAIR("Fair"),
        STRONG("Strong"),
        VERY_STRONG("Very Strong");

        private final String label;
        StrengthTier(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /**
     * Holds the result of an entropy calculation.
     */
    public static class EntropyResult {
        public final double bits;
        public final StrengthTier tier;
        public final String timeToCrack;

        public EntropyResult(double bits, StrengthTier tier, String timeToCrack) {
            this.bits = bits;
            this.tier = tier;
            this.timeToCrack = timeToCrack;
        }
    }

    /**
     * Calculates entropy in bits, strength tier, and estimated crack time.
     * <p>
     * Assumes an offline attack rate of 10 billion guesses per second
     * (modern GPU cluster).
     *
     * @return an EntropyResult with bits, tier, and human-readable crack time
     */
    public EntropyResult estimateEntropy(int length,
                                         boolean useLowercase,
                                         boolean useUppercase,
                                         boolean useDigits,
                                         boolean useSymbols,
                                         boolean excludeAmbiguous) {

        String pool = buildPool(useLowercase, useUppercase, useDigits, useSymbols, excludeAmbiguous);
        int poolSize = pool.length();

        if (poolSize == 0 || length <= 0) {
            return new EntropyResult(0, StrengthTier.NONE, "Instant");
        }

        // Entropy = length × log2(poolSize)
        double bits = length * (Math.log(poolSize) / Math.log(2));

        // Determine strength tier based on entropy bits
        StrengthTier tier;
        if (bits < 36) {
            tier = StrengthTier.WEAK;
        } else if (bits < 60) {
            tier = StrengthTier.FAIR;
        } else if (bits < 100) {
            tier = StrengthTier.STRONG;
        } else {
            tier = StrengthTier.VERY_STRONG;
        }

        // Estimated time to crack at 10 billion guesses/second
        String timeToCrack = estimateCrackTime(bits);

        return new EntropyResult(bits, tier, timeToCrack);
    }

    // ── Private helpers ──────────────────────────────────────────────

    /**
     * Builds the character pool based on toggle selections.
     */
    private String buildPool(boolean lower, boolean upper, boolean digits,
                             boolean symbols, boolean excludeAmbiguous) {
        StringBuilder pool = new StringBuilder(100);
        if (lower)   pool.append(LOWERCASE);
        if (upper)   pool.append(UPPERCASE);
        if (digits)  pool.append(DIGITS);
        if (symbols) pool.append(SYMBOLS);

        if (excludeAmbiguous) {
            // Remove each ambiguous character from the pool
            String result = pool.toString();
            for (char c : AMBIGUOUS.toCharArray()) {
                result = result.replace(String.valueOf(c), "");
            }
            return result;
        }
        return pool.toString();
    }

    /**
     * Inserts a delimiter every {@code blockSize} characters.
     * Example: "abcdefghij" with blockSize=4 and delimiter="-" → "abcd-efgh-ij"
     */
    private String insertDelimiters(String raw, int blockSize, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            if (i > 0 && i % blockSize == 0) {
                sb.append(delimiter);
            }
            sb.append(raw.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Converts entropy bits into a human-readable estimated crack time.
     * Assumes 10^10 guesses/second (offline GPU attack).
     */
    private String estimateCrackTime(double bits) {
        // Total combinations = 2^bits
        // Average guesses    = 2^bits / 2   (brute force on average)
        // seconds            = averageGuesses / guessesPerSecond
        double guessesPerSecond = 1e10; // 10 billion/s
        double logSeconds = (bits - 1) * Math.log(2) - Math.log(guessesPerSecond);
        // logSeconds = ln(seconds)

        if (logSeconds < 0) {
            return "Instant";
        }

        double seconds = Math.exp(logSeconds);

        if (seconds < 60)                    return String.format("%.0f seconds", seconds);
        if (seconds < 3_600)                 return String.format("%.1f minutes", seconds / 60);
        if (seconds < 86_400)                return String.format("%.1f hours",   seconds / 3_600);
        if (seconds < 86_400 * 365.25)       return String.format("%.1f days",    seconds / 86_400);
        if (seconds < 86_400 * 365.25 * 1e3) return String.format("%.1f years",   seconds / (86_400 * 365.25));
        if (seconds < 86_400 * 365.25 * 1e6) return String.format("%.2e years",   seconds / (86_400 * 365.25));
        if (seconds < 86_400 * 365.25 * 1e9) return String.format("%.2e years",   seconds / (86_400 * 365.25));

        // For astronomically large values, work in log-space to avoid overflow
        double logYears = logSeconds - Math.log(86_400 * 365.25);
        if (Double.isInfinite(seconds) || seconds >= 86_400 * 365.25 * 1e9) {
            double exponent = logYears / Math.log(10);
            return String.format("~10^%.0f years", exponent);
        }

        return String.format("%.2e years", seconds / (86_400 * 365.25));
    }
}
