import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// --- 1. Transaction Class ---
class Transaction implements Serializable {
    private String type;
    private double amount;
    private LocalDateTime timestamp;

    public Transaction(String type, double amount) {
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "[" + timestamp.format(formatter) + "] " + type + ": $" + String.format("%.2f", amount);
    }
}

// --- 2. Abstract Base Account Class (Encapsulation & Inheritance) ---
abstract class Account {
    protected String accountNumber;
    protected int pin;
    protected double balance;
    protected List<Transaction> transactions;
    protected int failedAttempts = 0;
    protected boolean isLocked = false;

    public Account(String accountNumber, int pin, double initialBalance) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.balance = initialBalance;
        this.transactions = new ArrayList<>();
        if (initialBalance > 0) {
            transactions.add(new Transaction("INITIAL DEPOSIT", initialBalance));
        }
    }

    public boolean validatePin(int enteredPin) {
        if (isLocked) {
            return false;
        }
        if (this.pin == enteredPin) {
            failedAttempts = 0; // Reset on success
            return true;
        } else {
            failedAttempts++;
            if (failedAttempts >= 3) {
                isLocked = true;
            }
            return false;
        }
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            transactions.add(new Transaction("DEPOSIT", amount));
            System.out.println("Successfully deposited $" + String.format("%.2f", amount));
        } else {
            System.out.println("Invalid deposit amount.");
        }
    }

    // Abstract method to be overridden by subclasses (Polymorphism)
    public abstract boolean withdraw(double amount);

    public void printMiniStatement() {
        System.out.println("\n--- Mini Statement (" + getAccountType() + ") ---");
        if (transactions.isEmpty()) {
            System.out.println("No recent transactions.");
        } else {
            int start = Math.max(0, transactions.size() - 5);
            for (int i = start; i < transactions.size(); i++) {
                System.out.println(transactions.get(i));
            }
        }
        System.out.println("Current Balance: $" + String.format("%.2f", balance));
        System.out.println("----------------------------------------");
    }

    public String getAccountNumber() { return accountNumber; }
    public boolean isLocked() { return isLocked; }
    public int getFailedAttempts() { return failedAttempts; }
    public abstract String getAccountType();
}

// --- 3. Savings Account (Inheritance & Interest Feature) ---
class SavingsAccount extends Account {
    private double interestRate = 0.03; // 3% interest

    public SavingsAccount(String accountNumber, int pin, double initialBalance) {
        super(accountNumber, pin, initialBalance);
    }

    @Override
    public boolean withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance -= amount;
            transactions.add(new Transaction("WITHDRAWAL", amount));
            System.out.println("Successfully withdrew $" + String.format("%.2f", amount));
            return true;
        } else {
            System.out.println("Error: Insufficient funds or invalid amount!");
            return false;
        }
    }

    public void applyInterest() {
        double interest = balance * interestRate;
        balance += interest;
        transactions.add(new Transaction("INTEREST ADDED", interest));
        System.out.println("Interest of $" + String.format("%.2f", interest) + " applied to account.");
    }

    @Override
    public String getAccountType() { return "Savings Account"; }
}

// --- 4. Current Account (Inheritance & Overdraft Feature) ---
class CurrentAccount extends Account {
    private double overdraftLimit = 500.00; // Allows up to $500 in negative balance

    public CurrentAccount(String accountNumber, int pin, double initialBalance) {
        super(accountNumber, pin, initialBalance);
    }

    @Override
    public boolean withdraw(double amount) {
        if (amount > 0 && (balance - amount) >= -overdraftLimit) {
            balance -= amount;
            transactions.add(new Transaction("WITHDRAWAL (Overdraft)", amount));
            System.out.println("Successfully withdrew $" + String.format("%.2f", amount) + " (Overdraft active if negative)");
            return true;
        } else {
            System.out.println("Error: Exceeds overdraft limit ($500.00)!");
            return false;
        }
    }

    @Override
    public String getAccountType() { return "Current Account"; }
}

// --- 5. Bank Manager & File Persistence ---
class Bank {
    private List<Account> accounts;
    private final String FILE_NAME = "atm_database.txt";

    public Bank() {
        accounts = new ArrayList<>();
        loadDataFromFile();
        if (accounts.isEmpty()) {
            accounts.add(new SavingsAccount("1001", 1234, 1000.00));
            accounts.add(new CurrentAccount("1002", 4321, 2500.00));
        }
    }

