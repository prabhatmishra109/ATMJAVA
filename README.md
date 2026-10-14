# Advanced ATM / Bank Account Simulator (Core Java)

This is a complete, production-grade Core Java console application simulating an ATM system.

## Features
- **Object-Oriented Inheritance & Polymorphism:** Base `Account` class extended by `SavingsAccount` and `CurrentAccount`.
- **Security PIN Lockout:** Automatically locks accounts after 3 consecutive failed PIN attempts.
- **File Persistence:** Automatically saves and loads balance updates and account records using `atm_database.txt`.
- **Transactions & Statements:** Records deposits, withdrawals, and interest additions with timestamps[cite: 2].

## How to Compile and Run

1. Open your terminal or command prompt in the folder containing `ATMSimulator.java`[cite: 2].
2. Compile the Java file[cite: 2]:
   ```bash
   javac ATMSimulator.java
