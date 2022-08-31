package io.konveyor.tackle.core.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParamUtils {

    public static Map<String, Object> getFirst(List<Object> arguments) {
        return arguments.isEmpty() ? null : (Map<String, Object>) arguments.get(0);
    }

    public static String getString(Map<String, Object> obj, String key) {
		return (String) obj.get(key);
	}
    
}
