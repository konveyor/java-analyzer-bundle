package io.konveyor.tackle.core.internal.symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.core.search.SearchMatch;

public class SymbolProviderResolver {
    private static Map<Integer, SymbolProvider> map;
    static {
        map = new HashMap<>();
        map.put(1, new InheritanceSymbolProvider());
        map.put(2, new MethodCallSymbolProvider());
        map.put(3, new ConstructorCallSymbolProvider());
        map.put(4, new AnnotationSymbolProvider());
        map.put(5, new ImplementsTypeSymbolProvider());
        map.put(6, new DefaultSymbolProvider());
        map.put(7, new ReturnTypeSymbolProvider());
        map.put(8, new ImportSymbolProvider());
        map.put(9, new VariableDeclarationSymbolProvider());
        map.put(10, new TypeSymbolProvider());
        map.put(11, new ReferenceSymbolProvider());
    }

    public static SymbolProvider resolve(Integer i, SearchMatch SearchMatch) {
        return Optional.ofNullable(map.get(i)).orElse(new DefaultSymbolProvider());
    }
}
