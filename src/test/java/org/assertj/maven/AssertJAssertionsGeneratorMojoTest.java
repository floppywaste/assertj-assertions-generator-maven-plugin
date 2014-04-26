package org.assertj.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.util.Arrays.array;
import static org.assertj.core.util.Lists.newArrayList;
import static org.assertj.maven.AssertJAssertionsGeneratorMojo.shouldHaveNonEmptyPackagesOrClasses;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import org.assertj.assertions.generator.BaseAssertionGenerator;
import org.assertj.assertions.generator.description.ClassDescription;
import org.assertj.maven.generator.AssertionsGenerator;
import org.assertj.maven.test.Employee;
import org.assertj.maven.test2.adress.Address;

public class AssertJAssertionsGeneratorMojoTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private AssertJAssertionsGeneratorMojo assertjAssertionsGeneratorMojo;
  private MavenProject mavenProject;

  @Before
  public void setUp() throws Exception {
    mavenProject = mock(MavenProject.class);
    assertjAssertionsGeneratorMojo = new AssertJAssertionsGeneratorMojo();
    assertjAssertionsGeneratorMojo.project = mavenProject;
    assertjAssertionsGeneratorMojo.targetDir = temporaryFolder.getRoot().getAbsolutePath();
  }

  @Test
  public void executing_plugin_with_classes_and_packages_parameter_only_should_pass() throws Exception {
    assertjAssertionsGeneratorMojo.packages = array("org.assertj.maven.test", "org.assertj.maven.test2");
    assertjAssertionsGeneratorMojo.classes = array("org.assertj.maven.test.Employee");
    List<String> classes = newArrayList(Employee.class.getName(), Address.class.getName());
    when(mavenProject.getRuntimeClasspathElements()).thenReturn(classes);

    assertjAssertionsGeneratorMojo.execute();

    // check that expected assertions file exist (we don't check the content we suppose the generator works).
    assertThat(assertionsFileFor(Employee.class)).exists();
    assertThat(assertionsFileFor(Address.class)).exists();
    assertThat(assertionsEntryPointFile()).exists();
  }

  @Test
  public void executing_plugin_with_classes_parameter_only_should_pass() throws Exception {
    assertjAssertionsGeneratorMojo.classes = array("org.assertj.maven.test.Employee", "org.assertj.maven.test2.adress.Address");
    List<String> classes = newArrayList(Employee.class.getName(), Address.class.getName());
    when(mavenProject.getRuntimeClasspathElements()).thenReturn(classes);

    assertjAssertionsGeneratorMojo.execute();

    // check that expected assertions file exist (we don't check the content we suppose the generator works).
    assertThat(assertionsFileFor(Employee.class)).exists();
    assertThat(assertionsEntryPointFile()).exists();
  }
  
  @Test
  public void executing_plugin_with_custom_entry_point_class_package_should_pass() throws Exception {
    assertjAssertionsGeneratorMojo.classes = array("org.assertj.maven.test.Employee");
    assertjAssertionsGeneratorMojo.entryPointClassPackage = "my.custom.pkg";
    List<String> classes = newArrayList(Employee.class.getName(), Address.class.getName());
    when(mavenProject.getRuntimeClasspathElements()).thenReturn(classes);

    assertjAssertionsGeneratorMojo.execute();

    assertThat(assertionsFileFor(Employee.class)).exists();
    assertThat(assertionsEntryPointFileWithCustomPackage()).exists();
  }

  @Test
  public void executing_plugin_with_fake_package_should_not_generated_anything() throws Exception {
    assertjAssertionsGeneratorMojo.packages = array("fakepackage");
    List<String> classes = newArrayList();
    when(mavenProject.getRuntimeClasspathElements()).thenReturn(classes);

    assertjAssertionsGeneratorMojo.execute();

    assertThat(temporaryFolder.getRoot().list()).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void executing_plugin_with_error_should_be_reported_in_generator_report() throws Exception {
    assertjAssertionsGeneratorMojo.classes = array("org.assertj.maven.test.Employee");
    when(mavenProject.getRuntimeClasspathElements()).thenReturn(newArrayList(Employee.class.getName()));
    // let's throws an IOException when generating custom assertions
    AssertionsGenerator generator = new AssertionsGenerator(null);
    BaseAssertionGenerator baseGenerator = mock(BaseAssertionGenerator.class);
    generator.setBaseGenerator(baseGenerator);
    when(baseGenerator.generateCustomAssertionFor(Mockito.any(ClassDescription.class))).thenThrow(IOException.class);
    assertjAssertionsGeneratorMojo.executeWithAssertionGenerator(generator);

    assertThat(temporaryFolder.getRoot().list()).isEmpty();
  }

  @Test
  public void should_fail_if_packages_and_classes_parameters_are_null() throws Exception {
    try {
      assertjAssertionsGeneratorMojo.execute();
      failBecauseExceptionWasNotThrown(MojoFailureException.class);
    } catch (MojoFailureException e) {
      assertThat(e).hasMessage(shouldHaveNonEmptyPackagesOrClasses());
    }
  }

  private File assertionsFileFor(Class<?> clazz) throws IOException {
    return temporaryFolder.newFile(basePathName(clazz) + "Assert.java");
  }

  private static String basePathName(Class<?> clazz) {
    return clazz.getPackage().getName().replace('.', File.separatorChar) + File.separator + clazz.getSimpleName();
  }

  private File assertionsEntryPointFile() throws IOException {
    return temporaryFolder.newFile("org.assertj.maven.test".replace('.', File.separatorChar) + File.separator
        + "Assertions.java");
  }

  private File assertionsEntryPointFileWithCustomPackage() throws IOException {
    return temporaryFolder.newFile("my.custom.pkg".replace('.', File.separatorChar) + File.separator
        + "Assertions.java");
  }
}
