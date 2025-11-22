package io.konveyor.demo;

import java.util.List;
import java.util.ArrayList;
import javax.persistence.EntityManager;

/**
 * Example class demonstrating package usage through:
 * 1. Import statements (java.util, javax.persistence)
 * 2. Fully qualified names (java.sql.Connection, java.io.File)
 *
 * Used for testing PACKAGE location type (11) which should match
 * both import statements and fully qualified name usage.
 */
public class PackageUsageExample {

    // Using imported types
    private List<String> items = new ArrayList<>();
    private EntityManager entityManager;

    // Method using fully qualified name without import
    public java.sql.Connection getConnection() {
        return null;
    }

    // Method parameter using fully qualified name
    public void processFile(java.io.File file) {
        if (file != null) {
            System.out.println("Processing: " + file.getName());
        }
    }

    // Field using fully qualified name
    private java.util.concurrent.ConcurrentHashMap<String, Object> cache =
        new java.util.concurrent.ConcurrentHashMap<>();

    // Method with javax.persistence fully qualified name
    public void merge(Object entity) {
        if (entityManager != null) {
            entityManager.merge(entity);
        }
    }

    // Using java.time with fully qualified name
    public java.time.LocalDateTime getCurrentTime() {
        return java.time.LocalDateTime.now();
    }
}
