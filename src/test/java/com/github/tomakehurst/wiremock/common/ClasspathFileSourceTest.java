/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.common;

import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.fileNamed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.Test;

public class ClasspathFileSourceTest {

  ClasspathFileSource classpathFileSource;

  @SuppressWarnings("unchecked")
  @Test
  public void listsFilesRecursivelyFromJar() {
    initForJar();

    List<TextFile> files = classpathFileSource.listFilesRecursively();

    assertThat(files, hasItems(fileNamed("pom.properties"), fileNamed("pom.xml")));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void listsFilesRecursivelyFromFileSystem() {
    initForFileSystem();

    List<TextFile> files = classpathFileSource.listFilesRecursively();

    assertThat(
        files,
        hasItems(
            fileNamed("one"),
            fileNamed("two"),
            fileNamed("three"),
            fileNamed("four"),
            fileNamed("five"),
            fileNamed("six")));
  }

  @Test
  public void readsBinaryFileFromJar() {
    initForJar();

    BinaryFile binaryFile = classpathFileSource.getBinaryFileNamed("guava/pom.xml");

    assertThat("Expected a non zero length file", binaryFile.readContents().length, greaterThan(0));
  }

  @Test
  public void readsBinaryFileFromZip() {
    classpathFileSource = new ClasspathFileSource("zippeddir");

    BinaryFile binaryFile = classpathFileSource.getBinaryFileNamed("zippedfile.txt");

    String contents = new String(binaryFile.readContents());
    assertThat(contents, containsString("zip"));
  }

  @Test
  public void readsBinaryFileFromZipWithoutMatch() {
    classpathFileSource = new ClasspathFileSource("zippeddir");
    try {
      classpathFileSource.getBinaryFileNamed("thisWillNotBeFound.txt");
      fail("Should have thrown exception.");
    } catch (Exception e) {
      assertThat(
          "Informative error",
          e.getMessage(),
          startsWith("Was unable to find entry: \"zippeddir/thisWillNotBeFound.txt\", found:"));
    }
  }

  @Test
  public void readsBinaryFileFromFileSystem() {
    initForFileSystem();

    BinaryFile binaryFile = classpathFileSource.getBinaryFileNamed("subdir/deepfile.json");

    assertThat("Expected a non zero length file", binaryFile.readContents().length, greaterThan(0));
  }

  @Test
  public void createsChildSource() {
    initForFileSystem();

    FileSource child = classpathFileSource.child("subdir");

    assertThat(child.getPath(), is("filesource/subdir"));
  }

  @Test
  public void correctlyReportsExistence() {
    assertTrue("Expected to exist", new ClasspathFileSource("filesource/subdir").exists());
    assertTrue(
        "Expected to exist", new ClasspathFileSource("META-INF/maven/com.google.guava").exists());
    assertFalse("Expected not to exist", new ClasspathFileSource("not/exist").exists());
  }

  @Test
  public void failsSilentlyOnWrites() {
    initForFileSystem();
    classpathFileSource.deleteFile("one");
    classpathFileSource.writeBinaryFile("any-bytes", new byte[] {});
    classpathFileSource.writeTextFile("any-text", "things");
    classpathFileSource.createIfNecessary();
  }

  void initForJar() {
    classpathFileSource = new ClasspathFileSource("META-INF/maven/com.google.guava");
  }

  private void initForFileSystem() {
    classpathFileSource = new ClasspathFileSource("filesource");
  }
}
