package io.konveyor.tackle.core.internal;

import static java.lang.String.format;
import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.search.JavaSearchParticipant;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.JobHelpers;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.jdt.ls.core.internal.ResourceUtils;
import org.eclipse.lsp4j.SymbolInformation;

import io.konveyor.tackle.core.internal.query.AnnotationQuery;
import io.konveyor.tackle.core.internal.util.OpenSourceFilteredSearchScope;
import io.konveyor.tackle.core.internal.util.OpenSourceLibraryExclusionManager;

public class SampleDelegateCommandHandler implements IDelegateCommandHandler {

    public static final String COMMAND_ID = "io.konveyor.tackle.samplecommand";
    public static final String RULE_ENTRY_COMMAND_ID = "io.konveyor.tackle.ruleEntry";

    private static final String FullAnalysisMode = "full";
    private static final String sourceOnlyAnalysisMode = "source-only";

    @Override
    public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor progress) throws Exception {
        switch (commandId) {
            case COMMAND_ID:
                return "Hello World";
            case RULE_ENTRY_COMMAND_ID:
                logInfo("Here we get the arguments for rule entry: " + arguments);
                RuleEntryParams params = new RuleEntryParams(commandId, arguments);
                return search(params.getProjectName(), params.getIncludedPaths(), params.getQuery(),
                        params.getAnnotationQuery(), params.getLocation(), params.getAnalysisMode(),
                        params.getIncludeOpenSourceLibraries(), params.getMavenLocalRepoPath(),
                        params.getMavenIndexPath(), progress);
            default:
                throw new UnsupportedOperationException(format("Unsupported command '%s'!", commandId));
        }
    }

    private static void waitForJavaSourceDownloads() {
        JobHelpers.waitForInitializeJobs();
        JobHelpers.waitForBuildJobs(JobHelpers.MAX_TIME_MILLIS);
        JobHelpers.waitForDownloadSourcesJobs(JobHelpers.MAX_TIME_MILLIS);

    }

    // mapLocationToSearchPatternLocation will create the correct search pattern or throw an error if one can not be built.
    // the search patterns are responsible for finding as many locations/symbols as possible. We will relay on the client
    // to filter.
    private static SearchPattern mapLocationToSearchPatternLocation(int location, String query) throws Exception {
        //TODO: #21 Normalize queries and/or verify for each location.

        Pattern orPattern = Pattern.compile(".*\\(.*\\|.*\\).*");
        if (orPattern.matcher(query).matches()) {
            // We know that this is a list of things to loook for, broken by a | command. We should get this intra string and create an OR search pattern for each one. 
            // ex java.io.((FileWriter|FileReader|PrintStream|File|PrintWriter|RandomAccessFile))*
            // startQuery will contain java.io. and endQuery will contain *
            var startListIndex = query.indexOf("(");
            var endListIndex = query.indexOf(")");
            var startQuery = query.substring(0, startListIndex);
            var endQuery = query.substring(endListIndex+1, query.length());
            var optionalList = endQuery.startsWith("?");

            // This should strip the ( ) chars
            var listString = query.substring(startListIndex+1, endListIndex);
            var list = listString.split("\\|");
            ArrayList<SearchPattern> l = new ArrayList<SearchPattern>();

            if (optionalList) {
                // remove the ? from the endQueryString
                endQuery = endQuery.substring(1, endQuery.length());
                l.add(mapLocationToSearchPatternLocation(location, startQuery + endQuery));
            }

            for (String s: list) {
                var p = mapLocationToSearchPatternLocation(location, startQuery + s + endQuery);
                l.add(p);
            }
            
            // Get the end pattern
            SearchPattern p = l.subList(1, l.size()).stream().reduce(l.get(0), (SearchPattern::createOrPattern));
            return p;

        }

        if (location == 0) {
            logInfo("default query passed " + query + ", searching everything");
            ArrayList<SearchPattern> l = new ArrayList<SearchPattern>();
            // Searching for Type, Method, and Constructor's.
            var p = getPatternSingleQuery(10, query);
            if (p != null) {
                l.add(p);
            }
            p = getPatternSingleQuery(2, query);
            if (p != null) {
                l.add(p);
            }
            p = getPatternSingleQuery(3, query);
            if (p != null) {
                l.add(p);
            }
            logInfo("list of p: " + p);

            // Get the end pattern
            p = l.subList(1, l.size()).stream().reduce(l.get(0), (SearchPattern::createOrPattern));
            return p;
        }

        return getPatternSingleQuery(location, query);
    }

    /**
     * Location correspondence from java provider (TYPE is the default):
     * 	"":                 0,
     * 	"inheritance":      1,
     * 	"method_call":      2,
     * 	"constructor_call": 3,
     * 	"annotation":       4,
     * 	"implements_type":  5,
     * 	"enum_constant":        6,
     * 	"return_type":          7,
     * 	"import":               8,
     * 	"variable_declaration": 9,
     * 	"type":                 10,
     * 	"package":              11,
     * 	"field":                12,
     *  "method_declaration":   13,
     *  "class_declaration":    14,
     *
     * @param location
     * @param query
     * @return
     * @throws Exception
     */
    private static SearchPattern getPatternSingleQuery(int location, String query) throws Exception {
        var pattern = SearchPattern.R_PATTERN_MATCH;
        if ((!query.contains("?") || !query.contains("*")) && (location != 11)) {
            logInfo("Using full match");
            pattern = SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
        }
        switch (location) {
        // Using type for both type and annotation.
        // Type and annotation
        case 10:
        case 4:
        case 8:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.ALL_OCCURRENCES, pattern);
        case 5:
        case 1:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.IMPLEMENTORS, pattern);
        case 7:
        case 9:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.REFERENCES, pattern);
        case 2:
            if (query.contains(".")) {
                return SearchPattern.createPattern(query, IJavaSearchConstants.METHOD, IJavaSearchConstants.QUALIFIED_REFERENCE, SearchPattern.R_PATTERN_MATCH | SearchPattern.R_ERASURE_MATCH);
            }
            // Switched back to referenced
            return SearchPattern.createPattern(query, IJavaSearchConstants.METHOD, IJavaSearchConstants.REFERENCES, SearchPattern.R_PATTERN_MATCH | SearchPattern.R_ERASURE_MATCH);
        case 3:
            return SearchPattern.createPattern(query, IJavaSearchConstants.CONSTRUCTOR, IJavaSearchConstants.ALL_OCCURRENCES, pattern);
        case 11:
            return SearchPattern.createPattern(query, IJavaSearchConstants.PACKAGE, IJavaSearchConstants.ALL_OCCURRENCES, pattern);
        case 12:
            return SearchPattern.createPattern(query, IJavaSearchConstants.TYPE, IJavaSearchConstants.FIELD_DECLARATION_TYPE_REFERENCE, pattern);
        case 13:
            return SearchPattern.createPattern(query, IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_PATTERN_MATCH);
        case 14:
            return SearchPattern.createPattern(query, IJavaSearchConstants.CLASS, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_PATTERN_MATCH);
        }
        throw new Exception("unable to create search pattern"); 
    }

    private static List<SymbolInformation> search(String projectName, ArrayList<String> includedPaths, String query, AnnotationQuery annotationQuery, int location, String analysisMode,
        boolean includeOpenSourceLibraries, String mavenLocalRepoPath, String mavenIndexPath, IProgressMonitor monitor) throws Exception {
        IJavaProject[] targetProjects;
        IJavaProject project = ProjectUtils.getJavaProject(projectName);
        if (project != null) {
			targetProjects = new IJavaProject[] { project };
		} else {
			targetProjects= ProjectUtils.getJavaProjects();
		}
    
        logInfo("Searching in target project: " + targetProjects);

        //  For Partial results, we are going to filter out based on a list in the engine
		int s = IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS | IJavaSearchScope.APPLICATION_LIBRARIES | IJavaSearchScope.SYSTEM_LIBRARIES;
        if (analysisMode.equals(sourceOnlyAnalysisMode)) {
            logInfo("KONVEYOR_LOG: source-only analysis mode only scoping to Sources");
            s = IJavaSearchScope.SOURCES;
        } else {
            logInfo("KONVEYOR_LOG: waiting for source downloads");
            waitForJavaSourceDownloads();
            logInfo("KONVEYOR_LOG: waited for source downloads");
        }

        for (IJavaProject iJavaProject : targetProjects) {
            var errors = ResourceUtils.getErrorMarkers(iJavaProject.getProject());
            var warnings = ResourceUtils.getWarningMarkers(iJavaProject.getProject());
            logInfo("KONVEYOR_LOG:" +
                " found errors: " + errors.toString().replace("\n", " ") +
                " warnings: " + warnings.toString().replace("\n", " "));
        }

		IJavaSearchScope scope;
        var workspaceDirectoryLocation = JavaLanguageServerPlugin.getPreferencesManager().getPreferences().getRootPaths();
        if (workspaceDirectoryLocation == null || workspaceDirectoryLocation.size() == 0) {
            logInfo("unable to find workspace directory location");
            return new ArrayList<>();
        }

        if (includedPaths != null && includedPaths.size() > 0) {
            ArrayList<IJavaElement> includedFragments = new ArrayList<IJavaElement>();
            for (IJavaProject proj : targetProjects) {
                for (String includedPath : includedPaths) {
                    IPath includedIPath = Path.fromOSString(includedPath);
                    if (includedIPath.isAbsolute()) {
                        includedIPath = includedIPath.makeRelativeTo(workspaceDirectoryLocation.iterator().next());
                        // when the java project is in a sub-directory, we need to cut everything
                        // until the first occurrence of "src".
                        if (!includedIPath.segment(0).equals("src")) {
                            int srcSegmentIdx = -1;
                            for (int i = 0; i < includedIPath.segmentCount(); i += 1) {
                                if (includedIPath.segment(i).equals("src")) {
                                    srcSegmentIdx = i;
                                }
                            }
                            if (srcSegmentIdx != -1) {
                                includedIPath = includedIPath.removeFirstSegments(srcSegmentIdx);
                            }
                        }
                        // we need to remove the /src/main/java from the path
                        if (includedIPath.segment(0).equals("src")) {
                            includedIPath = includedIPath.removeFirstSegments(1);
                        }
                        if (includedIPath.segment(0).equals("main")) {
                            includedIPath = includedIPath.removeFirstSegments(1);
                        }
                        if (includedIPath.segment(0).equals("java")) {
                            includedIPath = includedIPath.removeFirstSegments(1);
                        }
                        var element = proj.findElement(includedIPath);
                        if (element == null) {
                            element = proj.findElement(includedIPath.removeLastSegments(1));
                            continue;
                        }
                        if (element instanceof ICompilationUnit) {
                            var x = element.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
                            if (x != null) {
                                includedFragments.add(x);
                            }
                        } else {
                            includedFragments.add(element);
                        }
                        continue;
                    }
                    for (IPackageFragment fragment : proj.getPackageFragments()) {
                        IPath fragmentPath = fragment.getPath();
                        // if there's no file extension, it's a path to java package from source
                        // else it is a path pointing to jar, ear, etc. we ignore deps for now
                        if (fragmentPath.getFileExtension() != null) {
                            continue;
                        }
                        // fragment paths are not actual filesystem paths
                        // they are of form /<artifact>/src/main/java
                        // we can only compare the relative path
                        fragmentPath = fragmentPath.removeFirstSegments(1);
                        // when there are more than one sub-projects, the paths are of form
                        // <project-name>/src/main/java/
                        if (includedPath.startsWith(proj.getElementName())) {
                            includedIPath = includedIPath.removeFirstSegments(1);
                        }
                        // instead of comparing path strings, comparing segments is better for 2 reasons:
                        // - we don't have to worry about redundant . / etc in input
                        // - matching sub-trees is easier with segments than strings
                        if (includedIPath.segmentCount() <= fragmentPath.segmentCount() && 
                            includedIPath.matchingFirstSegments(fragmentPath) == includedIPath.segmentCount()) {
                            includedFragments.add(fragment);
                        }
                    }
                }
            }
            IJavaElement[] includedElements = new IJavaElement[includedFragments.size()];
            includedElements = includedFragments.toArray(includedElements);
            scope = SearchEngine.createJavaSearchScope(true, includedElements, s);
        } else {
            scope = SearchEngine.createJavaSearchScope(true, targetProjects, s);
        }

        // Use a filtered scope when open source libraries are not included
        if (!includeOpenSourceLibraries) {
            scope = new OpenSourceFilteredSearchScope(scope,
                    OpenSourceLibraryExclusionManager.getInstance(mavenLocalRepoPath, mavenIndexPath));
        }
        logInfo("scope: " + scope);

        SearchPattern pattern;
        try {
            pattern = mapLocationToSearchPatternLocation(location, query);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logInfo("KONVEYOR_LOG: Unable to get search pattern: " + e.toString().replace("\n", " "));
            throw e;
        }
        logInfo("KONVEYOR_LOG: pattern: " + pattern.toString().replace("\n", " "));

        SearchEngine searchEngine = new SearchEngine();

        List<SymbolInformation> symbols = new ArrayList<SymbolInformation>();

        SymbolInformationTypeRequestor requestor = new SymbolInformationTypeRequestor(symbols, 0, monitor, location, query, annotationQuery);

        //Use the default search participents
        SearchParticipant participent = new JavaSearchParticipant();
        SearchParticipant[] participents = new SearchParticipant[]{participent};

        try {
            searchEngine.search(pattern, participents, scope, requestor, monitor);
        } catch (Exception e) {
            // TODO: handle exception
            logInfo("KONVEYOR_LOG: unable to get search " + e.toString().replace("\n", " "));
        }

        logInfo("KONVEYOR_LOG: got: " + requestor.getAllSearchMatches() +
            " search matches for " + query +
            " location " + location
            + " matches" + requestor.getSymbols().size());

        return requestor.getSymbols();

    }
}
