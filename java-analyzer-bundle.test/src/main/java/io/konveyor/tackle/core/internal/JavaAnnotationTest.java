package io.konveyor.tackle.core.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.junit.Assert;
import org.junit.Test;

public class JavaAnnotationTest extends ProjectUtilsTest {

    @Test
    public void shouldMatchSpringResponseBodyAnnotationTest() throws Exception {
        IJavaProject javaProject = loadMavenProject(MavenProjectName.springboot_todo_project);
        System.out.println("=== Java Project name : " + javaProject.getProject().getName());

        // String aQuery = "org.springframework.web.bind.annotation.ResponseBody"; //
        // !! We got 2 SymbolInformation: ResponseBody and PostMapping - https://github.com/konveyor/java-analyzer-bundle/issues/175

        String aQuery = "ResponseBody";

        // Query to search an annotation
        Map<String, Object> mapArgs = Map.of(
                "project", javaProject.getProject().getName(),
                "location", LOCATION_TYPE_ANNOTATION,
                "query", aQuery,
                "analysisMode", ANALYSIS_MODE_SOURCE_ONLY);

        RuleEntryParams params = new RuleEntryParams(RULE_ENTRY_COMMAND_ID, List.of(mapArgs));
        Assert.assertNotNull(params);

        SampleDelegateCommandHandler sdch = new SampleDelegateCommandHandler();
        List<SymbolInformation> results = sdch.search(params.getProjectName(),
                params.getIncludedPaths(), params.getQuery(),
                params.getAnnotationQuery(), params.getLocation(), params.getAnalysisMode(),
                params.getIncludeOpenSourceLibraries(), params.getMavenLocalRepoPath(),
                params.getMavenIndexPath(), new NullProgressMonitor());
        Assert.assertNotNull(results);

        // Search within the results the symbol matching thge annotation to search
        String targetAnnotation = "ResponseBody";
        Optional<SymbolInformation> foundSymbol = results.stream()
                .filter(symbol -> symbol.getName().equals(targetAnnotation))
                .findFirst();

        Assert.assertNotNull(foundSymbol.get());

        Location loc = foundSymbol.get().getLocation();
        Assert.assertNotNull(loc);

        // The annotation org.springframework.web.bind.annotation.ResponseBody is
        // included
        // within the file com.todo.app.controller.TaskController.java
        Assert.assertEquals(true, loc.getUri().contains("TaskController.java"));

        // Verify the location where the annotation has been declared using the Range
        Range range = loc.getRange();
        Assert.assertNotNull(range);

        Position posStart = range.getStart();
        Position posEnd = range.getEnd();
        Assert.assertEquals(76, posStart.getLine());
        Assert.assertEquals(5, posStart.getCharacter());

        Assert.assertEquals(76, posEnd.getLine());
        Assert.assertEquals(17, posEnd.getCharacter());
    }
}