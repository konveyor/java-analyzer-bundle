package io.konveyor.tackle.core.test;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.Test;

public class PomDependencyTest extends ProjectUtilsTest {
    @Test
    public void mavenProjectTest() throws Exception {
        IJavaProject javaProject = loadMavenProject(MavenProjectName.springboot_todo_project);
        IFile pomfile = javaProject.getProject().getFile(new Path("pom.xml"));
        
        assertEquals(String.format("/%s/pom.xml",MavenProjectName.springboot_todo_project), pomfile.getFullPath().toString());
    }
}