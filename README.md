# Repair Shop Job Card and Spare Parts Manager

Java 17 command-line application with JDBC/MySQL persistence, queued part usage,
and CSV import/export. Build with Maven and JDK 17 or newer.

## Topic and class guide

All paths below are relative to src/main/java.

| Topic / responsibility | Files |
| --- | --- |
| Startup and resource lifecycle | cli/Main.java |
| Menu, input and exception handling | cli/ConsoleMenu.java |
| OOP and immutable records | model/SparePart.java, model/RepairJob.java, model/PartUsage.java |
| Input validation | model/Validation.java |
| Business operations | service/RepairShopService.java |
| Threads, queue, futures and synchronization | service/PartRequestProcessor.java |
| Functional interface for worker callbacks | service/PartUsageHandler.java |
| Streams, lambdas and aggregation | service/ReportService.java |
| Comparator composition and sorting | service/ReportComparators.java |
| Generics and interfaces | repository/Repository.java |
| Connection abstraction and configuration | repository/ConnectionFactory.java, repository/DatabaseConfig.java |
| JDBC queries and collections | repository/JdbcPartRepository.java, repository/JdbcJobRepository.java |
| Stock row locking and transactional imports | repository/JdbcShopStore.java |
| Generic transactions, commit and rollback | repository/Transactions.java |
| Table initialization | repository/JdbcSchema.java |
| CSV parsing and formatting | csv/CsvFiles.java |
| File I/O and atomic replacement | fileio/AtomicFileWriter.java |

Each class owns a responsibility. Language concepts naturally work together:
streams use comparators, JDBC uses exceptions, and workers use interfaces.
Custom exceptions, TreeMap, PriorityQueue, custom Comparable and explicit
StringBuilder usage are not demonstrated by this project.

## Connect to MySQL on Windows

### Local sample database (no MySQL required)

From the project folder, run ` .\run-local.cmd ` in PowerShell. This starts the
application with an embedded H2 database stored at `data/repair-shop-local.mv.db`.
The launcher switches to the project directory so the database stays here even
when the launcher is called from elsewhere. No server or credentials are needed.
Use one running application at a time for this local database.

The equivalent command, from the project directory, is:

```powershell
java -jar target/repair-shop-2.0.0.jar --local
```

Run `mvn package` after Java source changes to rebuild the JAR. The launcher builds
automatically only when the JAR is missing. The sample database persists between
runs and is ignored by Git. It is an actual H2 database for practice, not a MySQL
server. The existing `data/repair-shop.dat` legacy file is unrelated.

The initial sample contains three parts (P101-P103), two jobs (J101-J102), and
one recorded use of P101 by J101. Its starting stock after that use is 9, 5, and 3.
Further actions in the menu change these values normally; launches do not reset them.

### MySQL server mode

The JDBC driver is included by Maven. MySQL Server runs separately. The application
creates its three tables, but the database and account must already exist.
It reads process environment variables, not .env files.

1. Check `Get-Service MySQL80` in PowerShell. If stopped, start it from Windows
   Services or use `Start-Service MySQL80` in an administrator terminal.
   The service name can differ on other installations.
2. Open your administrator connection in MySQL Workbench, or run:

   ```powershell
   & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -u root -p
   ```

3. For a new local setup, execute this SQL, replacing the example password first:

   ```sql
   CREATE DATABASE IF NOT EXISTS repair_shop
     CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
   CREATE USER IF NOT EXISTS 'repair_app'@'localhost'
     IDENTIFIED BY 'REPLACE_WITH_YOUR_OWN_PASSWORD';
   GRANT SELECT, INSERT, UPDATE, CREATE, REFERENCES
     ON repair_shop.* TO 'repair_app'@'localhost';
   ```

   If the account exists, CREATE USER IF NOT EXISTS does not change its password.
   Use its existing password or deliberately change it as administrator.

4. In the project folder, configure and run from the same PowerShell terminal:

   ```powershell
   $env:REPAIR_DB_URL = 'jdbc:mysql://localhost:3306/repair_shop?connectTimeout=5000&socketTimeout=15000'
   $env:REPAIR_DB_USER = 'repair_app'
   $credential = Get-Credential -UserName repair_app -Message 'Enter the MySQL application password'
   $env:REPAIR_DB_PASSWORD = $credential.GetNetworkCredential().Password
   mvn package
   java -jar target/repair-shop-2.0.0.jar
   ```

   Settings apply to this terminal and its child processes. For IDE launches,
   set these variables in the run configuration. Adjust the port if needed.
   Do not commit passwords.

DatabaseConfig opens JDBC connections. JdbcSchema initializes spare_parts,
repair_jobs and part_usage using src/main/resources/db/schema.sql. Recording
usage locks the part row, deducts stock and inserts usage in one transaction.
A failure rolls back both changes. Option 11 waits for queued usage to finish
before you inspect reports.

Troubleshooting:

- Missing REPAIR_DB_PASSWORD: configure it in the process launching Java.
- Connection refused / SQL state starting with 08: check server, host and port.
- Access denied / SQL state 28000: check account, password and permitted host.
- Unknown database: create the database named in the URL.
- CREATE denied: the application needs table-creation privileges at startup.

## Tests

`mvn test` runs the main suite on isolated H2 in-memory databases in MySQL
compatibility mode. This does not verify the running MySQL server.

MySqlIntegrationTest is skipped unless REPAIR_TEST_MYSQL_URL is configured.
A skipped test means no connection was attempted, not that JDBC failed.

To enable it, create a separate repair_shop_test database and repair_test account
using the setup SQL above with those names. Grant the test account SELECT,
INSERT, UPDATE, DELETE, CREATE and REFERENCES on repair_shop_test.*. DELETE is
needed to clean up test rows. Never point this test at production.

```powershell
$env:REPAIR_TEST_MYSQL_URL = 'jdbc:mysql://localhost:3306/repair_shop_test?connectTimeout=5000&socketTimeout=15000'
$env:REPAIR_TEST_MYSQL_USER = 'repair_test'
$testCredential = Get-Credential -UserName repair_test -Message 'Enter the MySQL test account password'
$env:REPAIR_TEST_MYSQL_PASSWORD = $testCredential.GetNetworkCredential().Password
mvn test
```

Historical MySQL demo logs live in docs/evidence/jdbc. They describe an earlier
run, not the current server or credentials.
