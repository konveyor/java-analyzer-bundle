package io.konveyor.demo.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * Test class for variable binding.
 * This file has ONLY PreparedStatement import (NO EntityManager).
 * Should NOT be found when:
 * - Searching for PreparedStatement filtered by files with EntityManager
 *
 * Should be found when:
 * - Searching for PreparedStatement without filtering
 */
public class JdbcOnlyService {

    public void executeQuery(Connection conn) throws SQLException {
        // Uses only JDBC PreparedStatement
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM products");
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            System.out.println("Product: " + rs.getString("name"));
        }

        rs.close();
        stmt.close();
    }
}
