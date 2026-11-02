import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PasswordUtil
 * ------------
 * Passwords are NEVER stored as plain text. We store a salted hash:
 *
 *      PBKDF2$65536$<random salt>$<hash>
 *
 * - salt : random bytes, so two admins with the same password get different hashes
 * - PBKDF2 with 65,536 rounds : deliberately slow, so guessing passwords is expensive
 * Uses only classes built into Java (no external library).
 */
public final class PasswordUtil {

    private static final String PREFIX = "PBKDF2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private PasswordUtil() {
    }

    /** Turns a plain password into the string stored in the database. */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** True if the stored value is already a hash (false for old plain-text passwords). */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX + "$");
    }

    /** Checks a typed password against a stored hash. */
    public static boolean verify(String password, String stored) {
        if (!isHashed(stored) || password == null) {
            return false;
        }
        try {
            String[] parts = stored.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);   // constant-time comparison
        } catch (RuntimeException e) {
            return false;   // malformed stored value
        }
    }

    /** Returns null if the password is acceptable, otherwise a message explaining why not. */
    public static String checkStrength(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (password.length() > 50) {
            return "Password must be at most 50 characters long.";
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                letter = true;
            }
            if (Character.isDigit(c)) {
                digit = true;
            }
        }
        if (!letter || !digit) {
            return "Password must contain at least one letter and one digit.";
        }
        return null;
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available on this Java version.", e);
        }
    }
}
