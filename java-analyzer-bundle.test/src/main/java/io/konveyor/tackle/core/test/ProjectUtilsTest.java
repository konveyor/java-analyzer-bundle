package io.konveyor.tackle.core.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.eclipse.buildship.core.internal.CorePlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.lsp4j.ClientCapabilities;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.konveyor.tackle.core.internal.SampleDelegateCommandHandler;

/**
 * Base Utility class to load a project for testing
 *
 * @author Charles Moulliard
 *
 */
public class ProjectUtilsTest {

	private static final Logger LOGGER = Logger.getLogger(ProjectUtilsTest.class.getSimpleName());
	private static Level oldLevel;

	public final String RULE_ENTRY_COMMAND_ID = "io.konveyor.tackle.ruleEntry";
	public final String ANALYSIS_MODE_SOURCE_ONLY = "source-only";
	public final String LOCATION_TYPE_ANNOTATION = "4";

	public static class MavenProjectName {
		public static String empty_maven_project = "empty-maven";
		public static String springboot_todo_project = "springboot-todo";
	}

	public static class GradleProjectName {
		public static String empty_gradle_project = "empty-gradle-project";
		public static String quarkus_gradle_project = "quarkus-gradle-project";
	}

	public static SampleDelegateCommandHandler cmdHandler;

	@BeforeClass
	public static void setUp() {
		oldLevel = LOGGER.getLevel();
		LOGGER.setLevel(Level.INFO);
		enableClassFileContentsSupport();

		cmdHandler = new SampleDelegateCommandHandler();
	}

	@AfterClass
	public static void tearDown() {
		LOGGER.setLevel(oldLevel);
	}

	@After
	public void cleanWorkspace() {
		try {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				project.delete(true, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static IJavaProject loadMavenProject(String mavenProject) throws CoreException, Exception {
		return loadJavaProjects(new String[] { "maven/" + mavenProject })[0];
	}

	public static IJavaProject loadGradleProject(String gradleProject) throws CoreException, Exception {
		var gradleJavaProject = loadJavaProjects(new String[] { "gradle/" + gradleProject })[0];
		Job.getJobManager().join(CorePlugin.GRADLE_JOB_FAMILY, new NullProgressMonitor());
		return gradleJavaProject;
	}

	public static IJavaProject loadMavenProjectFromSubFolder(String mavenProject, String subFolder) throws Exception {
		return loadJavaProjects(new String[] { "maven/" + subFolder + "/" + mavenProject })[0];
	}

	public static IJavaProject[] loadJavaProjects(String[] parentSlashName) {

		List<IPath> paths = new ArrayList<>();
		List<IJavaProject> javaProjects = new ArrayList<>();

		try {
			for (String parentSlashNameEntry : parentSlashName) {
				String parentDirName = parentSlashNameEntry.substring(0, parentSlashNameEntry.lastIndexOf('/'));
				String projectName = parentSlashNameEntry.substring(parentSlashNameEntry.lastIndexOf('/') + 1);

				// Move project to working directory
				File projectFolder = copyProjectToWorkingDirectory(projectName, parentDirName);
				IPath path = new Path(projectFolder.getAbsolutePath());
				paths.add(path);
			}

			JavaLanguageServerPlugin.getPreferencesManager().initialize();
			JavaLanguageServerPlugin.getPreferencesManager().updateClientPrefences(new ClientCapabilities(),
					new HashMap<>());
			JavaLanguageServerPlugin.getProjectsManager().initializeProjects(paths, new NullProgressMonitor());

			IProgressMonitor monitor = new NullProgressMonitor();
			waitForBackgroundJobs(monitor);
			org.eclipse.jdt.ls.core.internal.JobHelpers.waitUntilIndexesReady();

			for (IPath path : paths) {
				IProjectDescription description = ResourcesPlugin.getWorkspace()
						.loadProjectDescription(path.append(".project"));
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
				javaProjects.add(JavaCore.create(project));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Set the rootPaths manually. This is is needed when running the tests
		JavaLanguageServerPlugin.getPreferencesManager().getPreferences().setRootPaths(paths);

		return javaProjects.toArray(new IJavaProject[0]);
	}

	private static File copyProjectToWorkingDirectory(String projectName, String parentDirName) throws IOException {
		File from = new File("projects/" + parentDirName + "/" + projectName);
		File to = new File(JavaUtils.getWorkingProjectDirectory(),
				java.nio.file.Paths.get(parentDirName, projectName).toString());

		if (to.exists()) {
			FileUtils.forceDelete(to);
		}

		if (from.isDirectory()) {
			FileUtils.copyDirectory(from, to);
		} else {
			FileUtils.copyFile(from, to);
		}

		return to;
	}

	private static void waitForBackgroundJobs(IProgressMonitor monitor) throws Exception {
		JobHelpers.waitForJobsToComplete(monitor);
	}

	private static void createFile(IFile file, String contents) throws CoreException {
		createParentFolders(file);
		file.refreshLocal(IResource.DEPTH_ZERO, null);
		InputStream fileContents = new ByteArrayInputStream(contents.getBytes());
		if (file.exists()) {
			file.setContents(fileContents, IResource.NONE, null);
		} else {
			file.create(fileContents, true, null);
		}
	}

	private static void createParentFolders(final IResource resource) throws CoreException {
		if (resource == null || resource.exists())
			return;
		if (!resource.getParent().exists())
			createParentFolders(resource.getParent());
		switch (resource.getType()) {
			case IResource.FOLDER:
				((IFolder) resource).create(IResource.FORCE, true, new NullProgressMonitor());
				break;
			case IResource.PROJECT:
				((IProject) resource).create(new NullProgressMonitor());
				((IProject) resource).open(new NullProgressMonitor());
				break;
		}
	}

	private static void updateFile(IFile file, String content) throws CoreException {
		// For Mac OS, Linux OS, the call of Files.getLastModifiedTime is working for 1
		// second.
		// Here we wait for > 1s to be sure that call of Files.getLastModifiedTime will
		// work.
		try {
			Thread.sleep(1050);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		createFile(file, content);
	}

	protected static void saveFile(String configFileName, String content, IJavaProject javaProject)
			throws CoreException {
		saveFile(configFileName, content, javaProject, false);
	}

	protected static void saveFile(String configFileName, String content, IJavaProject javaProject, boolean inSource)
			throws CoreException {
		IFile file = getFile(configFileName, javaProject, inSource);
		updateFile(file, content);
	}

	protected static void deleteFile(String configFileName, IJavaProject javaProject)
			throws IOException, CoreException {
		deleteFile(configFileName, javaProject, false);
	}

	protected static void deleteFile(String configFileName, IJavaProject javaProject, boolean inSource)
			throws IOException, CoreException {
		IFile file = getFile(configFileName, javaProject, inSource);
		file.delete(true, new NullProgressMonitor());
	}

	private static IFile getFile(String configFileName, IJavaProject javaProject, boolean inSource)
			throws JavaModelException {
		if (inSource) {
			return javaProject.getProject().getFile(new Path("src/main/java/" + configFileName));
		}
		IPath output = javaProject.getOutputLocation();
		IPath filePath = output.append(configFileName);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
	}

	private static void enableClassFileContentsSupport() {
		Map<String, Object> extendedClientCapabilities = new HashMap<>();
		extendedClientCapabilities.put("classFileContentsSupport", "true");
		JavaLanguageServerPlugin.getPreferencesManager().updateClientPrefences(new ClientCapabilities(),
				extendedClientCapabilities);
	}
}
