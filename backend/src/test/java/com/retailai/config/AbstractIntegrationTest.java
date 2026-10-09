package com.retailai.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void verifyTestDatabaseSafetyGuard() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String dbUrl = connection.getMetaData().getURL();
            if (dbUrl == null || (dbUrl.contains("retailai_db") && !dbUrl.contains("retailai_test_db"))) {
                throw new IllegalStateException(
                    "CRITICAL SAFETY GUARD TRIGGERED: Test execution attempted on development database ('retailai_db')! " +
                    "Aborting test execution immediately to preserve development data. Current JDBC URL: " + dbUrl
                );
            }
            assertTrue(dbUrl.contains("retailai_test_db"),
                "Safety Guard Failure: Configured test database URL must contain 'retailai_test_db'. Actual: " + dbUrl);
        }
    }
}
