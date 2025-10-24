package io.konveyor.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import io.konveyor.demo.annotations.CustomAnnotation;
import io.konveyor.demo.inheritance.BaseService;

/**
 * Sample application class for testing various JDT.LS search patterns.
 * This class demonstrates:
 * - Method calls (location type 2)
 * - Constructor calls (location type 3)
 * - Type references (location type 10)
 * - Import statements (location type 8)
 * - Field declarations (location type 12)
 * - Variable declarations (location type 9)
 * - Method declarations (location type 13)
 */
@CustomAnnotation(value = "SampleApp", version = "1.0")
public class SampleApplication extends BaseService {

    // Field declarations - can search for String, List, File types
    private String applicationName;
    private List<String> items;
    private File configFile;

    // Static field
    private static final int MAX_RETRIES = 3;

    public SampleApplication() {
        // Constructor calls
        this.applicationName = new String("Test Application");
        this.items = new ArrayList<>();
        this.configFile = new File("config.xml");
    }

    public SampleApplication(String name) {
        this.applicationName = name;
        this.items = new ArrayList<>();
    }

    // Method declaration
    public void processData() {
        // Variable declarations
        String tempData = "temporary";
        int count = 0;

        // Method calls
        System.out.println("Processing: " + tempData);
        items.add(tempData);

        // More constructor calls
        File tempFile = new File("/tmp/data.txt");
        List<String> results = new ArrayList<>();
    }

    // Method with return type
    public String getName() {
        return applicationName;
    }

    // Method with parameters
    public void writeToFile(String data) throws IOException {
        // Constructor call with chained method call
        FileWriter writer = new FileWriter(configFile);
        writer.write(data);
        writer.close();
    }

    // Static method
    public static void printVersion() {
        System.out.println("Version 1.0");
    }

    // Method calling methods from java.io package
    public void fileOperations() throws IOException {
        // Multiple constructor calls
        File dir = new File("/tmp");
        File file1 = new File(dir, "test.txt");

        // Method calls
        if (dir.exists()) {
            dir.mkdirs();
        }

        String path = file1.getAbsolutePath();
        System.out.println(path);
    }

    @Override
    public void initialize() {
        System.out.println("Initializing SampleApplication");
    }
}
