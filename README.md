# Repair Shop — compact syllabus version

591 Java lines across four files, including comments and blank lines.
Uses Java 17 and MySQL through JDBC. The only external library is the MySQL
JDBC driver in lib/. No Maven, H2, CSV library or testing framework is required.

## Files to explain

| File | Responsibility |
| --- | --- |
| Main.java | Menu, input validation, reports, import/export |
| Shop.java | Models, inheritance, custom exception and thread/queue |
| Database.java | JDBC connection, schema and database operations |
| Csv.java | CSV reading/writing with Java character streams |

## First-time database setup

Install JDK 17+ and MySQL Server 8+. Start MySQL, then create the database as an
administrator:

```sql
CREATE DATABASE IF NOT EXISTS repair_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
```

You can use your existing Workbench account. To create a dedicated account
instead, replace the example password and run:

```sql
CREATE USER IF NOT EXISTS 'repair_app'@'localhost' IDENTIFIED BY 'YOUR_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, REFERENCES ON repair_shop.* TO 'repair_app'@'localhost';
```

An existing account keeps its existing password. Existing repair_shop tables
and data from the previous version remain usable; the application does not
reset them. Tables must use InnoDB. The application creates missing tables.

In PowerShell, from this project directory:

```powershell
.\run-local.cmd
```

Enter your MySQL username (press Enter for root) and password when prompted.
The password is hidden while typing. If REPAIR_DB_PASSWORD is already configured,
the application uses the environment settings automatically.

The launcher compiles every time and starts the MySQL version. Its name is kept
for convenience; the former H2/--local mode has been removed. MySQL must run.
For a different server/database, set REPAIR_DB_URL to its JDBC URL, for example
jdbc:mysql://localhost:3306/repair_shop?connectTimeout=5000&socketTimeout=15000.

Equivalent manual commands:

```powershell
javac --release 17 -d out Main.java Shop.java Database.java Csv.java
java -cp "out;lib/*" Main
```

- javac compiles the four source files; --release 17 targets Java 17.
- -d out puts generated class files in out/.
- java runs the compiled Main class.
- -cp tells Java where classes and the MySQL driver are located.
- Windows separates classpath entries with a semicolon; Linux/macOS use a colon.

When sharing this project, include lib/mysql-connector-j-9.2.0.jar. The driver
is needed at runtime; compilation only needs the standard JDK JDBC interfaces.

## Features and behaviour

Add/list parts, create/view jobs, queue part usage, report low stock and usage,
import/export CSV, restock parts and delete unused parts. Used parts cannot be
deleted because their usage history references them through foreign keys.

Option 5 queues a request; option 11 shows success or failure. Other operations
wait for earlier requests, so reports include completed stock changes. Exit
also drains accepted requests. Stock deduction and usage recording share a
transaction; imports also roll back the whole batch on an error.

Exports replace the selected file. Enter a path whose parent folder exists.
CSV supports quoted commas, escaped double quotes and multiline text. Import
adds new part IDs only; duplicate IDs reject the entire import.

## Syllabus concepts actually demonstrated

- Classes, constructors, this/static, encapsulation and inheritance (Entity).
- Abstraction, interfaces and polymorphism (Entity, Comparable, Runnable, UsageTask).
- Strings, comparison, formatting, StringBuilder and Integer parsing.
- Checked/unchecked exceptions, try/catch/finally, multi-catch, throw/throws
  and a custom ShopException.
- ArrayList, LinkedList, HashSet, Map configuration and Queue traversal/use.
- Comparable, Comparator and Collections.sort.
- Thread/Runnable, start, join, interruption, synchronized, wait/notifyAll,
  a guarded producer-consumer queue and visibility through intrinsic locks.
- JDBC DriverManager/Connection, Statement/PreparedStatement, secure parameters,
  ResultSet iteration, object mapping, CRUD, transactions and resource cleanup.
- Lambdas, method references, streams, filter/sorted/skip and forEach.
- Generic collections and a bounded generic method (showParts).
- FileReader/FileWriter, character streams, CSV and collection import/export.

This uses a subset of the syllabus, not a forced demonstration of every listed
collection, stream operation or thread state. The line count is not achieved
by putting the whole program on a few dense lines.

## Manual checks for your demonstration

1. Add P1/Battery/5 and job J1/Phone/No power.
2. Queue two P1 for J1; wait (11). Remaining stock should be 3.
3. Request four more: failure, with stock still 3.
4. Request one for a missing job: failure; stock stays 3 (rollback).
5. Restock P1 by 2: stock becomes 5. A negative quantity is rejected.
6. Export inventory; check the CSV header and values. Import inventory.csv.
7. Import inventory.csv again: duplicate error; no partial additions.
8. Export usage: J1/P1 totals 2. Restart: records should still be present.
9. Delete unused P201: succeeds. Delete used P1: foreign-key rejection.
10. Type letters at a numeric prompt: error, then the menu continues.

The pre-simplification source, tests and documentation are preserved in
original-project-backup.zip. Old target/ and data/ files, if present locally,
are legacy build/demo artifacts and are not used by this version.

Validation: compiled for Java 17 and passed 27 checks, including CSV round trips,
input validation, queued requests, transactional rollback, concurrent stock
claims, persistence and CLI behaviour against an isolated MySQL 8 instance.
The temporary test server was shut down and its database removed.

