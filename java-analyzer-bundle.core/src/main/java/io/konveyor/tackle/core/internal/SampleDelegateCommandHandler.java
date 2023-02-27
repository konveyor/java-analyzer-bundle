package io.konveyor.tackle.core.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.search.JavaSearchParticipant;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;
import org.eclipse.jdt.ls.core.internal.JobHelpers;
import org.eclipse.lsp4j.SymbolInformation;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

public class SampleDelegateCommandHandler implements IDelegateCommandHandler {

    public static final String COMMAND_ID = "io.konveyor.tackle.samplecommand";
    public static final String RULE_ENTRY_COMMAND_ID = "io.konveyor.tackle.ruleEntry";

    @Override
    public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor progress) throws Exception {
        logInfo("waiting for source downloads");
        waitForJavaSourceDownloads();
        logInfo("waited for source downloads");
        switch (commandId) {
            case COMMAND_ID:
                return "Hello World";
            case RULE_ENTRY_COMMAND_ID:
                logInfo("Here we get the arguments for rule entry: "+arguments);
                RuleEntryParams params = new RuleEntryParams(commandId, arguments);
                return search(params.getProjectName(), params.getQuery(), params.getLocation(), progress);
            default:
                throw new UnsupportedOperationException(format("Unsupported command '%s'!", commandId));
        }
    }

    private void waitForJavaSourceDownloads() {
        JobHelpers.waitForInitializeJobs();
        JobHelpers.waitForBuildJobs(JobHelpers.MAX_TIME_MILLIS);
        JobHelpers.waitForDownloadSourcesJobs(JobHelpers.MAX_TIME_MILLIS);
    }

    // mapLocationToSearchPatternLocation will create the correct search pattern or throw an error if one can not be built.
    // the search patterns are responsible for finding as many locations/symbols as possible. We will relay on the client
    // to filter.
    private static SearchPattern mapLocationToSearchPatternLocation(int location, String query) throws Exception {
        //TODO: #21 Normalize queries and/or verify for each location.
        switch (location) {
        // Using type for both type and annotation.
        case 0:
        case 4:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_PATTERN_MATCH);
        case 5:
        case 1:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.IMPLEMENTORS, SearchPattern.R_PATTERN_MATCH);
        case 2: 
            // Switched back to referenced
            return SearchPattern.createPattern(query, IJavaSearchConstants.METHOD, IJavaSearchConstants.REFERENCES, SearchPattern.R_PATTERN_MATCH | SearchPattern.R_ERASURE_MATCH);
        case 3:
            return SearchPattern.createPattern(query, IJavaSearchConstants.CONSTRUCTOR, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_PATTERN_MATCH);
        case 7:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.REFERENCES, SearchPattern.R_PATTERN_MATCH);
        case 8:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_PATTERN_MATCH);
        case 9:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.REFERENCES, SearchPattern.R_PATTERN_MATCH);
        }
        throw new Exception("unable to create search pattern"); 
    }

    private static List<SymbolInformation> search(String projectName, String query, int location, IProgressMonitor monitor) throws Exception {
        IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
        logInfo("scope: " + scope);

        SearchPattern pattern;
        try {
            pattern = mapLocationToSearchPatternLocation(location, query);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logInfo("Unable to get search pattern: " + e);
            throw e;
        }
        logInfo("pattern: " + pattern);

        SearchEngine searchEngine = new SearchEngine();

        List<SymbolInformation> symbols = new ArrayList<SymbolInformation>();

        SymbolInformationTypeRequestor requestor = new SymbolInformationTypeRequestor(symbols, 0, monitor, location, query);

        //Use the default search participents
        SearchParticipant participent = new JavaSearchParticipant();
        SearchParticipant[] participents = new SearchParticipant[]{participent};

        try {
            searchEngine.search(pattern, participents, scope, requestor, monitor);
        } catch (Exception e) {
            //TODO: handle exception
            logInfo("unable to get search " + e);
        }

        return requestor.getSymbols();

    }
}

