package io.konveyor.tackle.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class RuleEntryParamsTest {

	@Test
	public void testBasicParameterParsing() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "com.example.*");
		params.put("location", "10");
		params.put("analysisMode", "full");
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertEquals("test-project", ruleParams.getProjectName());
		assertEquals("com.example.*", ruleParams.getQuery());
		assertEquals(10, ruleParams.getLocation());
		assertEquals("full", ruleParams.getAnalysisMode());
	}

	@Test
	public void testSourceOnlyAnalysisMode() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "javax.servlet.*");
		params.put("location", "2");
		params.put("analysisMode", "source-only");
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertEquals("source-only", ruleParams.getAnalysisMode());
		assertEquals(2, ruleParams.getLocation());
	}

	@Test
	public void testIncludedPaths() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		params.put("location", "10");
		params.put("analysisMode", "full");

		ArrayList<String> includedPaths = new ArrayList<>();
		includedPaths.add("/src/main/java");
		includedPaths.add("/src/test/java");
		params.put("includedPaths", includedPaths);
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertNotNull(ruleParams.getIncludedPaths());
		assertEquals(2, ruleParams.getIncludedPaths().size());
		assertTrue(ruleParams.getIncludedPaths().contains("/src/main/java"));
		assertTrue(ruleParams.getIncludedPaths().contains("/src/test/java"));
	}

	@Test
	public void testOpenSourceLibrariesTrue() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		params.put("location", "10");
		params.put("analysisMode", "full");
		params.put("includeOpenSourceLibraries", true);
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertTrue(ruleParams.getIncludeOpenSourceLibraries());
	}

	@Test
	public void testOpenSourceLibrariesFalse() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		params.put("location", "10");
		params.put("analysisMode", "full");
		params.put("includeOpenSourceLibraries", false);
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertFalse(ruleParams.getIncludeOpenSourceLibraries());
	}

	@Test
	public void testOpenSourceLibrariesDefaultsToFalse() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		params.put("location", "10");
		params.put("analysisMode", "full");
		// Not setting includeOpenSourceLibraries
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertFalse(ruleParams.getIncludeOpenSourceLibraries());
	}

	@Test
	public void testMavenLocalRepoPath() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		params.put("location", "10");
		params.put("analysisMode", "full");
		params.put("mavenLocalRepo", "/home/user/.m2/repository");
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertEquals("/home/user/.m2/repository", ruleParams.getMavenLocalRepoPath());
	}

	@Test
	public void testMavenIndexPath() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		params.put("location", "10");
		params.put("analysisMode", "full");
		params.put("mavenIndexPath", "/tmp/maven-index");
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertEquals("/tmp/maven-index", ruleParams.getMavenIndexPath());
	}

	@Test
	public void testAllLocations() {
		// Test all valid location types
		int[] locations = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};

		for (int location : locations) {
			List<Object> arguments = new ArrayList<>();
			Map<String, Object> params = new HashMap<>();
			params.put("project", "test-project");
			params.put("query", "test.query");
			params.put("location", String.valueOf(location));
			params.put("analysisMode", "full");
			arguments.add(params);

			RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);
			assertEquals(location, ruleParams.getLocation());
		}
	}

	@Test
	public void testAnnotationQueryWithElements() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "javax.ejb.Stateless");
		params.put("location", "4");
		params.put("analysisMode", "full");

		Map<String, Object> annotationQuery = new HashMap<>();
		annotationQuery.put("pattern", "javax.ejb.Stateless");
		List<Map<String, String>> elements = new ArrayList<>();
		Map<String, String> element = new HashMap<>();
		element.put("name", "value");
		element.put("value", "TestBean");
		elements.add(element);
		annotationQuery.put("elements", elements);

		params.put("annotationQuery", annotationQuery);
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertNotNull(ruleParams.getAnnotationQuery());
		assertEquals("javax.ejb.Stateless", ruleParams.getAnnotationQuery().getType());
	}

	@Test
	public void testNullAnnotationQuery() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		params.put("location", "10");
		params.put("analysisMode", "full");
		// Not setting annotationQuery
		arguments.add(params);

		RuleEntryParams ruleParams = new RuleEntryParams("test-command", arguments);

		assertNull(ruleParams.getAnnotationQuery());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testMissingArguments() {
		List<Object> arguments = new ArrayList<>();
		// Empty arguments list should throw exception
		new RuleEntryParams("test-command", arguments);
	}

	@Test(expected = NumberFormatException.class)
	public void testInvalidLocationFormat() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		params.put("location", "invalid");
		params.put("analysisMode", "full");
		arguments.add(params);

		new RuleEntryParams("test-command", arguments);
	}

	@Test(expected = NumberFormatException.class)
	public void testMissingLocation() {
		List<Object> arguments = new ArrayList<>();
		Map<String, Object> params = new HashMap<>();
		params.put("project", "test-project");
		params.put("query", "test.query");
		// Missing location
		params.put("analysisMode", "full");
		arguments.add(params);

		new RuleEntryParams("test-command", arguments);
	}
}
