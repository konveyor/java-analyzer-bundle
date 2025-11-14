package io.konveyor.tackle.core.internal.symbol;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.search.SearchMatch;

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

    public CustomASTVisitor(String query, SearchMatch match, QueryLocation location) {
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
                    if (fullyQualifiedName.matches(this.query)) {
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
     */
    List<String> extractParameterTypes(String query) {
        int openParen = query.indexOf('(');
        int closeParen = query.lastIndexOf(')');

        if (openParen == -1 || closeParen == -1 || openParen >= closeParen) {
            return null;  // No parameters specified in query
        }

        String paramsString = query.substring(openParen + 1, closeParen).trim();
        if (paramsString.isEmpty()) {
            return Collections.emptyList();  // Empty parameter list: method()
        }

        List<String> params = new ArrayList<>();
        // Split by comma, handling nested generics like Map<String, Integer>
        int depth = 0;
        int start = 0;
        for (int i = 0; i < paramsString.length(); i++) {
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
        if (queryParameterTypes == null) {
            return true;  // No parameter filter specified in query
        }

        if (queryParameterTypes.size() != actualTypes.length) {
            return false;  // Different number of parameters
        }

        for (int i = 0; i < queryParameterTypes.size(); i++) {
            if (!typeMatches(queryParameterTypes.get(i), actualTypes[i])) {
                return false;
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

        // Get the qualified name of the actual type
        String actualTypeName = getQualifiedTypeName(actualType);

        // Exact match
        if (queryType.equals(actualTypeName)) {
            return true;
        }

        // Subtype matching: check if actualType is assignable to queryType
        // This requires resolving the queryType to a binding, which is complex
        // For now, we'll do a simple check by walking up the type hierarchy
        return isSubtypeOf(actualType, queryType);
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
     */
    private boolean isSubtypeOf(ITypeBinding actualType, String queryTypeName) {
        if (actualType == null) {
            return false;
        }

        // Check the type itself (including with generics)
        if (queryTypeName.equals(getQualifiedTypeName(actualType))) {
            return true;
        }

        // Check without generics (erasure)
        ITypeBinding erasure = actualType.getErasure();
        if (erasure != null && queryTypeName.equals(erasure.getQualifiedName())) {
            return true;
        }

        // Check superclass
        ITypeBinding superclass = actualType.getSuperclass();
        if (superclass != null && isSubtypeOf(superclass, queryTypeName)) {
            return true;
        }

        // Check interfaces
        ITypeBinding[] interfaces = actualType.getInterfaces();
        if (interfaces != null) {
            for (ITypeBinding interfaceType : interfaces) {
                if (isSubtypeOf(interfaceType, queryTypeName)) {
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
