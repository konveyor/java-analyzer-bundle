package io.konveyor.tackle.core.internal.testing;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.osgi.framework.Bundle;

/**
 * Manages test project lifecycle for integration tests.
 * Handles project import (with Maven support), indexing synchronization, and cleanup.
 * 
 * <p>This class is designed to be used with shared project instances across tests.
 * Projects are imported once per test class and cleaned up after all tests complete.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @BeforeClass
 * public static void setUp() throws Exception {
 *     project = TestProjectManager.importMavenProject("test-project");
 *     TestProjectManager.waitForProjectReady(project);
 * }
 * 
 * @AfterClass
 * public static void tearDown() throws Exception {
 *     TestProjectManager.deleteProject(project);
 * }
 * }</pre>
 */
@SuppressWarnings("restriction")
public class TestProjectManager {

    private static final String TEST_BUNDLE_ID = "java-analyzer-bundle.test";
    private static final String PROJECTS_FOLDER = "projects";
    private static final String WORKING_DIR = "target/test-workspaces";
    
    private static final IProgressMonitor NULL_MONITOR = new NullProgressMonitor();
    
    // Track imported projects for cleanup
    private static final List<String> importedProjects = new ArrayList<>();

    /**
     * Imports a Maven project from the test resources.
     * The project is copied to a working directory and imported via M2E.
     * 
     * @param projectName Name of the project folder under projects/ (e.g., "test-project")
     * @return The imported IJavaProject
     * @throws Exception if import fails
     */
    public static IJavaProject importMavenProject(String projectName) throws Exception {
        logInfo("Importing Maven project: " + projectName);
        
        // Resolve source project path
        File sourceProjectDir = resolveTestProjectPath(projectName);
        if (!sourceProjectDir.exists()) {
            throw new IllegalArgumentException(
                "Test project not found: " + sourceProjectDir.getAbsolutePath());
        }
        
        // Copy to working directory
        File workingDir = getWorkingDirectory();
        File targetProjectDir = new File(workingDir, projectName);
        
        if (targetProjectDir.exists()) {
            logInfo("Cleaning existing working copy: " + targetProjectDir);
            FileUtils.deleteDirectory(targetProjectDir);
        }
        
        logInfo("Copying project to: " + targetProjectDir);
        FileUtils.copyDirectory(sourceProjectDir, targetProjectDir);
        
        // Import via M2E
        IJavaProject javaProject = importMavenProjectFromDirectory(targetProjectDir);
        
        importedProjects.add(projectName);
        logInfo("Project imported successfully: " + projectName);
        
        return javaProject;
    }

    /**
     * Imports a Maven project from the given directory.
     */
    private static IJavaProject importMavenProjectFromDirectory(File projectDir) throws Exception {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        
        // Use M2E to scan and import the project
        MavenModelManager modelManager = MavenPlugin.getMavenModelManager();
        IProjectConfigurationManager configManager = MavenPlugin.getProjectConfigurationManager();
        
        // Scan for Maven projects
        LocalProjectScanner scanner = new LocalProjectScanner(
            Collections.singletonList(projectDir.getAbsolutePath()),
            false, // not recursive into subdirectories
            modelManager
        );
        scanner.run(NULL_MONITOR);
        
        List<MavenProjectInfo> projectInfos = new ArrayList<>();
        collectProjects(scanner.getProjects(), projectInfos);
        
        if (projectInfos.isEmpty()) {
            throw new IllegalStateException(
                "No Maven projects found in: " + projectDir.getAbsolutePath());
        }
        
        logInfo("Found " + projectInfos.size() + " Maven project(s) to import");
        
        // Import the projects
        ProjectImportConfiguration importConfig = new ProjectImportConfiguration();
        List<org.eclipse.m2e.core.project.IMavenProjectImportResult> results = 
            configManager.importProjects(projectInfos, importConfig, NULL_MONITOR);
        
        // Find the main project
        IProject project = null;
        for (org.eclipse.m2e.core.project.IMavenProjectImportResult result : results) {
            if (result.getProject() != null) {
                project = result.getProject();
                logInfo("Imported: " + project.getName());
                break;
            }
        }
        
        if (project == null) {
            throw new IllegalStateException("Failed to import Maven project");
        }
        
        return JavaCore.create(project);
    }

    /**
     * Recursively collects all MavenProjectInfo from the scanner results.
     */
    private static void collectProjects(
            Collection<MavenProjectInfo> projects, 
            List<MavenProjectInfo> result) {
        for (MavenProjectInfo info : projects) {
            result.add(info);
            collectProjects(info.getProjects(), result);
        }
    }

