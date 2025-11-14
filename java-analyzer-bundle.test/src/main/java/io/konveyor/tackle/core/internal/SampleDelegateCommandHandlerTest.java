package io.konveyor.tackle.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/**
* Integration test for SampleDelegateCommandHandler. In Eclipse, right-click > Run As > JUnit-Plugin. <br/>
* In Maven CLI, run "mvn integration-test".
*/
public class SampleDelegateCommandHandlerTest {

	private SampleDelegateCommandHandler commandHandler;

	@Before
	public void setUp() {
		commandHandler = new SampleDelegateCommandHandler();
	}

	@Test
	public void testSampleCommand() throws Exception {
		assertEquals("Hello World", commandHandler.executeCommand(SampleDelegateCommandHandler.COMMAND_ID, null, null));
	}

	@Test
	public void testRuleEntryWithSourceOnlyMode() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "project",
			"query", "customresourcedefinition",
			"location", "10",
			"analysisMode", "source-only"
		);
		params.add(param);

		assertEquals(Collections.emptyList(), commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null));
	}

	@Test
	public void testRuleEntryWithFullMode() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "java.io.*",
			"location", "10",
			"analysisMode", "full"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithMethodCallLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "*.println",
			"location", "2",  // method_call
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithConstructorCallLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "java.util.ArrayList",
			"location", "3",  // constructor_call
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithAnnotationLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		param.put("project", "test-project");
		param.put("query", "javax.ejb.Stateless");
		param.put("location", "4");  // annotation
		param.put("analysisMode", "source-only");
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithInheritanceLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "java.lang.Exception",
			"location", "1",  // inheritance
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithImplementsTypeLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "java.io.Serializable",
			"location", "5",  // implements_type
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithImportLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "java.util.*",
			"location", "8",  // import
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithIncludedPaths() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		param.put("project", "test-project");
		param.put("query", "test.query");
		param.put("location", "10");
		param.put("analysisMode", "source-only");

		ArrayList<String> includedPaths = new ArrayList<>();
		includedPaths.add("src/main/java");
		param.put("includedPaths", includedPaths);

		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testUnsupportedCommand() throws Exception {
		commandHandler.executeCommand("unsupported.command", null, null);
	}

	@Test
	public void testRuleEntryWithWildcardQuery() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "java.io.*",
			"location", "10",
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithPackageLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "java.io",
			"location", "11",  // package
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithFieldLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "java.lang.String",
			"location", "12",  // field
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithMethodDeclarationLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "toString",
			"location", "13",  // method_declaration
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}

	@Test
	public void testRuleEntryWithClassDeclarationLocation() throws Exception {
		List<Object> params = new ArrayList<>();
		Map<String, String> param = Map.of(
			"project", "test-project",
			"query", "TestClass",
			"location", "14",  // class_declaration
			"analysisMode", "source-only"
		);
		params.add(param);

		List<Object> result = (List<Object>) commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null);
		assertNotNull(result);
	}
}
