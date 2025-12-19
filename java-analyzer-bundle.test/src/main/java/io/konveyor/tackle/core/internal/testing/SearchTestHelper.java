package io.konveyor.tackle.core.internal.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.lsp4j.SymbolInformation;

import io.konveyor.tackle.core.internal.SampleDelegateCommandHandler;
import io.konveyor.tackle.core.internal.query.AnnotationQuery;

/**
 * Helper class for executing searches and handling results in integration tests.
 * Provides convenient wrappers around the RuleEntry command with filtering and
 * debugging utilities.
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Simple search
 * List<SymbolInformation> results = SearchTestHelper.executeSearch(
 *     "test-project", "println", LocationType.METHOD_CALL, "source-only");
 * 
 * // Filter and assert
 * List<SymbolInformation> filtered = SearchTestHelper.filterByFile(
 *     results, "SampleApplication.java");
 * assertFalse(filtered.isEmpty());
 * }</pre>
 */
@SuppressWarnings("restriction")
public class SearchTestHelper {
    public static final int LOCATION_DEFAULT = 0;
    public static final int LOCATION_INHERITANCE = 1;
    public static final int LOCATION_METHOD_CALL = 2;
    public static final int LOCATION_CONSTRUCTOR_CALL = 3;
    public static final int LOCATION_ANNOTATION = 4;
    public static final int LOCATION_IMPLEMENTS_TYPE = 5;
    public static final int LOCATION_ENUM_CONSTANT = 6;
    public static final int LOCATION_RETURN_TYPE = 7;
    public static final int LOCATION_IMPORT = 8;
    public static final int LOCATION_VARIABLE_DECLARATION = 9;
    public static final int LOCATION_TYPE = 10;
    public static final int LOCATION_PACKAGE = 11;
    public static final int LOCATION_FIELD = 12;
    public static final int LOCATION_METHOD_DECLARATION = 13;
    public static final int LOCATION_CLASS_DECLARATION = 14;

    private static final SampleDelegateCommandHandler commandHandler = new SampleDelegateCommandHandler();


    /**
     * Executes a search using the RuleEntry command.
     * 
     * @param projectName Name of the project to search in
     * @param query       Search query pattern
     * @param location    Location type (use LOCATION_* constants)
     * @param analysisMode "source-only" or "full"
     * @return List of matching symbols
     */
    public static List<SymbolInformation> executeSearch(
            String projectName,
            String query,
            int location,
            String analysisMode) {
        return executeSearch(projectName, query, location, analysisMode, null, null);
    }

    /**
     * Executes a search with included paths filter.
     * 
     * @param projectName   Name of the project to search in
     * @param query         Search query pattern
     * @param location      Location type
     * @param analysisMode  "source-only" or "full"
     * @param includedPaths List of paths to include (null for all)
     * @return List of matching symbols
     */
    public static List<SymbolInformation> executeSearch(
            String projectName,
            String query,
            int location,
            String analysisMode,
            List<String> includedPaths) {
        return executeSearch(projectName, query, location, analysisMode, includedPaths, null);
    }

    /**
     * Executes a search with all options including annotation query.
     * 
     * @param projectName     Name of the project to search in
     * @param query           Search query pattern
     * @param location        Location type
     * @param analysisMode    "source-only" or "full"
     * @param includedPaths   List of paths to include (null for all)
     * @param annotationQuery Annotation query for filtering (null for none)
     * @return List of matching symbols
     */
    @SuppressWarnings("unchecked")
    public static List<SymbolInformation> executeSearch(
            String projectName,
            String query,
            int location,
            String analysisMode,
            List<String> includedPaths,
            AnnotationQuery annotationQuery) {
        
        logInfo("Executing search: project=" + projectName + 
                ", query=" + query + 
                ", location=" + location +
                ", mode=" + analysisMode);
        
        try {
            // Build parameters map matching RuleEntryParams expectations
            Map<String, Object> params = new HashMap<>();
            params.put("project", projectName);
            params.put("query", query);
            params.put("location", String.valueOf(location));
            params.put("analysisMode", analysisMode);
            
            if (includedPaths != null) {
                params.put("includedPaths", new ArrayList<>(includedPaths));
            }
            
            if (annotationQuery != null) {
                Map<String, Object> annotationQueryMap = new HashMap<>();
                annotationQueryMap.put("pattern", annotationQuery.getType());
                
                List<Map<String, String>> elements = new ArrayList<>();
                for (Map.Entry<String, String> entry : annotationQuery.getElements().entrySet()) {
                    Map<String, String> element = new HashMap<>();
                    element.put("name", entry.getKey());
                    element.put("value", entry.getValue());
                    elements.add(element);
                }
                annotationQueryMap.put("elements", elements);
                
                params.put("annotationQuery", annotationQueryMap);
            }
            
            // Execute command
            List<Object> arguments = new ArrayList<>();
            arguments.add(params);
            
            Object result = commandHandler.executeCommand(
                SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID,
                arguments,
                null // IProgressMonitor
            );
            
            List<SymbolInformation> symbols = (List<SymbolInformation>) result;
            logInfo("Search returned " + symbols.size() + " results");
            
            return symbols;
            
        } catch (Exception e) {
            logInfo("Search failed: " + e.getMessage());
            throw new RuntimeException("Search execution failed", e);
        }
    }


