# Learn the Repair Shop Project from Zero

A beginner's teaching guide to Java, backend logic, JDBC, databases, collections,
threads, reports, and files.

## 1. How to use this guide

- Read sections 2-7 first to understand the project before studying its code.
- Then study one concept at a time: meaning, example, project connection, and explanation.
- Lines marked **Explain it simply** are sentences you can use in a presentation.
- Small code snippets explain ideas; they are not always complete runnable programs.
- **Used here** means the concept appears in the current application.
- **Extra learning** means the concept was mentioned in our topic discussion but is not implemented here. Do not claim it as a project feature.
- Source links are relative to this document and open the relevant Java files.

## 2. What is a backend?

- Think of a repair shop with a reception desk and a stockroom.
- The receptionist takes requests: "Repair this phone" or "Use one charging port."
- Behind the desk, someone checks the stock, applies rules, and updates records.
- That behind-the-scenes work is similar to a software **backend**.
- A backend usually handles rules, calculations, data access, and storage.
- A user interface is the part a person interacts with. Here, it is a text menu in a terminal.
- This project is a standalone command-line application. It does not provide a website or an HTTP/REST API.
- You can still study backend concepts in it because it separates business rules from storage and user input.

**Explain it simply:** "The menu collects a request. The backend checks whether it is allowed and updates the stored information."

## 3. What problem does this project solve?

- A repair shop needs to know what spare parts it owns.
- It needs to record what is wrong with each device.
- When a technician uses a part, stock should decrease.
- The repair job should also record that the part was used.
- The system must reject a request if there is not enough stock.
- The shop can import stock from a CSV file and export useful reports.

Our running example:

| Item | Example |
| --- | --- |
| Part ID | P101 |
| Part name | USB-C Port |
| Starting stock | 2 |
| Repair job ID | J101 |
| Device | Phone |
| Issue | Not charging |
| Requested usage | 1 port |
| Expected remaining stock | 1 |

- An **ID** is a short identifier used to distinguish one record from another.
- Two parts can have similar names, so the application uses IDs for lookups.
- This project currently does not include billing, customer accounts, restocking existing parts, or job-completion status.

## 4. How the pieces fit together

```text
Person types into the console
          |
          v
ConsoleMenu: reads input and displays results
          |
          v
Service: performs a business operation
          |
          v
Repository / store: executes SQL through JDBC
          |
          v
MySQL: stores the records
```

- A **layer** groups code with a related responsibility.
- The console should not contain every SQL statement.
- Database code should not ask the user to type a menu choice.
- Separating these responsibilities makes the project easier to read, test, and change.
- Concepts still work together: a JDBC class can use collections and exceptions without being badly organized.

**Explain it simply:** "Each layer has a job, just like reception, the technician, and the stockkeeper have different jobs."

## 5. Java building blocks: class, object, field, method, constructor

- A **class** defines what a kind of object contains and can do.
- An **object** is a particular instance created from that definition.
- A **field** stores information inside an object.
- A **method** is an operation that an object or class can perform.
- A **constructor** runs when an object is created and establishes its initial state.

```java
var part = new SparePart("P101", "USB-C Port", 2);
System.out.println(part.quantity());
```

- `SparePart` is the type being created.
- `new` creates an instance.
- The constructor receives the ID, name, and quantity.
- `part` is a variable referring to that instance.
- `quantity()` reads its quantity.
- `var` asks Java to infer the variable's type from the right-hand side. Java still checks the type at compile time.

**Explain it simply:** "A class describes a kind of thing; an object represents one particular thing."

## 6. OOP, encapsulation, records, and immutability

**Used here:** [SparePart](../src/main/java/model/SparePart.java), [RepairJob](../src/main/java/model/RepairJob.java), and [PartUsage](../src/main/java/model/PartUsage.java).

- **Object-oriented programming**, or OOP, organizes a program using objects with data and behavior.
- **Encapsulation** controls how an object's internal data can be accessed or changed.
- This project uses Java **records** for small data models.
- A record automatically supplies a constructor, accessors, `equals`, `hashCode`, and `toString` based on its components.
- Its component fields are private and final. There are no generated setter methods.
- `SparePart` checks its inputs when constructed, so negative starting stock is rejected.
- **Immutable** means an object cannot be changed after construction through its normal API.
- Records are only shallowly immutable: a record could still refer to a mutable list.
- `RepairJob` uses `List.copyOf(usages)` to avoid retaining a list that another caller can edit.
- The elements are also immutable `PartUsage` records, which makes the usage data safe to share.
- An immutable Java object does not make a database row permanent. The application can update the database and load a new object with the new value.

**Explain it simply:** "The model validates its data when created and does not let callers casually change its fields afterward."

## 7. Packages and access modifiers

- A **package** groups related Java types, similar to a labeled folder.
- `model` contains data definitions; `service` contains operations; `repository` contains storage access.
- An `import` lets a file refer to a type from another package by its short name.
- `public`: other code can access the type or member where Java's access rules allow.
- `private`: only the enclosing class can directly access that member.
- No access keyword: package-private, accessible inside the same package. `Transactions` uses this.
- `final` on a field means its reference/value cannot be reassigned after initialization; it does not automatically make the referenced object immutable.
- `final` on a class prevents subclassing.
- `static` means a member belongs to the class itself rather than a particular instance.
- For example, `Validation.id(...)` is a static utility method.

