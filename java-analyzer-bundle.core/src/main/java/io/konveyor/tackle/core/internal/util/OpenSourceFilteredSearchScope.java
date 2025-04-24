package io.konveyor.tackle.core.internal.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import java.util.Arrays;

/*
 * A search scope that excludes open source libraries using the exclusion manager
 */
public class OpenSourceFilteredSearchScope implements IJavaSearchScope {
    private final IJavaSearchScope scope;
    private final OpenSourceLibraryExclusionManager exclusionManager;

    public OpenSourceFilteredSearchScope(IJavaSearchScope scope) {
        this.scope = scope;
        this.exclusionManager = OpenSourceLibraryExclusionManager.getInstance();
    }

    @Override
    public boolean encloses(String resourcePath) {
        if (exclusionManager.shouldExcludeLibrary(resourcePath)) {
            return false;
        }
        return scope.encloses(resourcePath);
    }

    @Override
    public boolean encloses(IJavaElement element) {
        IPackageFragmentRoot root = (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
        if (root != null && exclusionManager.shouldExcludePackageRoot(root)) {
            return false;
        }
        return scope.encloses(element);
    }

    @Override
    public IPath[] enclosingProjectsAndJars() {
        // Filter out excluded JARs from the enclosing projects and JARs
        IPath[] original = scope.enclosingProjectsAndJars();
        return Arrays.stream(original)
                .filter(path -> !exclusionManager.shouldExcludeLibrary(path.toString()))
                .toArray(IPath[]::new);
    }

    @Override
    public boolean includesBinaries() {
        return scope.includesBinaries();
    }

    @Override
    public boolean includesClasspaths() {
        return scope.includesClasspaths();
    }

    @Override
    public void setIncludesBinaries(boolean includesBinaries) {
        scope.setIncludesBinaries(includesBinaries);
    }

    @Override
    public void setIncludesClasspaths(boolean includesClasspaths) {
        scope.setIncludesClasspaths(includesClasspaths);
    }
}