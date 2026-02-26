package com.khathabook.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Normalizes the SPRING_DATASOURCE_URL from Railway's mysql:// format
 * to the JDBC-required jdbc:mysql:// format.
 *
 * Railway provides: mysql://user:password@host:port/database
 * JDBC requires:    jdbc:mysql://host:port/database?user=X&password=Y
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        String user   = System.getenv("DB_USER");
        String pass   = System.getenv("DB_PASSWORD");

        // If SPRING_DATASOURCE_URL is not set, use individual components
        if (rawUrl == null || rawUrl.isBlank()) {
            String host = getEnvOrDefault("MYSQLHOST", "127.0.0.1");
            String port = getEnvOrDefault("MYSQLPORT", "3306");
            String db   = getEnvOrDefault("MYSQLDATABASE", "khathabook");
            rawUrl = "jdbc:mysql://" + host + ":" + port + "/" + db;
            if (user == null) user = getEnvOrDefault("MYSQLUSER", "root");
            if (pass == null) pass = getEnvOrDefault("MYSQLPASSWORD", "");
        } else {
            // Normalize: mysql://user:pass@host:port/db → jdbc:mysql://host:port/db
            if (rawUrl.startsWith("mysql://")) {
                rawUrl = rawUrl.replace("mysql://", "");  // strip scheme
                // Extract user:pass@host:port/db
                int atIdx = rawUrl.lastIndexOf('@');
                if (atIdx > 0) {
                    String credentials = rawUrl.substring(0, atIdx);
                    String hostPortDb  = rawUrl.substring(atIdx + 1);
                    String[] creds     = credentials.split(":", 2);
                    if (user == null && creds.length >= 1) user = creds[0];
                    if (pass == null && creds.length >= 2) pass = creds[1];
                    rawUrl = "jdbc:mysql://" + hostPortDb;
                } else {
                    rawUrl = "jdbc:mysql://" + rawUrl;
                }
            }
            // If it doesn't start with jdbc:mysql:// yet
            if (!rawUrl.startsWith("jdbc:")) {
                rawUrl = "jdbc:" + rawUrl;
            }
        }

        // Append required query params if not present
        if (!rawUrl.contains("?")) {
            rawUrl += "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }

        System.out.println("🔌 [DB] Connecting to: " + rawUrl.replaceAll(":[^:@/]+@", ":****@"));

        return DataSourceBuilder.create()
                .url(rawUrl)
                .username(user != null ? user : "root")
                .password(pass != null ? pass : "")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
