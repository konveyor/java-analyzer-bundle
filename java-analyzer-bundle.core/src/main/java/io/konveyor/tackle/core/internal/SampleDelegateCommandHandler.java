package io.konveyor.tackle.core.internal;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.internal.core.DeltaProcessingState;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.JavaSearchParticipant;

public class SampleDelegateCommandHandler implements IDelegateCommandHandler {

    public static final String COMMAND_ID = "io.konveyor.tackle.samplecommand";
    public static final String RULE_ENTRY_COMMAND_ID = "io.konveyor.tackle.ruleEntry";

    @Override
    public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor progress) throws Exception {

        switch (commandId) {
            case COMMAND_ID:
                return "Hello World";
            case RULE_ENTRY_COMMAND_ID:
                logInfo("Here we get the arguments for rule entry: "+arguments);
                RuleEntryParams params = this.getRuleEntryParams(commandId, arguments);
                return search(params.projectName, params.query, params.location, progress);
            default:
                throw new UnsupportedOperationException(String.format("Unsupported command '%s'!", commandId));
        }
    }

    private RuleEntryParams getRuleEntryParams(String commandId, List<Object> arguments) {

        Map<String, Object> obj = ParamUtils.getFirst(arguments);

        if (obj == null) {
            throw new UnsupportedOperationException(String.format(
                "Command '%s' must be called with one rule entry argument!", commandId));
        }

        RuleEntryParams params = new RuleEntryParams();

        params.projectName = ParamUtils.getString(obj, "project");
        params.query = ParamUtils.getString(obj, "query");
        params.location = Integer.parseInt(ParamUtils.getString(obj, "location"));
        return params;
    }

    // mapLocationToSearchPatternLocation will create the correct search pattern or throw an error if one can not be built.
    // the search patterns are responsible for finding as many locations/symbols as possible. We will relay on the client
    // to filter.
    private static SearchPattern mapLocationToSearchPatternLocation(int location, String query) throws Exception {
        //TODO: Normalize queries and/or verify for each location.
        switch (location) {
        // Using type for both type and annotation.
        case 0:
        case 4:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_PATTERN_MATCH);
        case 1:
            throw new Exception("Inheritance is not implemented");
        case 2: 
            return SearchPattern.createPattern(query, IJavaSearchConstants.METHOD, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_PATTERN_MATCH);
        case 3:
            return SearchPattern.createPattern(query, IJavaSearchConstants.CONSTRUCTOR, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_PATTERN_MATCH);
        }
        throw new Exception("unable to create search pattern"); 
    }

    private static List<SymbolInformation> search(String projectName, String query, int location, IProgressMonitor monitor) throws Exception {
        JavaModelManager.getIndexManager().waitForIndex(true, monitor);
        DeltaProcessingState state = JavaModelManager.getDeltaState();
        logInfo("sourceAttachements: " + state.sourceAttachments);

        // TODO: Hopefully we can eventually find a way that will alert this thread, when sourceAttachments are downloaded.
        // https://github.com/konveyor/java-analyzer-bundle/issues/14
        Map<IPath, IPath> attachments = state.sourceAttachments;
        Thread.sleep(5000);
        while (attachments.size() != state.sourceAttachments.size()) {
            logInfo("waiting size: " + state.sourceAttachments.size());
            attachments = state.sourceAttachments;
            Thread.sleep(10000);
        }

        IJavaSearchScope scope = SearchEngine.createWorkspaceScope();

        SearchPattern pattern;
        try {
            pattern = mapLocationToSearchPatternLocation(location, query);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logInfo("Unable to get search pattern: " + e);
            throw e;
            
        }

        SearchEngine searchEngine = new SearchEngine();

        List<SymbolInformation> symbols = new ArrayList<SymbolInformation>();

        SymbolInformationTypeRequestor requestor = new SymbolInformationTypeRequestor(symbols, 0, monitor, location);

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