## 8. Interfaces, abstraction, and polymorphism

**Used here:** [Repository](../src/main/java/repository/Repository.java) and [ConnectionFactory](../src/main/java/repository/ConnectionFactory.java).

- An **interface** is a contract describing operations that implementations must provide.
- **Abstraction** means exposing useful operations while hiding their internal details.
- `Repository` describes operations such as adding an item and finding it by ID.
- `JdbcPartRepository` implements that contract using SQL.
- **Polymorphism** means code can work through the contract while different implementations supply the actual behavior.

```java
Repository<SparePart, String> parts = new JdbcPartRepository(connections);
```

- The variable is described by the repository interface.
- The actual object is a JDBC implementation.
- Calling `parts.findAll()` runs the implementation's method.
- `@Override` tells the compiler that a method is implementing or overriding a declared method.
- The project demonstrates interface-based polymorphism. It does not have a large custom superclass/subclass hierarchy.

**Explain it simply:** "An interface tells us what a component can do. Its implementation tells us how it does it."

## 9. Dependency injection, without a framework

- A **dependency** is another component a class needs to do its work.
- **Dependency injection** means passing that component in instead of constructing or locating it inside every method.
- `RepairShopService` receives a `ConnectionFactory` through its constructor.
- `PartRequestProcessor` receives a handler that performs stock usage.
- Production code can supply MySQL connections; tests can supply H2 connections or a function that throws an error.
- This makes testing easier because the class does not have to decide which database to use.
- No special framework is required for constructor-based dependency injection.

**Explain it simply:** "Give a worker the tool it needs when you create the worker, rather than making it search for the tool itself."

## 10. Generics: reusable code with type safety

**Used here:** [Repository](../src/main/java/repository/Repository.java) and [Transactions](../src/main/java/repository/Transactions.java).

```java
Repository<SparePart, String>
Repository<RepairJob, String>
```

- A **generic** type uses placeholders for types.
- In `Repository<T, ID>`, `T` means the stored item type and `ID` means its identifier type.
- For parts, `T` becomes `SparePart`; for jobs, it becomes `RepairJob`.
- The same contract works for both, while the compiler rejects inappropriate types.
- `List<SparePart>` means a list whose elements are spare parts.
- Java generics use reference types, so `Integer` is used instead of primitive `int` in a generic argument.
- Java can automatically convert between `int` and `Integer`; this is called boxing/unboxing.
- `Transactions.run` is also generic: it can return whichever result type its operation produces.
- Generics help reuse code without giving up type checking.

**Explain it simply:** "Generics are labeled containers: a container labeled SparePart should not receive a RepairJob."

## 11. Collections: holding groups of data

**Used here:** [JdbcJobRepository](../src/main/java/repository/JdbcJobRepository.java), [JdbcShopStore](../src/main/java/repository/JdbcShopStore.java), and [ConsoleMenu](../src/main/java/cli/ConsoleMenu.java).

| Collection | Simple meaning | Project use |
| --- | --- | --- |
| List | Ordered sequence; duplicates are allowed | Parts and job usage entries |
| ArrayList | A resizable list implementation | Building lists while reading database or CSV rows |
| Map | Associates a key with a value | Finding accumulated data by job ID |
| HashMap | Map using hashing; no defined iteration order | Job ID to its list of usages |
| LinkedHashMap | Map preserving insertion order by default | Pending requests and ordered job assembly |
| Set | Collection of distinct elements | Detecting repeated IDs during import |
| HashSet | Hash-based set | Checking whether an imported part ID was already seen |

- A map lookup is like using a roll number to find a student's record.
- **Hashing** transforms a key into a value used to choose where to look internally.
- Hash-based lookup is generally fast, but it is not a promise that every operation takes identical time.
- A map has one value per key. That value can itself be a list containing many items.
- Two usage entries for the same part can be legitimate separate actions, so a list is suitable for them.
- `List.copyOf` and `toList()` produce unmodifiable lists in the usages shown here.
- **Unmodifiable** means callers cannot add or remove elements through that list; it is not a general guarantee that every possible element type is immutable.

**Explain it simply:** "Use a list for a sequence, a map for lookup by key, and a set when duplicates should not be kept."

## 12. Optional: representing a value that may be absent

- Searching for an unknown part may find nothing.
- `Optional<SparePart>` explicitly represents either a part or an empty result.
- Repository methods return an Optional instead of requiring a raw `null` result.
- The service uses `orElseThrow(...)` to turn absence into a clear "Unknown part ID" error.
- Optional does not create missing data; it makes the possibility of absence visible in the method's return type.

**Explain it simply:** "Optional is a box that may contain a result. We must decide what to do when it is empty."

## 13. Validation and normalization

**Used here:** [Validation](../src/main/java/model/Validation.java).

