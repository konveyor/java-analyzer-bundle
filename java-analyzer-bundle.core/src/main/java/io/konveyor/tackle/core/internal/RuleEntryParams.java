package io.konveyor.tackle.core.internal;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class RuleEntryParams {
    
    private final String projectName;
    private final String query;
    private final int location;

    public RuleEntryParams(final String commandId, final List<Object> arguments) {
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) arguments.stream().findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(format("Command '%s' must be called with one rule entry argument!", commandId)));

        this.projectName = (String) obj.get("project");
        this.query = (String) obj.get("query");
        this.location = Integer.parseInt((String) obj.get("location"));
    }

    public String getProjectName() {
        return projectName;
    }

    public String getQuery() {
        return query;
    }

    public int getLocation() {
        return location;
    }

}
