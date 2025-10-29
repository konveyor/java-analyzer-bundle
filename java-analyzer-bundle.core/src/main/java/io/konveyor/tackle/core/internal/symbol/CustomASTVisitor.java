package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Parameter;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

/*
 * SearchEngine we use often gives us more matches than needed when
 * query contains a * and/or contains a fqn. e.g. java.io.paths.get* 
 * This class exists to help us get accurate results for such queries
 * For different type of symbols we get from a match, we try to
 * get fqn of that symbol and ensure it matches the given query
 * (pgaikwad): if you can, please make the visit() functions DRYer
 */
public class CustomASTVisitor extends ASTVisitor {
    private String query;
    private SearchPattern pattern;
    private SearchMatch match;
    private boolean symbolMatches;
    private QueryLocation location;
    private List<String> queryParameterTypes;

    /* 
     * we re-use this same class for different locations in a query
     * we should only be visiting nodes specific to a location, not all
    */
    public enum QueryLocation {
        METHOD_CALL,
        CONSTRUCTOR_CALL,
        ANNOTATION,
    }

    public CustomASTVisitor(String query, SearchPattern pattern, SearchMatch match, QueryLocation location) {
        /*
         * Extract parameter types from the query pattern if present
         * e.g., "java.util.Properties.setProperty(java.lang.String, java.lang.String)"
         * We'll extract ["java.lang.String", "java.lang.String"] and strip the parameters from the query
         */
        this.queryParameterTypes = extractParameterTypes(query);
        String processedQuery = query.replaceAll("\\([^)]*\\)", "");

        /*
         * When comparing query pattern with an actual java element's fqn
         * we need to make sure that * not preceded with a . are replaced
         * by .* so that java regex works as expected on them
        */
        this.query = processedQuery.replaceAll("(?<!\\.)\\*", ".*");
        this.pattern = pattern;
        this.symbolMatches = false;
        this.match = match;
        // depending on which location the query was for we only want to
        // visit certain nodes
        this.location = location;
    }

    /*
     * When visiting AST nodes, it may happen that we visit more nodes than
     * needed. We need to ensure that we are only visiting ones that are found
     * in the given search match. I wrote this for methods / constructors where 
     * I observed that node starts at the beginning of line whereas match starts 
     * at an offset within that line. However, both end on the same position. This 
     * could differ for other locations. In that case, change logic based on type of
     * the node you get.
     */
    private boolean shouldVisit(ASTNode node) {
        return (this.match.getOffset() + this.match.getLength()) == 
            (node.getStartPosition() + node.getLength());
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        return visit((Annotation) node);
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        return visit((Annotation) node);
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        return visit((Annotation) node);
    }

    private boolean visit(Annotation node) {
        // There is a problem with trying to run shouldVisit() here because
        // matches on annotations aren't directly on the annotation node,
        // but on the annotated one (class, method, field, etc). So we can't
        // use shouldVisit() to filter out nodes we don't want to visit.
        // TODO: think of a better way to handle this
        if (this.location != QueryLocation.ANNOTATION) {
            return true;
        }
        try {
            IAnnotationBinding binding = node.resolveAnnotationBinding();
            if (binding != null) {
                // get fqn
                ITypeBinding declaringClass = binding.getAnnotationType();
                if (declaringClass != null) {
                    // Handle Erasure results
                    if (declaringClass.getErasure() != null) {
                        declaringClass = declaringClass.getErasure();
                    }
                    String fullyQualifiedName = declaringClass.getQualifiedName();
                    // match fqn with query pattern
                    // Note: We keep using regex matching for now as SearchPattern doesn't expose
                    // a simple string matching API. The pattern is passed for potential future use.
                    boolean matches = fullyQualifiedName.matches(this.query);
                    if (matches) {
                        this.symbolMatches = true;
                        return false;
                    } else {
                        logInfo("method fqn " + fullyQualifiedName + " did not match with " + query);
                        return true;
                    }
                }
            }
            logInfo("failed to get accurate info for MethodInvocation, falling back");
            // sometimes binding or declaring class cannot be found, usually due to errors
            // in source code. in that case, we will fallback and accept the match
            this.symbolMatches = true;
            return false;
        } catch (Exception e) {
            logInfo("KONVEYOR_LOG: error visiting MethodInvocation node: " + e);
            // this is so that we fallback and don't lose a match when we fail
            this.symbolMatches = true;
            return false;
        }
    }

