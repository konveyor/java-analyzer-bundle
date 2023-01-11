package io.konveyor.tackle.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/**
* Sample integration test. In Eclipse, right-click > Run As > JUnit-Plugin. <br/>
* In Maven CLI, run "mvn integration-test".
*/
public class SampleDelegateCommandHandlerTest {

	private SampleDelegateCommandHandler commandHandler;

	@Before
	public void setUp() {
		commandHandler = new SampleDelegateCommandHandler();
	}

	@Test
	public void veryStupidTest() throws Exception {
		assertEquals("Hello World", commandHandler.executeCommand(SampleDelegateCommandHandler.COMMAND_ID, null, null));

		// Test call with params in list

		List<Object> params = new ArrayList<Object>();

		Map<String, String> param = Map.of("project", "project", "query", "customresourcedefinition");
		params.add(param);


		assertEquals(Collections.emptyList(), commandHandler.executeCommand(SampleDelegateCommandHandler.RULE_ENTRY_COMMAND_ID, params, null));
	}
}