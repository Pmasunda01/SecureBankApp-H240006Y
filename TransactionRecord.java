import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class TransactionRecord {
    private final String txId; // simple unique id
    private final String accountId;
    private final String type; // DEPOSIT or WITHDRAW
    private final BigDecimal amount;
    private final String timestamp; // ISO-8601

    public TransactionRecord(String txId, String accountId, String type, BigDecimal amount, String timestamp) {
        this.txId = txId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public static TransactionRecord create(String txId, String accountId, String type, BigDecimal amount) {
        return new TransactionRecord(txId, accountId, type, amount, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
    }

    public String toStorageString() {
        return String.join("|", txId, accountId, type, amount.toPlainString(), timestamp);
    }

    public static TransactionRecord fromStorageString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length != 5) return null;
        return new TransactionRecord(p[0], p[1], p[2], new BigDecimal(p[3]), p[4]);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s", timestamp, type, amount.toPlainString(), accountId);
    }
}
