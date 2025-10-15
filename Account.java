import java.math.BigDecimal;
import java.math.RoundingMode;

public class Account {
    private final String accountId; // unique id, e.g. username-1 or UUID optional
    private final String ownerUsername;
    private BigDecimal balance;

    public Account(String accountId, String ownerUsername, BigDecimal balance) {
        this.accountId = accountId;
        this.ownerUsername = ownerUsername;
        this.balance = balance.setScale(2, RoundingMode.HALF_UP);
    }

    public String getAccountId() { return accountId; }
    public String getOwnerUsername() { return ownerUsername; }
    public BigDecimal getBalance() { return balance; }

    public synchronized void deposit(BigDecimal amount) {
        balance = balance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public synchronized boolean withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) return false;
        balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        return true;
    }

    // storage: accountId|ownerUsername|balance
    public String toStorageString() {
        return String.join("|", accountId, ownerUsername, balance.toPlainString());
    }

    public static Account fromStorageString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length != 3) return null;
        return new Account(p[0], p[1], new BigDecimal(p[2]));
    }
}
