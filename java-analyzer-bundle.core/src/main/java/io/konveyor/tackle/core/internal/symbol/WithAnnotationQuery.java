package io.konveyor.tackle.core.internal.symbol;

import io.konveyor.tackle.core.internal.query.AnnotationQuery;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.core.Annotation;
import org.eclipse.jdt.internal.core.SourceRefElement;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mixin interface for {@link SymbolProvider}s that allows for Annotation queries to be performed.
 */
public interface WithAnnotationQuery {
    AnnotationQuery getAnnotationQuery();
    void setAnnotationQuery(AnnotationQuery annotationQuery);

    /**
     * Checks whether a given matched symbol has the corresponding annotations, if any.
     *
     * @param match the matched symbol.
     * @param matchClasses a list of potential classes for the match to be cast to. In order to get the annotations from
     *                     the match, it must be cast to a specific type of symbol. See usage examples.
     * @return a boolean indicating whether the symbol has the specified annotations.
     */
    default boolean matchesAnnotationQuery(SearchMatch match, List<Class<? extends SourceRefElement>> matchClasses) {
        if (getAnnotationQuery() != null) {
            try {
                // Try to cast the match element into one of the given matchClasses
                IAnnotation[] annotations = matchClasses.stream()
                        .filter(c -> c.isInstance(match.getElement()))
                        .map(c -> c.cast(match.getElement()))
                        .map(this::tryToGetAnnotations).findFirst().orElse(new IAnnotation[]{});

                // If we are expecting annotations and the symbol is not annotated, return false
                if (annotations.length == 0) {
                    return false;
                }

                // Iterate over the annotation this symbol is annotated with
                for (IAnnotation annotation : annotations) {
                    // See if the annotation's name matches the pattern given in the query for the annotation
                    String fqn = getFQN(annotation);
                    if (Pattern.matches(getAnnotationQuery().getType(), fqn)) {
                        // If the query has annotation elements to check, iterate through the annotation's values and check
                        if (getAnnotationQuery().getElements() != null && !getAnnotationQuery().getElements().entrySet().isEmpty()) {
                            IMemberValuePair[] memberValuePairs = annotation.getMemberValuePairs();
                            Set<Map.Entry<String, String>> annotationElements = getAnnotationQuery().getElements().entrySet();
                            for (IMemberValuePair member : memberValuePairs) {
                                for (Map.Entry<String, String> annotationElement : annotationElements) {
                                    if (annotationElement.getKey().equals(member.getMemberName())) {
                                        // Member values can be arrays. In this case, lets iterate over it and compare:
                                        if (member.getValue() instanceof Object[]) {
                                            Object[] values = (Object[]) member.getValue();
                                            // TODO: at the moment we are just toString()ing the values.
                                            //  We might want to make this more sophisticated, relying on
                                            //  member.getValueKind() to match on specific kinds.
                                            return Arrays.stream(values).anyMatch(v -> Pattern.matches(annotationElement.getValue(), v.toString()));
                                        } else {
                                            if (Pattern.matches(annotationElement.getValue(), member.getValue().toString())) {
                                                return true;
                                            }
                                        }
                                    }
                                }

                            }
                        } else {
                            // No annotation elements, but the annotation itself matches
                            return true;
                        }
                    }
                }
                return false;
            } catch(JavaModelException e) {
                // TODO: print exception
                return false;
            }
        }

        return true;
    }

    private IAnnotation[] tryToGetAnnotations(SourceRefElement t) {
        try {
            return t.getAnnotations();
        } catch (JavaModelException e) {
            return new IAnnotation[]{};
        }
    }

    /**
     * Tries to extract the fqn of the annotation from the list of imports of the compilation unit.
     */
    private String getFQN(IAnnotation annotation) {
        String name = annotation.getElementName();
        if (Pattern.matches(".*\\.", name)) {
            // If the name of the annotation has a dot on it, it's a fqn
            return name;
        } else {
            // If not, the annotation must have been imported. Look in the imports:
            try {
                return Arrays.stream(((Annotation) annotation).getCompilationUnit().getImports())
                        .filter(i -> i.getElementName().endsWith(name))
                        .findFirst()
                        .map(IImportDeclaration::getElementName)
                        .orElse("");
            } catch (JavaModelException e) {
                e.printStackTrace();
                return "";
            }
        }
    }
}
