package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.FieldReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.lsp4j.SymbolInformation;

/**
 * Symbol provider for enum constant references (location type 6).
 * Enum constants are special fields, so we search for fields and filter
 * for enum constants using IField.isEnumConstant().
 */
public class EnumConstantSymbolProvider implements SymbolProvider, WithQuery {
    private String query;

    @Override
    public List<SymbolInformation> get(SearchMatch match) {
        List<SymbolInformation> symbols = new ArrayList<>();
        try {
            IJavaElement element = (IJavaElement) match.getElement();

            // Enum constants are fields, so we need to check if the element is a field
            if (element.getElementType() == IJavaElement.FIELD) {
                IField field = (IField) element;

                // Only include if it's actually an enum constant
                if (field.isEnumConstant()) {
                    SymbolInformation symbol = new SymbolInformation();
                    symbol.setName(field.getElementName());
                    symbol.setKind(convertSymbolKind(field));
                    symbol.setContainerName(field.getParent().getElementName());
                    symbol.setLocation(getLocation(field, match));
                    symbols.add(symbol);
                }
            }
        } catch (Exception e) {
            logInfo("unable to convert enum constant match: " + e);
        }

        return symbols;
    }

    @Override
    public void setQuery(String query) {
        this.query = query;
    }
}
