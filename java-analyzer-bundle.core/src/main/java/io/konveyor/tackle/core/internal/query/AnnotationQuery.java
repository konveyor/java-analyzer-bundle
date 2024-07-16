package io.konveyor.tackle.core.internal.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents additional query information to inspect annotations in annotated symbols.
 */
public class AnnotationQuery {

    /**
     * The annotation type, ie: <code>@org.business.BeanAnnotation</code>
     */
    private String type;

    /**
     * The elements within the annotation, ie, "value" in <code>@BeanAnnotation(value = "value")</code>
     */
    private Map<String, String> elements;

    public AnnotationQuery(String type, Map<String, String> elements) {
        this.type = type;
        this.elements = elements;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getElements() {
        return elements;
    }

    public static AnnotationQuery fromMap(Map<String, Object> query) {
        if (query == null) {
            return null;
        }

        String typePattern = (String) query.get("pattern");
        final Map<String, String> elements = new HashMap<>();
        List<Map<String, String>> mapElements = (List<Map<String, String>>) query.get("elements");
        for (int i = 0; mapElements != null && i < mapElements.size(); i++) {
            String key = mapElements.get(i).get("name");
            String value = mapElements.get(i).get("value");
            elements.put(key, value);
        }

        return new AnnotationQuery(typePattern, elements);
    }
}