- **Validation** checks whether input follows the rules.
- **Normalization** converts equivalent input into a consistent form.
- IDs must not be blank, must fit within the maximum length, and may contain only letters, digits, underscores, or hyphens.
- The pattern `[A-Za-z0-9_-]+` is a regular expression describing those allowed characters.
- Leading and trailing whitespace is removed.
- IDs are converted to uppercase using `Locale.ROOT`, so the transformation does not depend on a user's language settings.
- `p101` and `P101` therefore become the same normalized ID.
- Starting stock must be zero or greater; usage quantity must be greater than zero.
- Names and descriptions must be nonblank and within their length limits.
- Model validation protects Java operations; database constraints provide another layer of protection for stored data.

**Explain it simply:** "Validation rejects invalid input. Normalization makes valid input consistent."

## 14. Exceptions and error handling

**Used here:** throughout the application, especially [ConsoleMenu](../src/main/java/cli/ConsoleMenu.java).

- An **exception** reports that an operation could not complete normally.
- `throw` raises an exception.
- `throws` in a method declaration tells callers which checked exceptions may escape.
- `try` surrounds work that may fail; `catch` handles a matching failure.
- `finally` runs when leaving a try/catch in normal Java execution and is often used for cleanup.

| Exception | Meaning here |
| --- | --- |
| IllegalArgumentException | Invalid ID, quantity, duplicate item, or unavailable stock |
| NumberFormatException | Text could not be parsed as the expected number |
| SQLException | Database operation or connection failed |
| IOException | File operation failed |
| RejectedExecutionException | Queue is full or processor is closed |
| CompletionException | A background result failed; its cause explains the underlying error |
| InterruptedException | A waiting thread was interrupted |
| NoSuchElementException | Console input ended before another line could be read |

- **Checked exceptions**, such as IOException and SQLException, generally must be caught or declared.
- **Unchecked exceptions**, such as IllegalArgumentException, are RuntimeException subclasses and do not have that compiler requirement.
- NumberFormatException is a subclass of IllegalArgumentException, so a separate catch for it must come first.
- Catching an error is not the same as undoing a database change. Rollback is a separate operation.
- The CLI displays understandable errors instead of falsely printing success.
- A SQL state is a short database error category code, useful for diagnosis.
- If rollback itself fails, `Transactions` adds that failure as a suppressed exception so the original failure is retained.

**Explain it simply:** "Exceptions let a failure travel to the part of the program that can handle or explain it."

## 15. Resources, AutoCloseable, and try-with-resources

- A database connection, file writer, and worker pool need cleanup.
- Forgetting cleanup can leave resources occupied after their useful work is done.
- `AutoCloseable` defines a `close()` operation.
- **Try-with-resources** automatically calls close when leaving its block, including when an exception occurs.

```java
try (var connection = connections.open()) {
    // Work with the database connection.
}
```

- JDBC connections, statements, and result sets are closed this way.
- File readers and writers are also closed this way.
- `PartRequestProcessor` implements AutoCloseable so startup code can safely manage its lifetime.
- Its close method stops new submissions and waits for accepted requests to finish.

**Explain it simply:** "Borrow a resource, use it, and return it automatically even if something goes wrong."

## 16. Database, MySQL, SQL, JDBC, and Workbench

These names refer to different things:

| Term | Meaning |
| --- | --- |
| Database | Organized stored information |
| MySQL Server | The program that manages MySQL databases and accepts connections |
| SQL | Language used to read and change relational database data |
| JDBC | Java interfaces for communicating with databases |
| MySQL Connector/J | Driver implementing the MySQL side of JDBC communication |
| MySQL Workbench | A separate graphical tool for administering and querying MySQL |
| H2 | A different database used by this project's automated tests |

- Installing a JDBC driver does not install or start a MySQL server.
- Opening Workbench does not automatically connect this Java application.
- The application needs a server address, database name, username, and password.
- MySQL keeps committed application records across Java application restarts, assuming its database files remain intact.
- Normal startup uses MySQL. Explicit `--local` startup uses an embedded H2 file database in the project's data folder; it is not an automatic fallback after a MySQL error.

**Explain it simply:** "SQL is the language, MySQL is the database system, and JDBC plus the driver lets Java talk to it."

## 17. Tables, rows, columns, and keys

**Used here:** [schema.sql](../src/main/resources/db/schema.sql).

- A **table** stores one category of records.
- A **row** is one stored record.
- A **column** is one field, such as name or quantity.
- A **primary key** uniquely identifies a row and cannot be null.
- A **foreign key** links a row to an existing row in another table.

| Table | Main columns | Purpose |
| --- | --- | --- |
| spare_parts | id, name, quantity | Current inventory |
| repair_jobs | id, device, issue | Repair job details |
| part_usage | usage_id, job_id, part_id, quantity | Individual part-use actions |

