package com.retailai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class TestDatabaseSafetyTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String configuredDatasourceUrl;

    @Test
    @DisplayName("Verify that test execution targets retailai_test_db and never retailai_db")
    void testExecutionUsesIsolatedTestDatabase() throws SQLException {
        assertNotNull(configuredDatasourceUrl, "Datasource URL must not be null");
        assertTrue(configuredDatasourceUrl.contains("retailai_test_db"),
                "Configured datasource URL must target retailai_test_db. Actual: " + configuredDatasourceUrl);
        assertFalse(configuredDatasourceUrl.endsWith("/retailai_db"),
                "Configured datasource URL MUST NOT target development retailai_db!");

        try (Connection connection = dataSource.getConnection()) {
            String activeDbUrl = connection.getMetaData().getURL();
            assertNotNull(activeDbUrl, "Active connection database URL must not be null");
            assertTrue(activeDbUrl.contains("retailai_test_db"),
                    "Active connection URL must target retailai_test_db. Actual: " + activeDbUrl);
            assertFalse(activeDbUrl.endsWith("/retailai_db"),
                    "Active connection URL MUST NOT target development retailai_db!");
        }
    }
}
