package io.konveyor.demo.config;

import javax.annotation.sql.DataSourceDefinition;

/**
 * Test class for annotated element matching with different database driver.
 * Tests matching className = "com.mysql.jdbc.Driver".
 */
@DataSourceDefinition(
    name = "java:app/MySQLDataSource",
    className = "com.mysql.jdbc.Driver",
    url = "jdbc:mysql://localhost:3306/testdb",
    user = "root",
    password = "root"
)
public class MySQLDataSourceConfig {

    public void initialize() {
        System.out.println("MySQL DataSource configured");
    }
}