- `usage_id` is an automatically generated number identifying each usage entry.
- `job_id` refers to a repair job; `part_id` refers to a spare part.
- One job can have many usage entries. One part can appear in many usage entries.
- Repeated usage actions remain separate records; the report can later total them.
- `NOT NULL` requires a value.
- `CHECK (quantity >= 0)` protects stock from negative values.
- `CHECK (quantity > 0)` requires positive usage quantities.
- A foreign key prevents a usage row from referring to a nonexistent job or part.
- `VARCHAR` stores bounded text; `INT` and `BIGINT` store integer values; `TEXT` stores longer text.

**Explain it simply:** "Keys connect the usage history to the correct job and spare part."

## 18. Basic SQL and prepared statements

**Used here:** [JdbcPartRepository](../src/main/java/repository/JdbcPartRepository.java) and [JdbcJobRepository](../src/main/java/repository/JdbcJobRepository.java).

| SQL operation | Meaning |
| --- | --- |
| CREATE TABLE | Define a table |
| INSERT | Add a row |
| SELECT | Read rows |
| UPDATE | Change stored values |
| DELETE | Remove rows; used in integration-test cleanup |
| WHERE | Restrict which rows an operation affects |
| ORDER BY | Specify result ordering |
| LEFT JOIN | Combine matching rows while retaining unmatched rows from the left table |

```java
var query = connection.prepareStatement(
        "SELECT id, name, quantity FROM spare_parts WHERE id = ?");
query.setString(1, "P101");
```

- `?` is a parameter placeholder, not a quoted value in the SQL text.
- `setString(1, ...)` supplies the first parameter; parameter positions start at 1.
- A **prepared statement** keeps bound values separate from the SQL command structure.
- This allows quotes in names to be handled as data and helps prevent SQL injection through those values.
- **SQL injection** means malicious input changes the meaning of an SQL command built unsafely from text.
- `executeQuery()` returns a ResultSet containing rows.
- `rows.next()` moves to the next row; getters read its columns.
- `executeUpdate()` is used for changes such as INSERT or UPDATE and returns an affected-row count.
- The job repository uses LEFT JOIN so a newly created job still appears even when it has no part usages.

**Explain it simply:** "Prepared statements send the command and its values separately, instead of treating user input as SQL code."

## 19. Transactions: keeping related changes together

**Used here:** [Transactions](../src/main/java/repository/Transactions.java) and [JdbcShopStore](../src/main/java/repository/JdbcShopStore.java).

Using one port requires two related changes:

1. Reduce P101 stock from 2 to 1.
2. Add a usage entry saying J101 used one P101.

- A **transaction** groups database operations into one unit.
- `setAutoCommit(false)` stops each statement from immediately committing on its own.
- `commit()` makes the transaction's changes committed.
- `rollback()` undoes the transaction's uncommitted changes.
- If stock changes but the usage insert fails, rollback restores the stock change too.
- CSV import uses a transaction so a duplicate ID does not leave half the file imported.
- Parsing and validating the CSV first avoids opening a transaction for malformed input.

The standard ACID terms:

| Letter | Meaning | Simple interpretation |
| --- | --- | --- |
| A | Atomicity | The transaction's changes succeed together or are rolled back together |
| C | Consistency | Declared rules and application invariants should remain satisfied |
| I | Isolation | Concurrent transactions interact under defined isolation and locking rules |
| D | Durability | Committed changes survive as provided by the database's durability configuration |

- MySQL tables use InnoDB here because transactions and row locking are required.
- A transaction alone is not a substitute for correct locking and validation.
- If a connection is lost during commit, the caller may not know whether commit succeeded. That is why the CLI asks users to check stored state before retrying an uncertain operation.

**Explain it simply:** "Reducing stock and recording its use are one business action, so both changes must stay together."

## 20. Threads, concurrency, and the producer-consumer pattern

**Used here:** [PartRequestProcessor](../src/main/java/service/PartRequestProcessor.java).

- A **thread** is a path of execution within a program.
- **Concurrency** means multiple tasks can be in progress during overlapping periods.
- Actual simultaneous execution depends on available processor resources and scheduling.
- The console thread can submit a request while worker threads handle accepted requests.
- A **producer** creates work; a **consumer** takes work and performs it.
- Here, the console produces part-usage requests and workers consume them.
- `ExecutorService` manages workers instead of creating a new thread manually for every request.
- `Executors.newFixedThreadPool(2)` creates a pool configured for two workers.

```text
Console submits requests --> bounded queue --> worker A --> database
                                         \-> worker B --> database
```

- The configured queue is an `ArrayBlockingQueue` with capacity 100.
- **Bounded** means there is a maximum number of waiting requests.
- `offer` tries to add a request immediately and returns false when the queue is full.
- `poll` waits briefly for an item and returns null if none becomes available in that time.
- The CLI also limits uncollected request results to 100, independently of the waiting queue capacity.
- FIFO removal from the queue does not guarantee completion order with multiple workers.
- More threads do not automatically make every task faster; tasks can still wait on database locks.

**Explain it simply:** "The queue is a tray of work orders, and the worker threads are technicians taking orders from it."

## 21. Race conditions, synchronized, volatile, and AtomicLong

