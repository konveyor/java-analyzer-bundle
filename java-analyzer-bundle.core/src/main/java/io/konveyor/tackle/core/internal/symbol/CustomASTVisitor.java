package io.konveyor.tackle.core.internal.symbol;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.search.SearchMatch;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

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

    /* 
     * we re-use this same class for different locations in a query
     * we should only be visiting nodes specific to a location, not all
    */
    public enum QueryLocation {
        METHOD_CALL,
        CONSTRUCTOR_CALL,
    }

    public CustomASTVisitor(String query, SearchMatch match, QueryLocation location) {
        /*
         * When comparing query pattern with an actual java element's fqn
         * we need to make sure that * not preceded with a . are replaced
         * by .* so that java regex works as expected on them
        */
        this.query = query.replaceAll("(?<!\\.)\\*", ".*");
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
                    String fullyQualifiedName = declaringClass.getQualifiedName() + "." + binding.getName();
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
            logInfo("error visiting MethodInvocation node: " + e);
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
                    String fullyQualifiedName = declaringClass.getQualifiedName() + "." + binding.getName();
                    // match fqn with query pattern
                    if (fullyQualifiedName.matches(this.query)) {
                        this.symbolMatches = true;
                        return false;
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
            logInfo("error visiting ConstructorInvocation node: " + e);
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
                    String fullyQualifiedName = declaringClass.getQualifiedName() + "." + binding.getName();
                    // match fqn with query pattern
                    if (fullyQualifiedName.matches(this.query)) {
                        this.symbolMatches = true;
                        return false;
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

    public boolean symbolMatches() {
        return this.symbolMatches;
    }
}
