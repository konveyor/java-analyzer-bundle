package io.konveyor.demo.inheritance;

import io.konveyor.demo.annotations.CustomAnnotation;

/**
 * Service that extends BaseService - for testing inheritance searches.
 */
@CustomAnnotation(value = "DataService", version = "2.0")
public class DataService extends BaseService implements AutoCloseable {

    private boolean initialized = false;

    public DataService() {
        super();
        this.serviceName = "DataService";
    }

    @Override
    public void initialize() {
        this.initialized = true;
        System.out.println("DataService initialized");
    }

    @Override
    public void close() throws Exception {
        this.initialized = false;
        System.out.println("DataService closed");
    }

    public boolean isInitialized() {
        return initialized;
    }
}
