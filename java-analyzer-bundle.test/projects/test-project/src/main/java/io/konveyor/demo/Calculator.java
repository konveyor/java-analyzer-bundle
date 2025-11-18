package io.konveyor.demo;

import io.konveyor.demo.annotations.DeprecatedApi;

/**
 * Calculator class for testing return types (location type 7) and method declarations.
 */
public class Calculator {

    // Method with int return type
    public int add(int a, int b) {
        return a + b;
    }

    // Method with double return type
    public double divide(double a, double b) {
        if (b == 0) {
            throw new IllegalArgumentException("Division by zero");
        }
        return a / b;
    }

    // Method with String return type
    public String format(int number) {
        return "Number: " + number;
    }

    // Method with custom return type
    public EnumExample getStatus() {
        return EnumExample.ACTIVE;
    }

    // Deprecated method
    @DeprecatedApi(since = "1.0", replacement = "add(int, int)")
    public int sum(int a, int b) {
        return a + b;
    }

    // Method returning void
    public void reset() {
        System.out.println("Calculator reset");
    }
}
