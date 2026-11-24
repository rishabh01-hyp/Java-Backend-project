package cli;

import repository.DatabaseConfig;
import repository.JdbcSchema;
import service.PartRequestProcessor;
import service.RepairShopService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

/** Application startup: configuration, database initialization, and worker lifecycle. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        boolean local = args.length == 1 && "--local".equals(args[0]);
        if (args.length != 0 && !local) {
            System.err.println("Usage: java -jar target/repair-shop-2.0.0.jar [--local] (otherwise configure REPAIR_DB_* environment variables)");
            System.exit(1);
        }
        try {
            var connections = (local ? DatabaseConfig.localDemo() : DatabaseConfig.fromEnvironment()).connections();
            JdbcSchema.initialize(connections);
            if (local) { System.out.println("Local demo database: data/repair-shop-local.mv.db (H2)"); }
            var shop = new RepairShopService(connections);
            try (var requests = new PartRequestProcessor(shop::recordUsage, 2, 100)) {
                new ConsoleMenu(shop, requests, new Scanner(System.in), System.out).run();
            }
        } catch (SQLException e) {
            System.err.println("Cannot initialize database (SQL state " + e.getSQLState()
                    + "). " + (local ? "Check the data folder permissions and whether another app has this database open."
                    : "Check MySQL, database permissions, and REPAIR_DB_* settings."));
            System.exit(1);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Cannot start application: " + e.getMessage());
            System.exit(1);
        }
    }

}