- A **race condition** happens when correctness depends on uncontrolled timing between operations.
- Suppose stock is 1 and two workers both read 1 before either updates it.
- Both might approve usage, even though there is only one part.
- This is the overselling problem the application must prevent.
- A `synchronized` method or block allows only one thread at a time to execute while holding the same monitor lock.
- Submission and shutdown coordinate through the processor's monitor so a request is not accepted after shutdown has closed submissions.
- `volatile boolean accepting` makes updates to the acceptance flag visible across threads.
- Volatile visibility does not make a sequence such as read-check-write atomic.
- `AtomicLong.incrementAndGet()` increases a request counter atomically and returns the new value.
- Its IDs identify submissions within that processor instance; they are not durable database IDs.

**Explain it simply:** "Synchronization controls who may enter a critical operation, volatile shares the latest flag value, and AtomicLong safely updates a counter."

## 22. Java locks versus database row locks

**Used here:** [JdbcShopStore](../src/main/java/repository/JdbcShopStore.java).

- The store has 64 Java lock objects and chooses one based on a part ID's hash.
- This technique is called **lock striping**: many IDs share a fixed number of locks.
- Requests for the same normalized ID use the same local lock within that store.
- Different IDs can share a lock too, so some unrelated work can wait unnecessarily.
- These local locks cannot protect against another application process or another store instance.
- The database query `SELECT ... FOR UPDATE` locks the selected stock row during the transaction.
- Competing transactions needing that row must wait or fail according to the database's rules and timeout settings.
- After the first transaction commits, the next locking read can work with the updated stock.
- The row lock is what protects shared stock across separate service instances and JVM processes.

**Explain it simply:** "A Java lock coordinates local workers. A database lock coordinates everyone using that database row."

## 23. Futures, asynchronous results, and graceful shutdown

- **Asynchronous** means the caller can receive an acknowledgement before the requested work finishes.
- A **future** represents a result that may become available later.
- The queue returns a Submission with a request ID and a `CompletionStage<Integer>`.
- The successful Integer result is the remaining stock after the transaction.
- Internally, a CompletableFuture is completed either with a value or with an exception.
- The returned minimal completion stage prevents the caller from controlling the internal completion directly.
- `join()` waits for a result and throws CompletionException if the operation failed.
- Menu option 11 waits for submitted results and prints their outcomes.
- "Queued" does not mean "committed". Reports may run before earlier queued work finishes.
- On normal exit, accepted work is finished before the application returns.
- `shutdown()` stops new executor tasks; the worker loops drain accepted queue items.
- `awaitTermination()` waits for workers to finish.
- The code restores interrupt status when appropriate instead of silently forgetting interruption.
- This queue exists only in memory. A crash or forced termination can lose accepted requests that were not committed; it is not a durable message queue.

**Explain it simply:** "A request receipt says the work was accepted. Its future tells us whether the work actually finished successfully."

## 24. Functional interfaces, lambdas, and method references

**Used here:** [PartUsageHandler](../src/main/java/service/PartUsageHandler.java), [ConnectionFactory](../src/main/java/repository/ConnectionFactory.java), and [ReportService](../src/main/java/service/ReportService.java).

- A **functional interface** has one abstract method and can be implemented by a lambda.
- `@FunctionalInterface` asks the compiler to check that rule.
- A **lambda** is a short way to supply behavior where a compatible functional interface is expected.

```java
part -> part.quantity() <= threshold
```

- Left of `->`: the input parameter.
- Right of `->`: what to compute using it.
- This expression answers true or false for one part.

```java
shop::recordUsage
```

- `::` creates a **method reference** to compatible existing behavior.
- Here it supplies the stock-usage handler to the processor.
- It does not call recordUsage immediately; workers call it later with request arguments.
- That supplied operation is a **callback**: behavior another component invokes when needed.

**Explain it simply:** "A lambda passes a small piece of behavior. A method reference passes behavior we already named as a method."

## 25. Streams: filtering, transforming, and summarizing

**Used here:** [ReportService](../src/main/java/service/ReportService.java).

- A **stream** is a pipeline for processing elements from a source such as a list.
- It is not the database and is not the collection storing the original data.
- Stream intermediate operations are generally lazy: they describe work performed when a terminal operation runs.
- These report streams are sequential; using a stream does not automatically create background workers.

```java
shop.listParts().stream()
        .filter(part -> part.quantity() <= threshold)
        .sorted(ReportComparators.LOW_STOCK)
        .toList();
```

Read this pipeline as four steps:

1. Obtain the inventory and begin processing its elements.
2. Keep only parts whose stock is at or below the threshold.
3. Order the remaining parts using the report comparator.
4. Collect them into a result list.

Other operations in the usage report:

- `map`: transform one item into another item.
- `flatMap`: turn many inner sequences into one sequence; here, usages from many jobs become one stream of report rows.
- `groupingBy`: gather rows with the same job ID and part ID.
- `summingLong`: add the quantities in each group using a long total.
- `entrySet`: access a map's key-value pairs.
- `collect`: finish processing by accumulating a result.
- `forEach`: perform an action for each element, such as printing a report row.
- `JobPart` is a record used as a compound grouping key. Record equality compares the two components, so equal job/part pairs group together.

