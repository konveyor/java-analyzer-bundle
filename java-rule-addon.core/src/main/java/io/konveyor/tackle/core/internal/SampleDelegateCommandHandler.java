package io.konveyor.tackle.core.internal;


import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.jdt.core.search.SearchPattern;

public class SampleDelegateCommandHandler implements IDelegateCommandHandler {

    public static final String COMMAND_ID = "io.konveyor.tackle.samplecommand";
    public static final String RULE_ENTRY_COMMAND_ID = "io.konveyor.tackle.ruleEntry";

    @Override
    public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor progress) throws Exception {

        switch (commandId) {
            case COMMAND_ID:
                return "Hello World";
            case RULE_ENTRY_COMMAND_ID:
                RuleEntryParams params = this.getRuleEntryParams(commandId, arguments);
                return search(params.projectName, params.query, progress);
            default:
                throw new UnsupportedOperationException(String.format("Unsupported command '%s'!", commandId));
        }
    }

    private RuleEntryParams getRuleEntryParams(String commandId, List<Object> arguments) {

        Map<String, Object> obj = ParamUtils.getFirst(arguments);

        if (obj == null) {
            throw new UnsupportedOperationException(String.format(
                "Command '%s' must be called with one MicroProfileJavaFileInfoParams argument!", commandId));
        }

        RuleEntryParams params = new RuleEntryParams();

        params.projectName = ParamUtils.getString(obj, "project");
        params.query = ParamUtils.getString(obj, "query");
        return params;
    }

    private static List<SymbolInformation> search(String projectName, String query, IProgressMonitor monitor) {
        IJavaProject[] targetProjects;
        IJavaProject project = ProjectUtils.getJavaProject(projectName);
        if (project != null) {
			targetProjects = new IJavaProject[] { project };
		} else {
			targetProjects= ProjectUtils.getJavaProjects();
		}

		int s = IJavaSearchScope.REFERENCED_PROJECTS | IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES | IJavaSearchScope.SYSTEM_LIBRARIES;

		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(targetProjects, s);

        SearchEngine searchEngine = new SearchEngine();

        SearchPattern pattern = SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_PATTERN_MATCH);

        List<SymbolInformation> symbols = new ArrayList<SymbolInformation>();

        SymbolInformationTypeRequestor requestor = new SymbolInformationTypeRequestor(symbols, 0, monitor);

        try {
            searchEngine.search(pattern, null, scope, requestor, monitor);
        } catch (Exception e) {
            //TODO: handle exception
            return Collections.emptyList();
        }
        return symbols;

    }
}

