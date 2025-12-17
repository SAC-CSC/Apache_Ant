package platform.core.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.FileInputStream;

public class DBConnection {

    private static String URL;
    private static String USER;
    private static String PASSWORD;

    static {
        try {
            String baseDir = System.getProperty("user.dir");
            String configPath = baseDir + "/config/db.properties";

            Properties props = new Properties();
            props.load(new FileInputStream(configPath));

            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");

            System.out.println("✅ DB configuration loaded");
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to load DB configuration", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
