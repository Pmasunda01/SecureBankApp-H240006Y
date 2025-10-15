import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class Securityutil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits

    public static String generateSaltBase64() {
        byte[] s = new byte[SALT_BYTES];
        RANDOM.nextBytes(s);
        return Base64.getEncoder().encodeToString(s);
    }

    public static String hashPasswordBase64(char[] password, String saltBase64) throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(key);
    }

    // constant-time comparison
    public static boolean isExpectedPassword(String hashBase64, char[] password, String saltBase64) throws Exception {
        String computed = hashPasswordBase64(password, saltBase64);
        return slowEquals(hashBase64.getBytes("UTF-8"), computed.getBytes("UTF-8"));
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.min(a.length, b.length); i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

    public static String sanitizeUsername(String username) {
        if (username == null) return "";
        return username.trim();
    }
}