Example: J101 uses P101 once, then uses it once again. The history has two actions; the report shows a total of 2.

- `long` can hold larger totals than `int`, reducing the risk of overflow when adding many int quantities. It still has a finite range.
- These reports load data into Java memory before processing. Very large datasets may be better filtered or aggregated in SQL.

**Explain it simply:** "A stream describes a chain of steps: keep these items, transform them, group them, and produce a result."

## 26. Comparator and sorting

**Used here:** [ReportComparators](../src/main/java/service/ReportComparators.java).

- A Comparator defines how to order two values.
- A comparison result is negative when the first goes before the second, zero when tied under that comparison, and positive when it goes after.
- `comparingInt(SparePart::quantity)` orders parts by stock quantity.
- `thenComparing(SparePart::id)` breaks a quantity tie using the part ID.
- Usage rows are ordered by job ID, then part ID.
- Stable, explicit ordering makes reports easier to understand and tests easier to reproduce.
- IDs are compared as strings, so this is not a special numeric sort of the digits inside an ID.

Example: P2 has stock 1, P1 has stock 1, P3 has stock 5. The report orders them P1, P2, P3.

**Explain it simply:** "A comparator is the sorting rule. Here, lower stock comes first, and IDs resolve ties."

## 27. File handling, CSV, encoding, and safe replacement

**Used here:** [CsvFiles](../src/main/java/csv/CsvFiles.java) and [AtomicFileWriter](../src/main/java/fileio/AtomicFileWriter.java).

- **File I/O** means reading information from files and writing information to files.
- A Path represents a filesystem location.
- Buffered readers and writers reduce the overhead of handling text in tiny pieces.
- **UTF-8** is the encoding used to represent text as bytes, including many languages and symbols.
- **CSV** means comma-separated values: a text format containing rows and columns.

```csv
id,name,quantity
P101,USB-C Port,2
P102,"Screen, small",5
```

- The header names the columns and must be exactly `id,name,quantity` for inventory imports.
- Quotes allow a comma to be part of a field instead of a column separator.
- Fields may also contain escaped quotes or line breaks, so simply splitting every line on commas is insufficient.
- Apache Commons CSV handles those formatting rules.
- The reader accepts an optional UTF-8 byte-order mark at the start of a file.
- Every row is validated, and the import is limited to 100,000 parts.
- Import adds new IDs; it does not restock or overwrite existing IDs.
- Inventory and low-stock exports use inventory columns; usage export uses `job_id,part_id,quantity_used`.

Export replacement works in this order:

1. Create the destination directory if needed.
2. Create a temporary file beside the destination.
3. Write the complete report and close the writer.
4. Request an atomic move to replace the destination.
5. Clean up the temporary file in a finally block.

- **Atomic replacement** means the destination changes as one filesystem operation rather than exposing a partially written new report.
- Atomic move support depends on the filesystem. The code reports an IOException if that operation is unsupported; it does not fall back to a non-atomic copy.
- This is file protection, separate from SQL transactions.
- CSV files are import/export artifacts. MySQL is the default persistent store; `--local` mode persists data in an H2 database file.

**Explain it simply:** "CSV transfers table-like data. We finish writing a temporary report before replacing the old one."

## 28. Startup, configuration, and the MySQL connection

**Used here:** [Main](../src/main/java/cli/Main.java), [DatabaseConfig](../src/main/java/repository/DatabaseConfig.java), and [JdbcSchema](../src/main/java/repository/JdbcSchema.java).

Startup proceeds like this:

1. Read environment variables.
2. Create a factory that opens JDBC connections.
3. Connect and create missing application tables.
4. Create the service and two-worker request processor.
5. Start the console menu.
6. Close workers after the menu finishes.

- In `--local` mode, startup uses a built-in local H2 configuration instead of reading MySQL environment variables. `run-local.cmd` selects this mode and anchors the working directory to the project folder. The file `data/repair-shop-local.mv.db` holds the sample data between runs.

Connection settings:

| Setting | Purpose | Default / requirement |
| --- | --- | --- |
| REPAIR_DB_URL | Server, port, database, and connection options | Localhost, port 3306, database repair_shop |
| REPAIR_DB_USER | Database account name | repair_app |
| REPAIR_DB_PASSWORD | Database account password | Must be supplied in the environment |

- An **environment variable** is a named value made available to a running process.
- Setting one in a terminal affects programs launched from that terminal, not automatically an already-running IDE.
- `localhost` means the machine running Java; a port identifies a listening service on that machine.
- `connectTimeout` limits connection establishment waiting; `socketTimeout` limits waiting for socket reads. Neither is a complete end-to-end deadline for every operation.
- The JDBC factory opens connections when operations need them; this project does not use a connection pool.
- DatabaseConfig overrides toString so printing the configuration object does not reveal its credentials.
- JdbcSchema creates missing tables, not the database account or database itself.
- `CREATE TABLE IF NOT EXISTS` makes initialization repeatable but does not migrate existing tables to a changed schema.
- The initialization checks MySQL tables use InnoDB.

