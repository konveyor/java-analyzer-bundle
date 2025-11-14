package io.konveyor.tackle.core.internal.symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class SymbolProviderResolver {
    private Map<Integer, Supplier<SymbolProvider>> map;

    public SymbolProviderResolver() {
        map = new HashMap<>();
        map.put(1, InheritanceSymbolProvider::new);
        map.put(2, MethodCallSymbolProvider::new);
        map.put(3, ConstructorCallSymbolProvider::new);
        map.put(4, AnnotationSymbolProvider::new);
        map.put(5, ImplementsTypeSymbolProvider::new);
        map.put(6, EnumConstantSymbolProvider::new);
        map.put(7, ReturnTypeSymbolProvider::new);
        map.put(8, ImportSymbolProvider::new);
        map.put(9, VariableDeclarationSymbolProvider::new);
        map.put(10, TypeSymbolProvider::new);
        map.put(11, PackageDeclarationSymbolProvider::new);
        map.put(12, FieldSymbolProvider::new);
        map.put(13, MethodDeclarationSymbolProvider::new);
        map.put(14, ClassDeclarationSymbolProvider::new);
    }

    public Supplier<SymbolProvider> resolve(Integer i) {
        return Optional.ofNullable(this.map.get(i)).orElse(DefaultSymbolProvider::new);
    }
}
