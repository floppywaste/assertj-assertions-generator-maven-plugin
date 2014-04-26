package org.assertj.maven.generator;

import static org.assertj.assertions.generator.util.ClassUtil.collectClasses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.assertj.assertions.generator.BaseAssertionGenerator;
import org.assertj.assertions.generator.description.ClassDescription;
import org.assertj.assertions.generator.description.converter.ClassToClassDescriptionConverter;
import org.assertj.core.util.VisibleForTesting;

import com.google.common.base.Strings;

/**
 * Is able to generate AssertJ assertions classes from packages.
 */
public class AssertionsGenerator {

  private ClassToClassDescriptionConverter converter;
  private ClassLoader classLoader;
  private BaseAssertionGenerator generator;

  public AssertionsGenerator(ClassLoader classLoader) throws FileNotFoundException, IOException {
    this.generator = new BaseAssertionGenerator();
    this.converter = new ClassToClassDescriptionConverter();
    this.classLoader = classLoader;
  }

  /**
   * Generates custom assertions for classes in given packages with the Assertions class entry point in given
   * destination dir.
   * 
   * @param packages the packages containing the classes we want to generate Assert classes for.
   * @param classes the packages containing the classes we want to generate Assert classes for.
   * @param destDir the base directory where the classes are going to be generated.
   * @param entryPointFilePackage the package of the assertions entry point class, may be <code>null</code>.
   * @throws IOException if the files can't be generated
   */
  public AssertionsGeneratorReport generateAssertionsFor(String[] packages, String[] classes, String destDir,
      String entryPointFilePackage) {
    generator.setDirectoryWhereAssertionFilesAreGenerated(destDir);
    Set<ClassDescription> classDescriptions = new HashSet<ClassDescription>();
    AssertionsGeneratorReport report = new AssertionsGeneratorReport();
    try {
      for (Class<?> clazz : collectClasses(classLoader, ArrayUtils.addAll(packages, classes))) {
        ClassDescription classDescription = converter.convertToClassDescription(clazz);
        File generatedCustomAssertionFile = generator.generateCustomAssertionFor(classDescription);
        report.addGeneratedAssertionFile(generatedCustomAssertionFile);
        classDescriptions.add(classDescription);
      }
      report.setInputPackages(packages);
      report.setInputClasses(classes);
      report.setDirectoryPathWhereAssertionFilesAreGenerated(destDir);
      File standardAssertionsEntryPointFile = Strings.isNullOrEmpty(entryPointFilePackage) ? generator
          .generateAssertionsEntryPointFor(classDescriptions) : generator.generateAssertionsEntryPointFor(
          classDescriptions, entryPointFilePackage);
      report.setAssertionsEntryPointFile(standardAssertionsEntryPointFile);
    } catch (Exception e) {
      report.setException(e);
    }
    return report;
  }

  @VisibleForTesting
  public void setBaseGenerator(BaseAssertionGenerator generator) {
    this.generator = generator;
  }

}