Why a project can have JDBC code but still not connect:

- The server may be stopped or at a different port.
- The named database may not exist.
- The password may not be available to the Java process.
- The account may have the wrong password or insufficient privileges.
- The MySQL integration test may simply be disabled because its separate settings are absent.

For exact setup SQL and PowerShell commands, follow [the project README](../README.md#connect-to-mysql-on-windows).

**Explain it simply:** "JDBC provides the connection mechanism; correct server settings and credentials allow that mechanism to work."

## 29. Maven, dependencies, compilation, and the executable JAR

**Used here:** [pom.xml](../pom.xml).

- **Source code** is the Java text written by a programmer.
- The Java compiler translates source into class files containing bytecode.
- The **JVM**, or Java Virtual Machine, executes that bytecode.
- A **JAR** packages Java classes and resources into one archive.
- **Maven** automates compilation, testing, dependency management, and packaging.
- `pom.xml` declares how the project is built and which libraries it needs.
- A **dependency** here means an external library used by the project.

| Dependency | Job |
| --- | --- |
| MySQL Connector/J | JDBC communication with MySQL |
| Apache Commons CSV | CSV parsing and writing |
| H2 | Database used by automated tests and the optional local file database |
| JUnit Jupiter | Test framework |

- The compiler release target is Java 17.
- `mvn test` compiles code and runs tests.
- `mvn package` also packages the application after testing by default.
- `mvn -o ...` requests offline mode, which requires the needed dependencies/plugins to already be cached locally.
- The Shade plugin builds a JAR containing runtime dependencies.
- Its manifest identifies `cli.Main` as the entry point.
- Run the packaged app using `java -jar target/repair-shop-2.0.0.jar` after configuring MySQL.

**Explain it simply:** "Maven gathers the libraries, builds the Java code, runs tests, and packages an application we can launch."

## 30. Testing: checking behavior automatically

**Used here:** [test source directory](../src/test/java).

- A **test** runs an operation and checks its result against an expectation.
- `@Test` marks a JUnit test method.
- An **assertion** expresses what must be true, such as remaining stock being 1.
- `assertThrows` verifies that an invalid operation raises the expected exception.
- `@BeforeEach` prepares a fresh setup before each test in a class.
- `@TempDir` supplies a temporary directory for file tests.
- H2 runs inside the test process, with separate in-memory databases for isolation.
- MySQL compatibility mode makes some H2 behavior resemble MySQL; it does not make H2 identical to MySQL.
- The suite tests duplicate IDs, invalid input, rollback, CSV quoting, reports, console workflows, queue saturation, and concurrent stock claims.
- A rollback test deliberately causes a usage insert to fail after stock deduction, then checks that stock was restored.
- Concurrency tests check that competing requests cannot successfully consume more stock than exists.
- CountDownLatch helps coordinate a concurrent start; timed future waits prevent tests from waiting forever for results.
- A dedicated MySQL test verifies row-lock behavior against a real server when explicitly configured.
- Its settings start with `REPAIR_TEST_MYSQL_`, separate from normal application settings.
- **Skipped** means the test did not execute. It is neither a passed connection test nor a failed connection attempt.
- Passing tests increase confidence; they do not prove there can never be a bug.

**Explain it simply:** "Tests are repeatable experiments that check whether the program still follows its rules."

## 31. Extra learning: Comparable, TreeMap, and PriorityQueue

These topics were mentioned in the earlier topic list. They are **not implemented as application features here**.

### Comparable

- Comparable defines a type's natural ordering inside the type itself through `compareTo`.
- For example, a hypothetical Student class could naturally order students by roll number.
- Comparator defines an ordering separately, so one type can have many sorting rules.
- This project uses Comparator for reports and has no custom Comparable implementation.
- **Explain it simply:** "Comparable gives a type its usual order; Comparator supplies a chosen order from outside."

### TreeMap

- TreeMap is a map that keeps keys sorted according to their natural order or a supplied comparator.
- HashMap provides no defined key iteration order.
- LinkedHashMap normally preserves insertion order.
- TreeMap's basic lookup and update operations are typically logarithmic in the number of entries.
- It could help if a feature needed repeated traversal by sorted keys, but this project builds report order using sorting.
- **Explain it simply:** "TreeMap is a key-value collection that keeps its keys in sorted order."

### PriorityQueue

- PriorityQueue selects the next item according to priority ordering, rather than arrival order.
- By default its head is the least element according to its ordering.
- Iterating over it does not guarantee a fully sorted sequence.
- It could model urgent repair requests if the application added that feature.
- The ordinary Java PriorityQueue is not thread-safe; it cannot simply replace the existing concurrent queue without further design.
- This project uses ArrayBlockingQueue, not PriorityQueue.
- **Explain it simply:** "A normal waiting line serves the next arrival; a priority queue selects the next item by its priority rule."

## 32. Extra learning: custom exceptions, StringBuilder, and formatting

### Custom exceptions

- A custom exception is an exception class you define for a domain-specific failure.
- A hypothetical `InsufficientStockException` could distinguish stock shortages from other invalid requests.
- Extending RuntimeException makes it unchecked; extending Exception directly normally makes it checked.
- It can make callers' catch logic more specific when that distinction is useful.
- The current project uses standard exceptions and does not define this class.
- **Explain it simply:** "A custom exception gives a particular business failure its own recognizable type."

### StringBuilder

- Java Strings are immutable: changing text produces a new String value rather than modifying the old one.
- StringBuilder provides a mutable buffer for building text through repeated appends.

```java
StringBuilder message = new StringBuilder();
message.append("Part: ").append("P101");
message.append(" | Stock: ").append(2);
System.out.println(message.toString());
```

- This is a teaching example, not code currently used explicitly in the application.
- StringBuilder can be useful when assembling many pieces, especially in loops.
- Simple `+` expressions are still appropriate for short messages; there is no need to replace every concatenation.
- **Explain it simply:** "StringBuilder is a reusable text buffer we can keep adding to before producing the final string."

### Output formatting

- Formatting controls how values appear to the reader.
- A hypothetical `System.out.printf("Stock: %d%n", 2)` prints an integer followed by a line separator.
- `%s` is commonly used for a string representation and `%d` for an integer.
- The current console mainly builds messages through concatenation.
- CSV formatting is already implemented through Commons CSV and must follow CSV rules, not just visual spacing.
- **Explain it simply:** "Formatting is how we arrange data for display or for a required file format."

## 33. Follow one request through the entire project

Suppose P101 has stock 2, J101 already exists, and the user requests one P101:

1. ConsoleMenu reads the job ID, part ID, and quantity.
2. PartRequestProcessor validates the request values and tries to enqueue it.
3. A Submission gives the console a request ID and future result.
4. A worker takes the request from the queue.
5. The PartUsageHandler callback invokes RepairShopService.recordUsage.
6. JdbcShopStore chooses the local stock lock and starts a database transaction.
7. It checks that job J101 exists.
8. It reads P101 stock with `SELECT ... FOR UPDATE`, locking the row.
9. It checks that 1 is no greater than the available stock of 2.
10. It updates stock to 1 and inserts the usage entry.
11. Transactions commits the changes.
12. The worker completes the future with remaining stock 1.
13. Option 11 waits for that result and prints success.
14. Later, the report reads persisted jobs and groups usage by job and part.

If step 10 fails, the transaction rolls back. The future records failure, and the CLI displays an error instead of confirming success.

## 34. Short answers for explaining the project to someone else

| Question | Beginner-friendly answer |
| --- | --- |
| What does your project do? | It tracks repair jobs, spare-part stock, and which parts each job uses. |
| Why separate classes? | Each class handles a focused responsibility, making the code easier to follow and test. |
| Where is OOP used? | Models represent data, services perform operations, and repositories handle storage. |
| Where are generics used? | Repository works with different entity types while keeping type checking. |
| Why use an interface? | It defines a contract so callers can use a component without depending on every implementation detail. |
| Why use JDBC? | It lets Java execute SQL and read results from the database through a driver. |
| Why use transactions? | Stock deduction and usage history must succeed or fail together. |
| Why use threads? | Background workers process accepted requests while the console can continue receiving input. |
| Why use database locks? | Multiple workers or applications must not claim the same remaining stock. |
| What do streams do? | They filter, group, total, and sort report data. |
| Why write a temporary file? | The complete new export is written before the old destination is replaced. |
| Does queued mean saved? | No. The request's completed result confirms whether the database operation succeeded. |
| Does a skipped MySQL test mean JDBC is broken? | No. It means the test did not run because its enabling configuration was absent. |
| Is every discussed syllabus topic implemented? | No. The extra-learning sections explain topics that the current code does not use. |

## 35. Check your understanding

Try answering before reading the answers below:

1. Stock is 2 and a user requests 3. What should change in the database?
2. Stock deduction succeeds but the usage insert fails. What restores consistency?
3. Why is a Java synchronized block insufficient when two separate application processes use the same database?
4. Does a stream automatically use multiple threads?
5. What is the difference between the queue receipt and the completed future?
6. Can creating the tables succeed if the named database does not exist?
7. Does an immutable SparePart object prevent database stock updates?
8. Why is splitting CSV text on every comma unreliable?
9. Where should you study sorting separately from report aggregation?
10. Which discussed topics are extra learning rather than current features?

Answers:

1. Nothing should change; insufficient stock is rejected.
2. The transaction rolls back the uncommitted stock deduction.
3. Each process has its own Java locks; database row locks coordinate access to shared rows.
4. No. The report pipelines here are sequential.
5. The receipt confirms acceptance; the completed future provides the operation's result or failure.
6. No. The database and account need to be set up first.
7. No. The database row can change, and a new object can represent the updated state.
8. A quoted field can contain a comma or line break as data.
9. ReportComparators defines order; ReportService performs filtering and aggregation.
10. Custom Comparable, TreeMap, PriorityQueue, custom exceptions, and explicit StringBuilder usage are not implemented here.