    /**
     * Filters results to those in a specific file.
     * 
     * @param symbols          List of symbols to filter
     * @param fileNameContains Substring that the file URI should contain
     * @return Filtered list
     */
    public static List<SymbolInformation> filterByFile(
            List<SymbolInformation> symbols,
            String fileNameContains) {
        return symbols.stream()
                .filter(s -> s.getLocation() != null && 
                            s.getLocation().getUri() != null &&
                            s.getLocation().getUri().contains(fileNameContains))
                .collect(Collectors.toList());
    }

    /**
     * Filters results by symbol name.
     * 
     * @param symbols List of symbols to filter
     * @param name    Symbol name to match
     * @return Filtered list
     */
    public static List<SymbolInformation> filterByName(
            List<SymbolInformation> symbols,
            String name) {
        return symbols.stream()
                .filter(s -> name.equals(s.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Filters results by symbol name pattern.
     * 
     * @param symbols     List of symbols to filter
     * @param namePattern Regex pattern to match against symbol name
     * @return Filtered list
     */
    public static List<SymbolInformation> filterByNamePattern(
            List<SymbolInformation> symbols,
            String namePattern) {
        return symbols.stream()
                .filter(s -> s.getName() != null && s.getName().matches(namePattern))
                .collect(Collectors.toList());
    }



    /**
     * Pretty-prints search results for debugging.
     * 
     * @param symbols List of symbols to print
     */
    public static void printResults(List<SymbolInformation> symbols) {
        System.out.println("\n========== Search Results (" + symbols.size() + " total) ==========");
        for (int i = 0; i < symbols.size(); i++) {
            SymbolInformation s = symbols.get(i);
            System.out.println(String.format("[%d] %s", i + 1, formatSymbol(s)));
        }
        System.out.println("=".repeat(50) + "\n");
    }

    /**
     * Formats a single symbol for display.
     * 
     * @param symbol The symbol to format
     * @return Formatted string
     */
    public static String formatSymbol(SymbolInformation symbol) {
        StringBuilder sb = new StringBuilder();
        sb.append(symbol.getName());
        sb.append(" (").append(symbol.getKind()).append(")");
        
        if (symbol.getContainerName() != null) {
            sb.append(" in ").append(symbol.getContainerName());
        }
        
        if (symbol.getLocation() != null) {
            String uri = symbol.getLocation().getUri();
            // Extract just the filename
            int lastSlash = uri.lastIndexOf('/');
            String fileName = lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
            sb.append(" @ ").append(fileName);
            
            if (symbol.getLocation().getRange() != null) {
                int line = symbol.getLocation().getRange().getStart().getLine() + 1;
                sb.append(":").append(line);
            }
        }
        
        return sb.toString();
    }

    /**
     * Returns a summary string of results.
     * 
     * @param symbols List of symbols
     * @return Summary string
     */
    public static String summarizeResults(List<SymbolInformation> symbols) {
        if (symbols.isEmpty()) {
            return "No results";
        }
        
        // Group by container
        Map<String, Long> byContainer = symbols.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getContainerName() != null ? s.getContainerName() : "<unknown>",
                        Collectors.counting()));
        
        StringBuilder sb = new StringBuilder();
        sb.append(symbols.size()).append(" total results: ");
        sb.append(byContainer.entrySet().stream()
                .map(e -> e.getValue() + " in " + e.getKey())
                .collect(Collectors.joining(", ")));
        
        return sb.toString();
    }


    /**
     * Checks if any result is in the specified file.
     * 
     * @param symbols          List of symbols
     * @param fileNameContains Substring to look for in file URI
     * @return true if at least one match is found
     */
    public static boolean hasResultInFile(
            List<SymbolInformation> symbols,
            String fileNameContains) {
        return !filterByFile(symbols, fileNameContains).isEmpty();
    }

    /**
     * Checks if any result has the specified name.
     * 
     * @param symbols List of symbols
     * @param name    Symbol name to look for
     * @return true if at least one match is found
     */
    public static boolean hasResultWithName(
            List<SymbolInformation> symbols,
            String name) {
        return !filterByName(symbols, name).isEmpty();
    }

    private static void logInfo(String message) {
        System.out.println("[SearchTestHelper] " + message);
        try {
            JavaLanguageServerPlugin.logInfo("[SearchTestHelper] " + message);
        } catch (Exception e) {
        }
    }
}