    /**
     * Waits for all background jobs to complete, ensuring the project is fully ready.
     * This includes Maven configuration, dependency resolution, JDT build, and search indexing.
     * 
     * @param project The project to wait for
     * @throws InterruptedException if waiting is interrupted
     */
    public static void waitForProjectReady(IJavaProject project) throws InterruptedException {
        logInfo("Waiting for project to be ready: " + project.getElementName());
        
        // Configure workspace root paths for search to work
        configureWorkspaceRootPaths();
        
        // Wait for Maven jobs
        waitForMavenJobs();
        
        // Wait for build jobs
        waitForBuildJobs();
        
        // Wait for JDT indexing
        waitForSearchIndex();
        
        logInfo("Project ready: " + project.getElementName());
    }
    
    /**
     * Configures the JavaLanguageServerPlugin preferences with workspace root paths.
     * This is required for SampleDelegateCommandHandler.search() to work correctly.
     */
    private static void configureWorkspaceRootPaths() {
        try {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            org.eclipse.core.runtime.IPath workspacePath = root.getLocation();
            
            if (workspacePath == null) {
                logInfo("Warning: Workspace location is null, using default");
                return;
            }
            
            logInfo("Configuring workspace root path: " + workspacePath.toOSString());
            
            PreferenceManager prefManager = JavaLanguageServerPlugin.getPreferencesManager();
            if (prefManager != null) {
                Preferences prefs = prefManager.getPreferences();
                if (prefs != null) {
                    // Set the root paths to include the workspace
                    List<org.eclipse.core.runtime.IPath> rootPaths = new ArrayList<>();
                    rootPaths.add(workspacePath);
                    prefs.setRootPaths(rootPaths);
                    logInfo("Workspace root paths configured successfully");
                } else {
                    logInfo("Warning: Preferences is null");
                }
            } else {
                logInfo("Warning: PreferenceManager is null");
            }
        } catch (Exception e) {
            logInfo("Warning: Failed to configure workspace root paths: " + e.getMessage());
        }
    }

    /**
     * Waits for Maven-related jobs to complete.
     */
    private static void waitForMavenJobs() throws InterruptedException {
        logInfo("Waiting for Maven jobs...");
        
        // Wait for jobs belonging to M2E
        Job.getJobManager().join("org.eclipse.m2e", NULL_MONITOR);
        
        // Additional wait for any straggler jobs
        waitForJobsMatching(job -> 
            job.getClass().getName().contains("maven") ||
            job.getClass().getName().contains("Maven") ||
            job.getClass().getName().contains("m2e"));
        
        logInfo("Maven jobs completed");
    }

    /**
     * Waits for workspace build jobs to complete.
     */
    private static void waitForBuildJobs() throws InterruptedException {
        logInfo("Waiting for build jobs...");
        
        // Wait for auto-build
        Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, NULL_MONITOR);
        
