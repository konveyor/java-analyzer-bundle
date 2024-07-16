package io.konveyor.tackle.core.internal.symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SymbolProviderResolver {
    private Map<Integer, SymbolProvider> map;

    public SymbolProviderResolver() {
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
        map.put(12, new FieldSymbolProvider());
        map.put(13, new MethodDeclarationSymbolProvider());
    }

    public SymbolProvider resolve(Integer i) {
        return Optional.ofNullable(this.map.get(i)).orElse(new DefaultSymbolProvider());
    }
}
