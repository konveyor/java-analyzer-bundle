package io.konveyor.demo.inheritance;

import java.io.Serializable;

/**
 * Base service class for testing inheritance patterns (location type 1).
 * Also implements Serializable for testing implements_type (location type 5).
 */
public abstract class BaseService implements Serializable, Comparable<BaseService> {

    private static final long serialVersionUID = 1L;

    protected String serviceName;

    public BaseService() {
        this.serviceName = "BaseService";
    }

    /**
     * Abstract method to be implemented by subclasses.
     */
    public abstract void initialize();

    /**
     * Common method available to all services.
     */
    public void start() {
        System.out.println("Starting service: " + serviceName);
    }

    public void stop() {
        System.out.println("Stopping service: " + serviceName);
    }

    @Override
    public int compareTo(BaseService other) {
        return this.serviceName.compareTo(other.serviceName);
    }
}