    public Account login(String accNum, int pin, Scanner scanner) {
        for (Account acc : accounts) {
            if (acc.getAccountNumber().equals(accNum)) {
                if (acc.isLocked()) {
                    System.out.println("ALERT: This account has been locked due to 3 consecutive failed PIN attempts.");
                    return null;
                }

                if (acc.validatePin(pin)) {
                    return acc;
                } else {
                    int attemptsLeft = 3 - acc.getFailedAttempts();
                    if (acc.isLocked()) {
                        System.out.println("Incorrect PIN. Account is now LOCKED due to 3 failed attempts.");
                    } else {
                        System.out.println("Incorrect PIN. Attempts remaining: " + attemptsLeft);
                    }
                    return null;
                }
            }
        }
        System.out.println("Account Number not found.");
        return null;
    }

    public void saveDataToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Account acc : accounts) {
                writer.println(acc.getAccountNumber() + "," + acc.pin + "," + acc.getBalance() + "," + acc.getAccountType());
            }
            System.out.println("[System: Account data successfully saved to file.]");
        } catch (IOException e) {
            System.out.println("Error saving data to file: " + e.getMessage());
        }
    }

    private void loadDataFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String accNum = parts[0];
                    int pin = Integer.parseInt(parts[1]);
                    double balance = Double.parseDouble(parts[2]);
                    String type = parts[3];

                    if (type.contains("Savings")) {
                        accounts.add(new SavingsAccount(accNum, pin, balance));
                    } else {
                        accounts.add(new CurrentAccount(accNum, pin, balance));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading data: " + e.getMessage());
        }
    }
}

// --- 6. Main Controller Interface ---
public class ATMSimulator {
    public static void main(String[] args) {
        Bank bank = new Bank();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================");
        System.out.println("    ADVANCED JAVA ATM SIMULATOR SYSTEM   ");
        System.out.println("=========================================");
        System.out.println("Test Accounts Available:");
        System.out.println(" - Savings Account: Number '1001', PIN '1234'");
        System.out.println(" - Current Account: Number '1002', PIN '4321'");
        System.out.println("-----------------------------------------");

        System.out.print("Enter Account Number: ");
        String accNum = scanner.nextLine();

        System.out.print("Enter 4-digit PIN: ");
        int pin = scanner.nextInt();

        Account currentAccount = bank.login(accNum, pin, scanner);

        if (currentAccount == null) {
            scanner.close();
            return;
        }

        System.out.println("\nLogin Successful! Welcome to your " + currentAccount.getAccountType() + ".");

        boolean exit = false;
        while (!exit) {
            System.out.println("\n--- ATM Menu ---");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Mini Statement (Transactions)");
            
            if (currentAccount instanceof SavingsAccount) {
                System.out.println("5. Apply Annual Interest (3%)");
                System.out.println("6. Exit");
            } else {
                System.out.println("5. Exit");
            }
            
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();

            if (currentAccount instanceof SavingsAccount) {
                SavingsAccount savings = (SavingsAccount) currentAccount;
                switch (choice) {
                    case 1:
                        System.out.println("Current Balance: $" + String.format("%.2f", savings.getBalance()));
                        break;
                    case 2:
                        System.out.print("Enter deposit amount: $");
                        savings.deposit(scanner.nextDouble());
                        break;
                    case 3:
                        System.out.print("Enter withdrawal amount: $");
                        savings.withdraw(scanner.nextDouble());
                        break;
                    case 4:
                        savings.printMiniStatement();
                        break;
                    case 5:
                        savings.applyInterest();
                        break;
                    case 6:
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            } else {
                CurrentAccount current = (CurrentAccount) currentAccount;
                switch (choice) {
                    case 1:
                        System.out.println("Current Balance: $" + String.format("%.2f", current.getBalance()) + " (Overdraft Limit: $500.00)");
                        break;
                    case 2:
                        System.out.print("Enter deposit amount: $");
                        current.deposit(scanner.nextDouble());
                        break;
                    case 3:
                        System.out.print("Enter withdrawal amount: $");
                        current.withdraw(scanner.nextDouble());
                        break;
                    case 4:
                        current.printMiniStatement();
                        break;
                    case 5:
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        }

        bank.saveDataToFile();
        System.out.println("Thank you for banking with us. Goodbye!");
        scanner.close();
    }
}
