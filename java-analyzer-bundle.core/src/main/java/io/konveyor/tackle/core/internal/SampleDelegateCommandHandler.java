package io.konveyor.tackle.core.internal;

import static java.lang.String.format;
import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.core.search.JavaSearchParticipant;
import org.eclipse.jdt.ls.core.internal.IDelegateCommandHandler;
import org.eclipse.jdt.ls.core.internal.JobHelpers;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.m2e.jdt.internal.BuildPathManager;

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
                logInfo("Here we get the arguments for rule entry: "+arguments);
                RuleEntryParams params = new RuleEntryParams(commandId, arguments);
                return search(params.getProjectName(), params.getQuery(), params.getLocation(), params.getAnalysisMode(), progress);
            default:
                throw new UnsupportedOperationException(format("Unsupported command '%s'!", commandId));
        }
    }

    private static void waitForJavaSourceDownloads() {
        JobHelpers.waitForInitializeJobs();
        JobHelpers.waitForBuildJobs(JobHelpers.MAX_TIME_MILLIS);
        JobHelpers.waitForDownloadSourcesJobs(JobHelpers.MAX_TIME_MILLIS);
        JobHelpers.waitForJobsToComplete();
    }

    // mapLocationToSearchPatternLocation will create the correct search pattern or throw an error if one can not be built.
    // the search patterns are responsible for finding as many locations/symbols as possible. We will relay on the client
    // to filter.
    private static SearchPattern mapLocationToSearchPatternLocation(int location, String query) throws Exception {
        //TODO: #21 Normalize queries and/or verify for each location.

        if (query.contains("(") || query.contains(")")) {
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
            logInfo("default query passed, searching everything");
            ArrayList<SearchPattern> l = new ArrayList<SearchPattern>();
            // Searching for Type, Method, and Constructor's.
            l.add(getPatternSingleQuery(10, query));
            l.add(getPatternSingleQuery(2, query));
            l.add(getPatternSingleQuery(3, query));
            
            // Get the end pattern
            SearchPattern p = l.subList(1, l.size()).stream().reduce(l.get(0), (SearchPattern::createOrPattern));
            return p;
        }

        return getPatternSingleQuery(location, query);
    }

    private static SearchPattern getPatternSingleQuery(int location, String query) throws Exception {
        switch (location) {
        // Using type for both type and annotation.
        // Type and annotation
        case 10:
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
        case 11:
            return SearchPattern.createPattern(query, IJavaSearchConstants.PACKAGE, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_PATTERN_MATCH);
        }
        throw new Exception("unable to create search pattern"); 
    }

    private static List<SymbolInformation> search(String projectName, String query, int location, String analsysisMode, IProgressMonitor monitor) throws Exception {
        //IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
         
        IJavaProject[] targetProjects;
        IJavaProject project = ProjectUtils.getJavaProject(projectName);
        logInfo("Searching in project: " + project + " Query: " + query);
        if (project != null) {
			targetProjects = new IJavaProject[] { project };
		} else {
			targetProjects= ProjectUtils.getJavaProjects();
		}
        logInfo("Searching in target project: " + targetProjects);

        //  For Partial results, we are going to filter out based on a list in the engine
		int s = IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS | IJavaSearchScope.APPLICATION_LIBRARIES;
        if (analsysisMode.equals(sourceOnlyAnalysisMode)) {
            logInfo("source-only analysis mode only scoping to Sources");
            s = IJavaSearchScope.SOURCES;
        } else {
            logInfo("waiting for source downloads");
            waitForJavaSourceDownloads();
            logInfo("waited for source downloads");
            for (var p: targetProjects) {
                var container = p.getResolvedClasspath(false);
                IClasspathEntry[] newClassPaths = new IClasspathEntry[container.length];
                for (var i = 0; i < container.length; i++) {
                    var c = container[i];
                    logInfo("checking if we need to get source: " + c.getSourceAttachmentPath() + ": for path " + c.getPath());
                    String fileName = c.getPath().lastSegment();
                    fileName = fileName.replace(".jar", "-sources.jar");
                    var outputPath = new Path(c.getPath().toFile().getParent().toString() + "/" + fileName);
                    
                    if (c.getSourceAttachmentPath() == null &&
                     c.getPath().toFile().getAbsoluteFile().toString().startsWith(JavaCore.getClasspathVariable(BuildPathManager.M2_REPO).toString()) &&
                     !outputPath.toFile().exists()) {
                        File theDir = new File(c.getPath().toFile().getParent().toString() + "/source");
                        if (!theDir.exists()){
                            theDir.mkdirs();
                        }

                        ProcessBuilder builder = new ProcessBuilder();
                        builder.command("java", "-jar", "/bin/fernflower.jar", c.getPath().toFile().getAbsolutePath(), theDir.getAbsolutePath());
                        Process process = builder.start();
                        process.waitFor(50, TimeUnit.SECONDS);
                        if (process.exitValue() != 0) {
                            logInfo("unable to decompile: " + c.getPath());
                        }
                        var newPath = new Path(theDir.getAbsolutePath()+"/"+c.getPath().lastSegment());
                        Files.move(newPath.toFile().toPath(), outputPath.toFile().toPath());
                   } else {
                    newClassPaths[i] = c;
                   }
                }
                p.getJavaModel().refreshExternalArchives(new IJavaElement[]{p}, monitor);
            }
            waitForJavaSourceDownloads();
        }

		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(targetProjects, s);

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

