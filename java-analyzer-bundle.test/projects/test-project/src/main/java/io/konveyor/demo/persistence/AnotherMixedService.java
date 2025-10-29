package io.konveyor.demo.persistence;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Test class for variable binding - second file with both imports.
 * This file has BOTH EntityManager AND PreparedStatement imports.
 * Should be found when searching for PreparedStatement filtered by EntityManager files.
 */
public class AnotherMixedService {

    @PersistenceContext
    private EntityManager entityManager;

    public void processData(Connection conn) throws SQLException {
        // Another example of mixed JPA/JDBC usage
        PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET status = ?");
        pstmt.setString(1, "PROCESSED");
        pstmt.executeUpdate();

        entityManager.flush();
    }
}
