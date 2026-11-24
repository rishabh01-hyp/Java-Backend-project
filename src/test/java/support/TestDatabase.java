package support;

import repository.ConnectionFactory;
import repository.JdbcSchema;
import service.RepairShopService;

import java.sql.DriverManager;
import java.util.UUID;

public final class TestDatabase {
    public final ConnectionFactory connections;
    public final RepairShopService shop;

    public TestDatabase() throws Exception {
        String url = "jdbc:h2:mem:shop_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000";
        connections = () -> DriverManager.getConnection(url, "sa", "");
        JdbcSchema.initialize(connections);
        shop = new RepairShopService(connections);
    }

    public void seed() throws Exception {
        shop.addPart("P101", "USB-C Port", 2);
        shop.createJob("J101", "Phone", "Not charging");
        shop.createJob("J102", "Tablet", "Loose port");
    }
}
