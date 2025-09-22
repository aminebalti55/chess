package com.example.chess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@SpringBootApplication
public class ChessApplication {
    private static final Logger log = LoggerFactory.getLogger(ChessApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ChessApplication.class);
        try {
            // Start Spring
            ConfigurableApplicationContext ctx = app.run(args);

            // App is up — log port/profile
            String port = ctx.getEnvironment().getProperty("local.server.port", "8080");
            String profiles = String.join(",", ctx.getEnvironment().getActiveProfiles());
            log.info("🚀 Chess backend is UP on port {} (profile: {})", port, profiles.isEmpty() ? "default" : profiles);

            // Quick DB connectivity check
            try {
                DataSource ds = ctx.getBean(DataSource.class);
                try (Connection c = ds.getConnection()) {
                    DatabaseMetaData meta = c.getMetaData();
                    log.info("✅ DB connection OK → {} {} via {}", meta.getDatabaseProductName(),
                            meta.getDatabaseProductVersion(), meta.getDriverName());
                }
            } catch (Exception e) {
                log.error("❌ DB connection FAILED: {}", e.getMessage(), e);
            }

        } catch (Throwable t) {
            // Peel to root cause for a clean one-liner
            Throwable root = t;
            while (root.getCause() != null) root = root.getCause();
            System.err.println("💥 Application startup FAILED: " + root);
            t.printStackTrace();
        }
    }
}
