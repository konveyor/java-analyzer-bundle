package io.konveyor.tackle.core.internal;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

/**
* Sample integration test. In Eclipse, right-click > Run As > JUnit-Plugin. <br/>
* In Maven CLI, run "mvn integration-test".
*/
public class CommandHandlerTest extends ProjectUtilsTest {

	@Test
	public void sampleCmdShouldReturnHelloWorld() throws Exception {
		assertEquals("Hello World", cmdHandler.executeCommand(SampleDelegateCommandHandler.COMMAND_ID, null, null));
	}

	@Test
	public void ruleEntryCmdShouldReturnEmptyListAsProjectIsNull() throws Exception {
		List<Object> params = new ArrayList<Object>();
		Map<String, String> param = Map.of(
			"project", "project",
			"query", "customresourcedefinition",
			"location","0",
			"analysisMode", ANALYSIS_MODE_SOURCE_ONLY);
		params.add(param);

		var result = cmdHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, new NullProgressMonitor());
		assertEquals(Collections.emptyList(), result);
	}
}