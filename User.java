import java.util.Objects;

public class User {
    private final String username;
    private final String passwordHash; // base64
    private final String salt; // base64

    public User(String username, String passwordHash, String salt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }

    // CSV: username|passwordHash|salt
    public String toStorageString() {
        return String.join("|", escape(username), passwordHash, salt);
    }

    public static User fromStorageString(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 3) return null;
        return new User(unescape(parts[0]), parts[1], parts[2]);
    }

    private static String escape(String s) {
        return s.replace("|", "%7C");
    }
    private static String unescape(String s) {
        return s.replace("%7C", "|");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User u = (User) o;
        return username.equals(u.username);
    }
    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
