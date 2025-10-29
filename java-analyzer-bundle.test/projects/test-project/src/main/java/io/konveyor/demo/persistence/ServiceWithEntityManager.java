package io.konveyor.demo.persistence;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Test class for variable binding.
 * This file has BOTH EntityManager AND PreparedStatement imports.
 * Should be found when:
 * 1. Searching for EntityManager (saved as variable)
 * 2. Searching for PreparedStatement filtered by files with EntityManager
 */
public class ServiceWithEntityManager {

    @PersistenceContext
    private EntityManager entityManager;

    public void mixedPersistenceLogic(Connection conn) throws SQLException {
        // Uses JPA EntityManager
        entityManager.persist(new Object());

        // Also uses JDBC PreparedStatement (anti-pattern when using JPA)
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
        stmt.executeQuery();
    }
}
