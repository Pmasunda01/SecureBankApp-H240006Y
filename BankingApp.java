import java.io.Console;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class BankingApp {
    private static DataStore ds;
    private static AuthService auth;

    public static void main(String[] args) {
        try {
            String baseDir = "data"; // will create data/ directory and files
            ds = new DataStore(baseDir);
            auth = new AuthService(ds);
            runConsole();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runConsole() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Secure Banking Application ===");
        while (true) {
            System.out.println("\n1) Login\n2) Register\n3) Exit");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            if (choice.equals("1")) {
                performLogin(scanner);
            } else if (choice.equals("2")) {
                performRegister(scanner);
            } else if (choice.equals("3")) {
                System.out.println("Goodbye.");
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }
        scanner.close();
    }

    private static void performRegister(Scanner scanner) throws Exception {
        System.out.println("--- Register ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        char[] password = readPassword("Password (min 6 chars): ");
        if (password == null) {
            System.out.println("No console available; reading password in plain (not recommended).");
            System.out.print("Password: ");
            password = scanner.nextLine().toCharArray();
        }
        boolean ok = auth.register(username, password);
        zeroOut(password);
        if (ok) System.out.println("Registration successful. Please login.");
        else System.out.println("Registration failed (username may exist or password too short).");
    }

    private static void performLogin(Scanner scanner) throws Exception {
        System.out.println("--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        char[] password = readPassword("Password: ");
        if (password == null) {
            System.out.print("Password: ");
            password = scanner.nextLine().toCharArray();
        }
        boolean ok = auth.login(username, password);
        zeroOut(password);
        if (ok) {
            System.out.println("Login successful.");
            userMenu(scanner, username);
        } else {
            System.out.println("Login failed. Check username/password.");
        }
    }

    private static void userMenu(Scanner scanner, String username) throws Exception {
        while (true) {
            System.out.println("\n--- Main Menu (" + username + ") ---");
            System.out.println("1) Create New Account");
            System.out.println("2) View Accounts");
            System.out.println("3) Deposit");
            System.out.println("4) Withdraw");
            System.out.println("5) Transaction History");
            System.out.println("6) Logout");
            System.out.print("Choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1": createAccount(scanner, username); break;
                case "2": viewAccounts(username); break;
                case "3": deposit(scanner, username); break;
                case "4": withdraw(scanner, username); break;
                case "5": txHistory(scanner, username); break;
                case "6": return;
                default: System.out.println("Invalid option."); break;
            }
        }
    }

    private static void createAccount(Scanner scanner, String username) throws Exception {
        String accountId = username + "-" + UUID.randomUUID().toString().substring(0,8);
        Account a = new Account(accountId, username, new BigDecimal("0.00"));
        ds.addAccount(a);
        System.out.println("Account created: " + accountId);
    }

    private static void viewAccounts(String username) {
        List<Account> list = ds.getAccountsForUser(username);
        if (list.isEmpty()) System.out.println("No accounts yet.");
        else {
            System.out.println("Your accounts:");
            for (Account a : list) {
                System.out.printf(" - %s : %s%n", a.getAccountId(), a.getBalance().toPlainString());
            }
        }
    }

    private static Account promptSelectAccount(Scanner scanner, String username) {
        List<Account> list = ds.getAccountsForUser(username);
        if (list.isEmpty()) {
            System.out.println("No accounts found. Create one first.");
            return null;
        }
        System.out.println("Select account by number:");
        for (int i = 0; i < list.size(); i++) {
            Account a = list.get(i);
            System.out.printf("%d) %s : %s%n", i+1, a.getAccountId(), a.getBalance().toPlainString());
        }
        System.out.print("Choice: ");
        String s = scanner.nextLine().trim();
        try {
            int idx = Integer.parseInt(s) - 1;
            if (idx < 0 || idx >= list.size()) { System.out.println("Invalid selection."); return null; }
            return list.get(idx);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid input.");
            return null;
        }
    }

    private static void deposit(Scanner scanner, String username) throws Exception {
        Account a = promptSelectAccount(scanner, username);
        if (a == null) return;
        System.out.print("Amount to deposit: ");
        String s = scanner.nextLine().trim();
        try {
            BigDecimal amt = new BigDecimal(s).setScale(2);
            if (amt.compareTo(BigDecimal.ZERO) <= 0) { System.out.println("Amount must be > 0."); return; }
            a.deposit(amt);
            ds.persistAccounts();
            String txId = "TX-" + UUID.randomUUID().toString().substring(0,8);
            TransactionRecord tr = TransactionRecord.create(txId, a.getAccountId(), "DEPOSIT", amt);
            ds.addTransaction(tr);
            System.out.println("Deposit complete. New balance: " + a.getBalance().toPlainString());
        } catch (NumberFormatException ex) {
            System.out.println("Invalid amount.");
        }
    }

    private static void withdraw(Scanner scanner, String username) throws Exception {
        Account a = promptSelectAccount(scanner, username);
        if (a == null) return;
        System.out.print("Amount to withdraw: ");
        String s = scanner.nextLine().trim();
        try {
            BigDecimal amt = new BigDecimal(s).setScale(2);
            if (amt.compareTo(BigDecimal.ZERO) <= 0) { System.out.println("Amount must be > 0."); return; }
            boolean ok = a.withdraw(amt);
            if (!ok) { System.out.println("Insufficient funds."); return; }
            ds.persistAccounts();
            String txId = "TX-" + UUID.randomUUID().toString().substring(0,8);
            TransactionRecord tr = TransactionRecord.create(txId, a.getAccountId(), "WITHDRAW", amt);
            ds.addTransaction(tr);
            System.out.println("Withdrawal complete. New balance: " + a.getBalance().toPlainString());
        } catch (NumberFormatException ex) {
            System.out.println("Invalid amount.");
        }
    }

    private static void txHistory(Scanner scanner, String username) {
        Account a = promptSelectAccount(scanner, username);
        if (a == null) return;
        List<TransactionRecord> list = ds.getTransactionsForAccount(a.getAccountId());
        if (list.isEmpty()) System.out.println("No transactions for this account.");
        else {
            System.out.println("Transactions for " + a.getAccountId() + ":");
            for (TransactionRecord t : list) System.out.println(" - " + t.toString());
        }
    }

    private static char[] readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            return console.readPassword(prompt);
        } else {
            return null;
        }
    }

    private static void zeroOut(char[] arr) {
        if (arr == null) return;
        for (int i = 0; i < arr.length; i++) arr[i] = '\0';
    }
}
