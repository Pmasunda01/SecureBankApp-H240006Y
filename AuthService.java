import java.io.IOException;

public class AuthService {
    private final DataStore ds;

    public AuthService(DataStore ds) {
        this.ds = ds;
    }

    public boolean register(String username, char[] password) throws Exception {
        username = Securityutil.sanitizeUsername(username);
        if (username.isEmpty() || password == null || password.length < 6) {
            return false;
        }
        if (ds.usernameExists(username)) return false;
        String salt = Securityutil.generateSaltBase64();
        String hash = Securityutil.hashPasswordBase64(password, salt);
        User u = new User(username, hash, salt);
        ds.addUser(u);
        return true;
    }

    public boolean login(String username, char[] password) throws Exception {
        username = Securityutil.sanitizeUsername(username);
        User u = ds.getUser(username);
        if (u == null) return false;
        return Securityutil.isExpectedPassword(u.getPasswordHash(), password, u.getSalt());
    }
}
