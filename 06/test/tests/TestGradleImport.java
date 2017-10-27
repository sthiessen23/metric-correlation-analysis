package tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.gravity.eclipse.importer.GradleImport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestGradleImport {
	
	private static final String gradleLinux = "";

	@Parameters(name="{0}") 
	public static Collection<String[]> collectParameters(){
		List<String[]> projects = new LinkedList<>();
		for(File file : new File("repositories").listFiles()) {
			if(file.isDirectory()) {
				projects.add(new String[] {file.getName(), file.getAbsolutePath()});
			}
		}
		return projects;
	}

	private File location;
	
	public TestGradleImport(String name, String location) {
		this.location = new File(location);
	}
	
	@Test
	public void testImportGradleProject() throws Exception {
		GradleImport importer = null;
		
		String os = System.getProperty("os.name");
		if(os.toLowerCase().indexOf("windows") >= 0) {
			importer = new GradleImport("", "");
		}
		else if(os.toLowerCase().indexOf("linux") >= 0) {
			importer = new GradleImport("/home/speldszus/.gradle", "/home/speldszus/Android/Sdk");
		}
		
		if(importer == null) {
			fail("Couldn't create importer");
		}
		
		IJavaProject project = importer.importGradleProject(location, location.getName(), new NullProgressMonitor());
		
		if(project == null || !project.exists()) {
			fail();
		}
	}

}