        // Wait for manual builds
        Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, NULL_MONITOR);
        
        logInfo("Build jobs completed");
    }

    /**
     * Waits for JDT search index to be up-to-date.
     */
    public static void waitForSearchIndex() throws InterruptedException {
        logInfo("Waiting for JDT search index...");
        
        // Wait for indexing jobs
        waitForJobsMatching(job -> 
            job.getClass().getName().contains("Index") ||
            job.getClass().getName().contains("Reconcile"));
        
        // Give the indexer a moment to settle
        Thread.sleep(500);
        
        logInfo("Search index ready");
    }

    /**
     * Waits for jobs matching the given predicate.
     */
    private static void waitForJobsMatching(java.util.function.Predicate<Job> matcher) 
            throws InterruptedException {
        int maxWaitMs = 60000; // 60 seconds max
        int pollingMs = 100;
        long deadline = System.currentTimeMillis() + maxWaitMs;
        
        while (System.currentTimeMillis() < deadline) {
            Job[] jobs = Job.getJobManager().find(null);
            boolean foundMatch = false;
            
            for (Job job : jobs) {
                if (matcher.test(job) && job.getState() != Job.NONE) {
                    foundMatch = true;
                    break;
                }
            }
            
            if (!foundMatch) {
                return; // No matching jobs running
            }
            
            Thread.sleep(pollingMs);
        }
        
        logInfo("Warning: Timeout waiting for jobs");
    }

    /**
     * Deletes a project from the workspace and cleans up its working directory.
     * 
     * @param project The project to delete
     * @throws CoreException if deletion fails
     */
    public static void deleteProject(IJavaProject project) throws CoreException {
        if (project == null) {
            return;
        }
        
        String projectName = project.getElementName();
        logInfo("Deleting project: " + projectName);
        
        IProject iProject = project.getProject();
        if (iProject.exists()) {
            // Delete project and contents
            iProject.delete(true, true, NULL_MONITOR);
        }
        
        // Clean up working directory
        try {
            File workingDir = getWorkingDirectory();
            File projectDir = new File(workingDir, projectName);
            if (projectDir.exists()) {
                FileUtils.deleteDirectory(projectDir);
            }
        } catch (IOException e) {
            logInfo("Warning: Failed to clean working directory: " + e.getMessage());
        }
        
        importedProjects.remove(projectName);
        logInfo("Project deleted: " + projectName);
    }

    /**
     * Cleans up all test projects that were imported during this test run.
     */
    public static void cleanupAllTestProjects() {
        logInfo("Cleaning up all test projects");
        
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        
        for (String projectName : new ArrayList<>(importedProjects)) {
            try {
                IProject project = root.getProject(projectName);
                if (project.exists()) {
                    project.delete(true, true, NULL_MONITOR);
                }
            } catch (CoreException e) {
                logInfo("Warning: Failed to delete project " + projectName + ": " + e.getMessage());
            }
        }
        
        // Clean working directory
        try {
            File workingDir = getWorkingDirectory();
            if (workingDir.exists()) {
                FileUtils.deleteDirectory(workingDir);
            }
        } catch (IOException e) {
            logInfo("Warning: Failed to clean working directory: " + e.getMessage());
        }
        
        importedProjects.clear();
        logInfo("Cleanup completed");
    }

    /**
     * Resolves the path to a test project in the projects/ folder.
     * Handles both IDE and Maven test execution contexts.
     * 
     * @param projectName Name of the project folder
     * @return File pointing to the project directory
     */
    public static File resolveTestProjectPath(String projectName) throws IOException {
        // Try to resolve from the test bundle
        Bundle bundle = Platform.getBundle(TEST_BUNDLE_ID);
        if (bundle != null) {
            URL projectsUrl = bundle.getEntry(PROJECTS_FOLDER);
            if (projectsUrl != null) {
                URL resolvedUrl = FileLocator.toFileURL(projectsUrl);
                File projectsDir = new File(resolvedUrl.getPath());
                return new File(projectsDir, projectName);
            }
        }
        
        // Fallback: Try relative path (for IDE execution)
        File projectsDir = new File(PROJECTS_FOLDER);
        if (projectsDir.exists()) {
            return new File(projectsDir, projectName);
        }
        
        // Another fallback: Try from current working directory
        String cwd = System.getProperty("user.dir");
        File cwdProjectsDir = new File(cwd, PROJECTS_FOLDER);
        if (cwdProjectsDir.exists()) {
            return new File(cwdProjectsDir, projectName);
        }
        
        throw new IOException("Cannot resolve test projects directory. " +
            "Tried bundle entry, relative path, and CWD: " + cwd);
    }

    /**
     * Gets the root directory containing all test projects.
     */
    public static File getTestProjectsRoot() throws IOException {
        Bundle bundle = Platform.getBundle(TEST_BUNDLE_ID);
        if (bundle != null) {
            URL projectsUrl = bundle.getEntry(PROJECTS_FOLDER);
            if (projectsUrl != null) {
                URL resolvedUrl = FileLocator.toFileURL(projectsUrl);
                return new File(resolvedUrl.getPath());
            }
        }
        
        // Fallback
        return new File(PROJECTS_FOLDER);
    }

    /**
     * Gets the working directory for test project copies.
     */
    private static File getWorkingDirectory() throws IOException {
        File workingDir = new File(WORKING_DIR);
        FileUtils.forceMkdir(workingDir);
        return workingDir;
    }

    /**
     * Gets the project by name from the workspace.
     */
    public static IJavaProject getProject(String projectName) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(projectName);
        if (project.exists()) {
            return JavaCore.create(project);
        }
        return null;
    }

    /**
     * Checks if a project exists in the workspace.
     */
    public static boolean projectExists(String projectName) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        return root.getProject(projectName).exists();
    }

    private static void logInfo(String message) {
        System.out.println("[TestProjectManager] " + message);
        try {
            JavaLanguageServerPlugin.logInfo("[TestProjectManager] " + message);
        } catch (Exception e) {
            // Ignore if JavaLanguageServerPlugin is not available
        }
    }
}