    /* 
     * This is to get information from a MethodInvocation, used for METHOD_CALL
     * we discard a match only when we can tell for sure. otherwise we accept
     * returning false stops further visits
    */
    @Override
    public boolean visit(MethodInvocation node) {
        if (this.location != QueryLocation.METHOD_CALL || !this.shouldVisit(node)) {
            return true;
        }
        try {
            IMethodBinding binding = node.resolveMethodBinding();
            if (binding != null) {
                // get fqn of the method being called
                ITypeBinding declaringClass = binding.getDeclaringClass();
                if (declaringClass != null) {
                    // Handle Erasure results
                    if (declaringClass.getErasure() != null) {
                        declaringClass = declaringClass.getErasure();
                    }
                    String fullyQualifiedName = declaringClass.getQualifiedName() + "." + binding.getName();
                    // match fqn with query pattern
                    if (fullyQualifiedName.matches(this.query)) {
                        // Check parameter types if specified in the query
                        ITypeBinding[] parameterTypes = binding.getParameterTypes();
                        if (matchesParameterTypes(parameterTypes)) {
                            this.symbolMatches = true;
                            return false;
                        } else {
                            logInfo("method parameters did not match query parameters");
                            return true;
                        }
                    } else {
                        logInfo("method fqn " + fullyQualifiedName + " did not match with " + query);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logInfo("KONVEYOR_LOG: error visiting MethodInvocation node: " + e);
            // this is so that we fallback and don't lose a match when we fail
            this.symbolMatches = true;
            return false;
        }
    }
    
    /* 
     * This is to get information from a ConstructorInvocation, used for CONSTRUCTOR_CALL
     * we discard a match only when we can tell for sure. otherwise we accept
     * returning false stops further visits
    */
    @Override
    public boolean visit(ConstructorInvocation node) {
        if (this.location != QueryLocation.CONSTRUCTOR_CALL || !this.shouldVisit(node)) {
            return true;
        }
        try {
            IMethodBinding binding = node.resolveConstructorBinding();
            logInfo("get type parameters: " + binding.getTypeParameters());
            if (binding != null) {
                // get fqn of the method being called
                ITypeBinding declaringClass = binding.getDeclaringClass();
                if (declaringClass != null) {
                    String fullyQualifiedName = declaringClass.getQualifiedName();
                    // match fqn with query pattern
                    if (fullyQualifiedName.matches(this.query)) {
                        // Check parameter types if specified in the query
                        ITypeBinding[] parameterTypes = binding.getParameterTypes();
                        if (matchesParameterTypes(parameterTypes)) {
                            this.symbolMatches = true;
                            return false;
                        } else {
                            logInfo("constructor parameters did not match query parameters");
                            return true;
                        }
                    } else {
                        logInfo("constructor fqn " + fullyQualifiedName + " did not match with " + query);
                        return true;
                    }
                }
            }
            logInfo("failed to get accurate info for ConstructorInvocation, falling back");
            // sometimes binding or declaring class cannot be found, usually due to errors
            // in source code. in that case, we will fallback and accept the match
            this.symbolMatches = true;
            return false;
        } catch (Exception e) {
            logInfo("KONVEYOR_LOG: error visiting ConstructorInvocation node: " + e);
            // this is so that we fallback and don't lose a match when we fail
            this.symbolMatches = true;
            return false;
        }
    }

    /* 
     * This is to get information from a ClassInstanceCreation, used for CONSTRUCTOR_CALL
     * we discard a match only when we can tell for sure. otherwise we accept
     * returning false stops further visits
    */
    @Override
    public boolean visit(ClassInstanceCreation node) {
        if (this.location != QueryLocation.CONSTRUCTOR_CALL || !this.shouldVisit(node)) {
            return true;
        }
        try {
            IMethodBinding binding = node.resolveConstructorBinding();
            logInfo("get type parameters: " + binding.getTypeParameters());
            if (binding != null) {
                // get fqn of the method being called
                ITypeBinding declaringClass = binding.getDeclaringClass();
                if (declaringClass != null) {
                    String fullyQualifiedName = declaringClass.getQualifiedName();
                    // match fqn with query pattern
                    if (fullyQualifiedName.matches(this.query)) {
                        // Check parameter types if specified in the query
                        ITypeBinding[] parameterTypes = binding.getParameterTypes();
                        if (matchesParameterTypes(parameterTypes)) {
                            this.symbolMatches = true;
                            return false;
                        } else {
                            logInfo("constructor parameters did not match query parameters");
                            return true;
                        }
                    } else {
                        logInfo("constructor fqn " + fullyQualifiedName + " did not match with " + query);
                        return true;
                    }
                }
            }
            logInfo("failed to get accurate info for ClassInstanceCreation, falling back");
            // sometimes binding or declaring class cannot be found, usually due to errors
            // in source code. in that case, we will fallback and accept the match
            this.symbolMatches = true;
            return false;
        } catch (Exception e) {
            logInfo("error visiting ConstructorInvocation node: " + e);
            // this is so that we fallback and don't lose a match when we fail
            this.symbolMatches = true;
            return false;
        }
    }

    /**
     * Extracts parameter types from a query pattern.
     * e.g., "ClassName.method(Type1, Type2)" -> ["Type1", "Type2"]
     * Returns null if no parameters are specified in the query
     *
     * Note: Must distinguish between method parameters like "method(String, int)"
     * and regex alternation groups like "java.io.(FileWriter|FileReader)"
     */
    List<String> extractParameterTypes(String query) {
        // Performance: Quick check before indexOf
        int openParen = query.indexOf('(');
        if (openParen == -1) {
            return null;  // No parameters specified in query
        }

        int closeParen = query.lastIndexOf(')');
        if (closeParen == -1 || openParen >= closeParen) {
            return null;  // Invalid or no parameters
        }

        // Check if this looks like method parameters vs regex alternation
        // Method parameters: "ClassName.methodName(Type1, Type2)" - parens at END with optional * after
        // Regex alternation: "java.io.(FileWriter|FileReader)*" - parens in MIDDLE with content after
        //
        // Heuristic: If there's a '|' (pipe) character inside the parentheses, it's likely
        // a regex alternation group, not method parameters
        String potentialParams = query.substring(openParen + 1, closeParen);
        if (potentialParams.contains("|")) {
            return null;  // Regex alternation, not method parameters
        }

        // Another check: method parameters should be near the end of the pattern
        // Look for content after the closing paren (besides wildcards * which are common)
        String afterParen = query.substring(closeParen + 1).trim();
        if (afterParen.length() > 0 && !afterParen.matches("\\**")) {
            // There's significant content after the closing paren, not method parameters
            return null;
        }

        String paramsString = potentialParams.trim();
        if (paramsString.isEmpty()) {
            return Collections.emptyList();  // Empty parameter list: method()
        }

        // Performance: Pre-size ArrayList for common cases (most methods have 1-3 params)
        List<String> params = new ArrayList<>(4);

        // Split by comma, handling nested generics like Map<String, Integer>
        int depth = 0;
        int start = 0;
        int length = paramsString.length();

        for (int i = 0; i < length; i++) {
            char c = paramsString.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                params.add(paramsString.substring(start, i).trim());
                start = i + 1;
            }
        }
        // Add the last parameter
        params.add(paramsString.substring(start).trim());

        return params;
    }

    /**
     * Checks if the actual parameter types from a method binding match the query parameter types.
     * Supports wildcards (*) and subtype matching.
     */
    private boolean matchesParameterTypes(ITypeBinding[] actualTypes) {
        // Performance: Early return if no parameter filter (most common case for backward compat)
        if (queryParameterTypes == null) {
            return true;  // No parameter filter specified in query
        }

        // Handle null actualTypes (can happen with certain bindings)
        if (actualTypes == null) {
            return queryParameterTypes.isEmpty();  // Match only if query expects no parameters
        }

        // Performance: Quick length check before iterating
        int paramCount = queryParameterTypes.size();
        if (paramCount != actualTypes.length) {
            return false;  // Different number of parameters
        }

        // Performance: Avoid loop overhead for common cases
        if (paramCount == 0) {
            return true;  // Both have no parameters
        }

        // Check each parameter type
        for (int i = 0; i < paramCount; i++) {
            if (!typeMatches(queryParameterTypes.get(i), actualTypes[i])) {
                return false;  // Early exit on first mismatch
            }
        }

        return true;
    }

    /**
     * Checks if a query type pattern matches an actual type binding.
     * Supports:
     * - Wildcards: "*" matches any type
     * - Subtype matching: "java.lang.Object" matches "java.lang.String"
     * - Generic signatures: "java.util.List<java.lang.String>" matches only that exact generic type
     */
    private boolean typeMatches(String queryType, ITypeBinding actualType) {
        if (actualType == null) {
            return false;
        }

        // Wildcard matches any type
        if ("*".equals(queryType)) {
            return true;
        }

        // Normalize varargs notation in the query ("Type..." -> "Type[]")
        // so it matches JDT's representation of varargs parameters as arrays.
        String normalizedQueryType = queryType;
        if (queryType.endsWith("...")) {
            normalizedQueryType = queryType.substring(0, queryType.length() - 3) + "[]";
        }

        // Performance optimization: primitives can only match exactly, not via subtype
        if (actualType.isPrimitive()) {
            return normalizedQueryType.equals(actualType.getName());
        }

        // Get the qualified name of the actual type (cache to avoid recomputation)
        String actualTypeName = getQualifiedTypeName(actualType);

        // Exact match - most common case, check first
        if (normalizedQueryType.equals(actualTypeName)) {
            return true;
        }

        // Quick check: try erasure type before expensive hierarchy traversal
        ITypeBinding erasure = actualType.getErasure();
        if (erasure != null && erasure != actualType) {
            String erasureName = erasure.getQualifiedName();
            if (normalizedQueryType.equals(erasureName)) {
                return true;
            }
        }

        // Subtype matching: check if actualType is assignable to queryType
        // Only do this expensive check if exact match failed
        return isSubtypeOf(actualType, normalizedQueryType, actualTypeName, new HashSet<>());
    }

    /**
     * Gets the fully qualified name of a type, including generic parameters.
     * e.g., "java.util.List<java.lang.String>"
     */
    private String getQualifiedTypeName(ITypeBinding type) {
        if (type == null) {
            return "";
        }

        // Handle arrays
        if (type.isArray()) {
            return getQualifiedTypeName(type.getElementType()) + "[]";
        }

        // Handle primitives
        if (type.isPrimitive()) {
            return type.getName();
        }

        // Get erasure for generic types to get base qualified name
        ITypeBinding erasure = type.getErasure();
        String baseName = erasure != null ? erasure.getQualifiedName() : type.getQualifiedName();

        // Add generic type arguments if present
        ITypeBinding[] typeArguments = type.getTypeArguments();
        if (typeArguments != null && typeArguments.length > 0) {
            StringBuilder sb = new StringBuilder(baseName);
            sb.append("<");
            for (int i = 0; i < typeArguments.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(getQualifiedTypeName(typeArguments[i]));
            }
            sb.append(">");
            return sb.toString();
        }

        return baseName;
    }

    /**
     * Checks if actualType is a subtype of (or equal to) queryTypeName.
     * Walks up the type hierarchy checking superclasses and interfaces.
     *
     * @param actualType the type to check
     * @param queryTypeName the query type name to match against
     * @param actualTypeName cached qualified name of actualType (performance optimization)
     * @param visited set of already visited types to prevent infinite loops
     */
    private boolean isSubtypeOf(ITypeBinding actualType, String queryTypeName,
                                String actualTypeName, Set<String> visited) {
        if (actualType == null) {
            return false;
        }

        // Performance: Check if we already examined this type (cycle detection)
        String typeKey = actualType.getKey();
        if (typeKey != null && !visited.add(typeKey)) {
            return false;  // Already visited, avoid infinite loop
        }

        // Use cached actualTypeName (already computed in typeMatches)
        if (queryTypeName.equals(actualTypeName)) {
            return true;
        }

        // Performance: Quick check of superclass before recursing
        ITypeBinding superclass = actualType.getSuperclass();
        if (superclass != null) {
            // Quick check: does superclass name match before recursing?
            ITypeBinding superErasure = superclass.getErasure();
            if (superErasure != null) {
                String superName = superErasure.getQualifiedName();
                if (queryTypeName.equals(superName)) {
                    return true;
                }
            }

            // Recurse to check full hierarchy
            String superTypeName = getQualifiedTypeName(superclass);
            if (isSubtypeOf(superclass, queryTypeName, superTypeName, visited)) {
                return true;
            }
        }

        // Performance: Check interfaces with same optimization
        ITypeBinding[] interfaces = actualType.getInterfaces();
        if (interfaces != null && interfaces.length > 0) {
            for (ITypeBinding interfaceType : interfaces) {
                // Quick check erasure name first
                ITypeBinding intfErasure = interfaceType.getErasure();
                if (intfErasure != null) {
                    String intfName = intfErasure.getQualifiedName();
                    if (queryTypeName.equals(intfName)) {
                        return true;
                    }
                }

                // Recurse to check full hierarchy
                String intfTypeName = getQualifiedTypeName(interfaceType);
                if (isSubtypeOf(interfaceType, queryTypeName, intfTypeName, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean symbolMatches() {
        return this.symbolMatches;
    }
}
