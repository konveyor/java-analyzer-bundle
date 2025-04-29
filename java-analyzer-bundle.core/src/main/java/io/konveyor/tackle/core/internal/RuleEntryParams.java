package io.konveyor.tackle.core.internal;

import io.konveyor.tackle.core.internal.query.AnnotationQuery;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RuleEntryParams {

    private final String projectName;
    private final String query;
    private final AnnotationQuery annotationQuery;
    private final int location;
    private final String analysisMode;
    private final ArrayList<String> includedPaths;
    private final boolean includeOpenSourceLibraries;

    public RuleEntryParams(final String commandId, final List<Object> arguments) {
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) arguments.stream().findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        format("Command '%s' must be called with one rule entry argument!", commandId)));

        this.projectName = (String) obj.get("project");
        this.query = (String) obj.get("query");
        this.location = Integer.parseInt((String) obj.get("location"));
        this.annotationQuery = AnnotationQuery.fromMap(this.query, (Map<String, Object>) obj.get("annotationQuery"), location);
        this.analysisMode = (String) obj.get("analysisMode");
        this.includedPaths = (ArrayList<String>) obj.get("includedPaths");
        Boolean includeOpenSourceLibs = (Boolean) obj.get("includeOpenSourceLibraries");
        this.includeOpenSourceLibraries = (includeOpenSourceLibs != null) ? includeOpenSourceLibs : false;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getQuery() {
        return query;
    }

    public AnnotationQuery getAnnotationQuery() {
        return annotationQuery;
    }

    public int getLocation() {
        return location;
    }

    public String getAnalysisMode() {
        return analysisMode;
    }

    public ArrayList<String> getIncludedPaths() {
        return includedPaths;
    }

    public Boolean getIncludeOpenSourceLibraries() {
        return includeOpenSourceLibraries;
    }
}