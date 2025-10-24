package io.konveyor.demo;

/**
 * Enum for testing enum constant searches (location type 6).
 */
public enum EnumExample {
    ACTIVE("active", 1),
    INACTIVE("inactive", 0),
    PENDING("pending", 2),
    ARCHIVED("archived", -1);

    private final String status;
    private final int code;

    EnumExample(String status, int code) {
        this.status = status;
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }

    public static EnumExample fromCode(int code) {
        for (EnumExample e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
