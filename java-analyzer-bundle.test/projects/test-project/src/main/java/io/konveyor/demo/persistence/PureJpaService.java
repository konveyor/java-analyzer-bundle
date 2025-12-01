package io.konveyor.demo.persistence;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Test class for variable binding.
 * This file has EntityManager but NO PreparedStatement.
 * Should be found when:
 * - Searching for EntityManager (saved as variable)
 *
 * Should NOT be found when:
 * - Searching for PreparedStatement (even with EntityManager file filter)
 */
public class PureJpaService {

    @PersistenceContext
    private EntityManager entityManager;

    public void performJpaOperations() {
        // Uses only JPA EntityManager, no JDBC
        entityManager.persist(new Object());

        Query query = entityManager.createQuery("SELECT u FROM User u");
        query.getResultList();
    }
}
