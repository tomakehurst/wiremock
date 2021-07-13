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

import static com.github.tomakehurst.wiremock.testsupport.TestFiles.filePath;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.fileNamed;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.hasExactlyIgnoringOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.security.NotAuthorisedException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class SingleRootFileSourceTest {

  public static final String ROOT_PATH = filePath("filesource");

  @SuppressWarnings("unchecked")
  @Test
  public void listsTextFilesRecursively() {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);

    List<TextFile> files = fileSource.listFilesRecursively();

    assertThat(
        files,
        hasExactlyIgnoringOrder(
            fileNamed("one"),
            fileNamed("two"),
            fileNamed("three"),
            fileNamed("four"),
            fileNamed("five"),
            fileNamed("six"),
            fileNamed("seven"),
            fileNamed("eight"),
            fileNamed("deepfile.json")));
  }

  @Test
  public void writesTextFileEvenWhenRootIsARelativePath() throws IOException {
    String relativeRootPath = "./target/tmp/";
    FileUtils.forceMkdir(new File(relativeRootPath));
    SingleRootFileSource fileSource = new SingleRootFileSource(relativeRootPath);
    Path fileAbsolutePath = Paths.get(relativeRootPath).toAbsolutePath().resolve("myFile");
    fileSource.writeTextFile(fileAbsolutePath.toString(), "stuff");

    assertThat(Files.exists(fileAbsolutePath), is(true));
  }

  @Test(expected = RuntimeException.class)
  public void listFilesRecursivelyThrowsExceptionWhenRootIsNotDir() {
    SingleRootFileSource fileSource = new SingleRootFileSource("src/test/resources/filesource/one");
    fileSource.listFilesRecursively();
  }

  @Test(expected = RuntimeException.class)
  public void writeThrowsExceptionWhenRootIsNotDir() {
    SingleRootFileSource fileSource = new SingleRootFileSource("src/test/resources/filesource/one");
    fileSource.writeTextFile("thing", "stuff");
  }

  @Test(expected = NotAuthorisedException.class)
  public void writeTextFileThrowsExceptionWhenGivenRelativePathNotUnderRoot() {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    fileSource.writeTextFile("..", "stuff");
  }

  @Test(expected = NotAuthorisedException.class)
  public void writeTextFileThrowsExceptionWhenGivenAbsolutePathNotUnderRoot() {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    String badPath = Paths.get("..", "not-under-root").toAbsolutePath().toString();
    fileSource.writeTextFile(badPath, "stuff");
  }

  @Test(expected = NotAuthorisedException.class)
  public void writeBinaryFileThrowsExceptionWhenGivenRelativePathNotUnderRoot() {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    fileSource.writeBinaryFile("..", "stuff".getBytes());
  }

  @Test(expected = NotAuthorisedException.class)
  public void writeBinaryFileThrowsExceptionWhenGivenAbsolutePathNotUnderRoot() {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    String badPath = Paths.get("..", "not-under-root").toAbsolutePath().toString();
    fileSource.writeBinaryFile(badPath, "stuff".getBytes());
  }

  @Test(expected = NotAuthorisedException.class)
  public void deleteThrowsExceptionWhenGivenPathNotUnderRoot() {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    String badPath = Paths.get("..", "not-under-root").toAbsolutePath().toString();
    fileSource.deleteFile(badPath);
  }

  @Test(expected = NotAuthorisedException.class)
  public void readBinaryFileThrowsExceptionWhenRelativePathIsOutsideRoot() {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    fileSource.getBinaryFileNamed("../illegal.file");
  }

  @Test(expected = NotAuthorisedException.class)
  public void readTextFileThrowsExceptionWhenRelativePathIsOutsideRoot() {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    fileSource.getTextFileNamed("../illegal.file");
  }

  @Test(expected = NotAuthorisedException.class)
  public void readBinaryFileThrowsExceptionWhenAbsolutePathIsOutsideRoot() throws Exception {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    String badPath = new File(ROOT_PATH, "../illegal.file").getCanonicalPath();
    fileSource.getBinaryFileNamed(badPath);
  }

  @Test(expected = NotAuthorisedException.class)
  public void readTextFileThrowsExceptionWhenAbsolutePathIsOutsideRoot() throws Exception {
    SingleRootFileSource fileSource = new SingleRootFileSource(ROOT_PATH);
    String badPath = new File(ROOT_PATH, "../illegal.file").getCanonicalPath();
    fileSource.getTextFileNamed(badPath);
  }
}
