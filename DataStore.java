import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private final Path usersFile;
    private final Path accountsFile;
    private final Path transactionsFile;

    // In-memory caches
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final List<TransactionRecord> transactions = Collections.synchronizedList(new ArrayList<>());

    public DataStore(String baseDir) throws IOException {
        Files.createDirectories(Paths.get(baseDir));
        usersFile = Paths.get(baseDir, "users.txt");
        accountsFile = Paths.get(baseDir, "accounts.txt");
        transactionsFile = Paths.get(baseDir, "transactions.txt");
        // ensure files exist
        if (!Files.exists(usersFile)) Files.createFile(usersFile);
        if (!Files.exists(accountsFile)) Files.createFile(accountsFile);
        if (!Files.exists(transactionsFile)) Files.createFile(transactionsFile);
        loadAll();
    }

    private void loadAll() throws IOException {
        loadUsers();
        loadAccounts();
        loadTransactions();
    }

    private void loadUsers() throws IOException {
        try (BufferedReader r = Files.newBufferedReader(usersFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                User u = User.fromStorageString(line);
                if (u != null) users.put(u.getUsername(), u);
            }
        }
    }

    private void loadAccounts() throws IOException {
        try (BufferedReader r = Files.newBufferedReader(accountsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Account a = Account.fromStorageString(line);
                if (a != null) accounts.put(a.getAccountId(), a);
            }
        }
    }

    private void loadTransactions() throws IOException {
        try (BufferedReader r = Files.newBufferedReader(transactionsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                TransactionRecord t = TransactionRecord.fromStorageString(line);
                if (t != null) transactions.add(t);
            }
        }
    }

    // Users
    public synchronized boolean usernameExists(String username) {
        return users.containsKey(username);
    }
    public synchronized void addUser(User user) throws IOException {
        users.put(user.getUsername(), user);
        appendLine(usersFile, user.toStorageString());
    }
    public synchronized User getUser(String username) { return users.get(username); }

    // Accounts
    public synchronized void addAccount(Account account) throws IOException {
        accounts.put(account.getAccountId(), account);
        appendLine(accountsFile, account.toStorageString());
    }
    public synchronized Account getAccount(String accountId) { return accounts.get(accountId); }
    public synchronized List<Account> getAccountsForUser(String username) {
        List<Account> list = new ArrayList<>();
        for (Account a : accounts.values()) if (a.getOwnerUsername().equals(username)) list.add(a);
        return list;
    }
    public synchronized void persistAccounts() throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(accountsFile, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Account a : accounts.values()) w.write(a.toStorageString() + System.lineSeparator());
        }
    }

    // Transactions
    public synchronized void addTransaction(TransactionRecord tr) throws IOException {
        transactions.add(tr);
        appendLine(transactionsFile, tr.toStorageString());
    }
    public synchronized List<TransactionRecord> getTransactionsForAccount(String accountId) {
        List<TransactionRecord> out = new ArrayList<>();
        for (TransactionRecord t : transactions) if (t != null && t.toStorageString().contains("|" + accountId + "|")) out.add(t);
        return out;
    }

    private void appendLine(Path file, String line) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
            w.flush();
        }
    }
}
