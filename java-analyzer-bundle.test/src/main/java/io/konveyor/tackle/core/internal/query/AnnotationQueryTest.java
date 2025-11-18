package io.konveyor.tackle.core.internal.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class AnnotationQueryTest {

	@Test
	public void testBasicConstructor() {
		Map<String, String> elements = new HashMap<>();
		elements.put("value", "testValue");

		AnnotationQuery query = new AnnotationQuery("javax.ejb.Stateless", elements, false);

		assertEquals("javax.ejb.Stateless", query.getType());
		assertEquals(elements, query.getElements());
		assertFalse(query.isOnAnnotation());
	}

	@Test
	public void testIsOnAnnotation() {
		AnnotationQuery query = new AnnotationQuery("javax.ejb.Stateless", new HashMap<>(), true);

		assertTrue(query.isOnAnnotation());
	}

	@Test
	public void testMatchesAnnotationExact() {
		AnnotationQuery query = new AnnotationQuery("javax.ejb.Stateless", new HashMap<>(), false);

		assertTrue(query.matchesAnnotation("javax.ejb.Stateless"));
	}

	@Test
	public void testMatchesAnnotationPattern() {
		AnnotationQuery query = new AnnotationQuery("javax\\.ejb\\..*", new HashMap<>(), false);

		assertTrue(query.matchesAnnotation("javax.ejb.Stateless"));
		assertTrue(query.matchesAnnotation("javax.ejb.Stateful"));
		assertFalse(query.matchesAnnotation("javax.persistence.Entity"));
	}

	@Test
	public void testMatchesAnnotationWildcard() {
		AnnotationQuery query = new AnnotationQuery(".*\\.Stateless", new HashMap<>(), false);

		assertTrue(query.matchesAnnotation("javax.ejb.Stateless"));
		assertTrue(query.matchesAnnotation("com.example.Stateless"));
		assertFalse(query.matchesAnnotation("javax.ejb.Stateful"));
	}

	@Test
	public void testMatchesAnnotationOnAnnotationWithNullType() {
		// When isOnAnnotation is true and type is null, it should match any annotation
		AnnotationQuery query = new AnnotationQuery(null, new HashMap<>(), true);

		assertTrue(query.matchesAnnotation("javax.ejb.Stateless"));
		assertTrue(query.matchesAnnotation("any.annotation.Type"));
	}

	@Test
	public void testMatchesAnnotationOnAnnotationWithType() {
		AnnotationQuery query = new AnnotationQuery("javax\\.ejb\\..*", new HashMap<>(), true);

		assertTrue(query.matchesAnnotation("javax.ejb.Stateless"));
		assertFalse(query.matchesAnnotation("javax.persistence.Entity"));
	}

	@Test
	public void testFromMapBasic() {
		Map<String, Object> annotationQueryMap = new HashMap<>();
		annotationQueryMap.put("pattern", "javax.ejb.Stateless");
		annotationQueryMap.put("elements", new ArrayList<>());

		AnnotationQuery query = AnnotationQuery.fromMap("fallbackQuery", annotationQueryMap, 10);

		assertNotNull(query);
		assertEquals("javax.ejb.Stateless", query.getType());
		assertFalse(query.isOnAnnotation());
	}

	@Test
	public void testFromMapWithElements() {
		Map<String, Object> annotationQueryMap = new HashMap<>();
		annotationQueryMap.put("pattern", "javax.ejb.Stateless");

		List<Map<String, String>> elements = new ArrayList<>();
		Map<String, String> element1 = new HashMap<>();
		element1.put("name", "value");
		element1.put("value", "TestBean");
		elements.add(element1);

		Map<String, String> element2 = new HashMap<>();
		element2.put("name", "description");
		element2.put("value", "Test Description");
		elements.add(element2);

		annotationQueryMap.put("elements", elements);

		AnnotationQuery query = AnnotationQuery.fromMap("fallbackQuery", annotationQueryMap, 10);

		assertNotNull(query);
		assertEquals("javax.ejb.Stateless", query.getType());
		assertEquals(2, query.getElements().size());
		assertEquals("TestBean", query.getElements().get("value"));
		assertEquals("Test Description", query.getElements().get("description"));
	}

	@Test
	public void testFromMapOnAnnotationLocation() {
		Map<String, Object> annotationQueryMap = new HashMap<>();
		annotationQueryMap.put("pattern", "javax.ejb.Stateless");
		annotationQueryMap.put("elements", new ArrayList<>());

		// Location 4 is annotation location
		AnnotationQuery query = AnnotationQuery.fromMap("fallbackQuery", annotationQueryMap, 4);

		assertNotNull(query);
		assertTrue(query.isOnAnnotation());
	}

	@Test
	public void testFromMapOnAnnotationWithEmptyPattern() {
		Map<String, Object> annotationQueryMap = new HashMap<>();
		annotationQueryMap.put("pattern", "");
		annotationQueryMap.put("elements", new ArrayList<>());

		// Location 4 is annotation location, empty pattern should use fallback query
		AnnotationQuery query = AnnotationQuery.fromMap("fallbackQuery", annotationQueryMap, 4);

		assertNotNull(query);
		assertEquals("fallbackQuery", query.getType());
		assertTrue(query.isOnAnnotation());
	}

	@Test
	public void testFromMapNonAnnotationLocation() {
		Map<String, Object> annotationQueryMap = new HashMap<>();
		annotationQueryMap.put("pattern", "javax.ejb.Stateless");
		annotationQueryMap.put("elements", new ArrayList<>());

		// Location 10 is type location
		AnnotationQuery query = AnnotationQuery.fromMap("fallbackQuery", annotationQueryMap, 10);

		assertNotNull(query);
		assertFalse(query.isOnAnnotation());
	}

	@Test
	public void testFromMapNullMap() {
		AnnotationQuery query = AnnotationQuery.fromMap("fallbackQuery", null, 10);

		assertNull(query);
	}

	@Test
	public void testFromMapNullElements() {
		Map<String, Object> annotationQueryMap = new HashMap<>();
		annotationQueryMap.put("pattern", "javax.ejb.Stateless");
		// Not setting elements field

		AnnotationQuery query = AnnotationQuery.fromMap("fallbackQuery", annotationQueryMap, 10);

		assertNotNull(query);
		assertEquals("javax.ejb.Stateless", query.getType());
		assertTrue(query.getElements().isEmpty());
	}

	@Test
	public void testElementsAreIndependent() {
		Map<String, String> elements = new HashMap<>();
		elements.put("value", "original");

		AnnotationQuery query = new AnnotationQuery("test.Type", elements, false);

		// Modify original map
		elements.put("value", "modified");

		// Query should still have the same reference (we're not doing defensive copy)
		assertEquals("modified", query.getElements().get("value"));
	}

	@Test
	public void testComplexPatternMatching() {
		// Test more complex regex patterns
		AnnotationQuery query = new AnnotationQuery("javax\\.(ejb|persistence)\\..*", new HashMap<>(), false);

		assertTrue(query.matchesAnnotation("javax.ejb.Stateless"));
		assertTrue(query.matchesAnnotation("javax.persistence.Entity"));
		assertFalse(query.matchesAnnotation("javax.servlet.WebServlet"));
	}

	@Test
	public void testEmptyElementsMap() {
		Map<String, String> elements = new HashMap<>();
		AnnotationQuery query = new AnnotationQuery("test.Type", elements, false);

		assertNotNull(query.getElements());
		assertTrue(query.getElements().isEmpty());
	}

	@Test
	public void testFromMapMultipleLocations() {
		// Test various location types
		int[] locations = {0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14};

		for (int location : locations) {
			Map<String, Object> annotationQueryMap = new HashMap<>();
			annotationQueryMap.put("pattern", "test.Pattern");
			annotationQueryMap.put("elements", new ArrayList<>());

			AnnotationQuery query = AnnotationQuery.fromMap("fallback", annotationQueryMap, location);

			assertNotNull(query);
			if (location == 4) {
				assertTrue("Location " + location + " should be on annotation", query.isOnAnnotation());
			} else {
				assertFalse("Location " + location + " should not be on annotation", query.isOnAnnotation());
			}
		}
	}
}
