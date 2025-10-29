package io.konveyor.demo.config;

import javax.annotation.sql.DataSourceDefinition;

/**
 * Test class for annotated element matching with @DataSourceDefinition.
 * Tests matching specific className attribute values.
 */
@DataSourceDefinition(
    name = "java:app/MyDataSource",
    className = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5432/testdb",
    user = "dbuser",
    password = "dbpass"
)
public class DataSourceConfig {

    public void initialize() {
        System.out.println("DataSource configured");
    }
}
