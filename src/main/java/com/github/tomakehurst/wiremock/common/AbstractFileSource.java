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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

import com.github.tomakehurst.wiremock.security.NotAuthorisedException;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public abstract class AbstractFileSource implements FileSource {

  protected final File rootDirectory;

  public AbstractFileSource(File rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  protected abstract boolean readOnly();

  @Override
  public BinaryFile getBinaryFileNamed(final String name) {
    assertFilePathIsUnderRoot(name);
    return new BinaryFile(new File(rootDirectory, name).toURI());
  }

  @Override
  public TextFile getTextFileNamed(String name) {
    assertFilePathIsUnderRoot(name);
    return new TextFile(new File(rootDirectory, name).toURI());
  }

  @Override
  public void createIfNecessary() {
    assertWritable();
    if (rootDirectory.exists() && rootDirectory.isFile()) {
      throw new IllegalStateException(rootDirectory + " already exists and is a file");
    } else if (!rootDirectory.exists()) {
      rootDirectory.mkdirs();
    }
  }

  @Override
  public String getPath() {
    return rootDirectory.getPath();
  }

  @Override
  public URI getUri() {
    return rootDirectory.toURI();
  }

  @Override
  public List<TextFile> listFilesRecursively() {
    assertExistsAndIsDirectory();
    List<File> fileList = newArrayList();
    recursivelyAddFilesToList(rootDirectory, fileList);
    return toTextFileList(fileList);
  }

  private void recursivelyAddFilesToList(File root, List<File> fileList) {
    File[] files = root.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        recursivelyAddFilesToList(file, fileList);
      } else {
        fileList.add(file);
      }
    }
  }

  private List<TextFile> toTextFileList(List<File> fileList) {
    return newArrayList(
        transform(
            fileList,
            new Function<File, TextFile>() {
              public TextFile apply(File input) {
                return new TextFile(input.toURI());
              }
            }));
  }

  @Override
  public void writeTextFile(String name, String contents) {
    writeTextFileAndTranslateExceptions(contents, writableFileFor(name));
  }

  @Override
  public void writeBinaryFile(String name, byte[] contents) {
    writeBinaryFileAndTranslateExceptions(contents, writableFileFor(name));
  }

  @Override
  public void deleteFile(String name) {
    writableFileFor(name).delete();
  }

  @Override
  public boolean exists() {
    return rootDirectory.exists();
  }

  private File writableFileFor(String name) {
    assertExistsAndIsDirectory();
    assertFilePathIsUnderRoot(name);
    assertWritable();
    final File filePath = new File(name);

    if (filePath.isAbsolute()) {
      return filePath;
    } else {
      // Convert to absolute path
      return new File(rootDirectory, name);
    }
  }

  private void assertExistsAndIsDirectory() {
    if (rootDirectory.exists() && !rootDirectory.isDirectory()) {
      throw new RuntimeException(rootDirectory + " is not a directory");
    } else if (!rootDirectory.exists()) {
      throw new RuntimeException(rootDirectory + " does not exist");
    }
  }

  private void assertWritable() {
    if (readOnly()) {
      throw new UnsupportedOperationException("Can't write to read only file sources");
    }
  }

  private void assertFilePathIsUnderRoot(String path) {
    try {
      String rootPath = rootDirectory.getCanonicalPath();

      File file = new File(path);
      String filePath =
          file.isAbsolute()
              ? new File(path).getCanonicalPath()
              : new File(rootDirectory, path).getCanonicalPath();

      if (!filePath.startsWith(rootPath)) {
        throw new NotAuthorisedException("Access to file " + path + " is not permitted");
      }
    } catch (IOException ioe) {
      throw new NotAuthorisedException("File " + path + " cannot be accessed", ioe);
    }
  }

  private void writeTextFileAndTranslateExceptions(String contents, File toFile) {
    try {
      Files.write(contents, toFile, UTF_8);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private void writeBinaryFileAndTranslateExceptions(byte[] contents, File toFile) {
    try {
      Files.write(contents, toFile);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private FileFilter filesOnly() {
    return new FileFilter() {
      public boolean accept(File file) {
        return file.isFile();
      }
    };
  }

  public static Predicate<BinaryFile> byFileExtension(final String extension) {
    return new Predicate<BinaryFile>() {
      public boolean apply(BinaryFile input) {
        return input.name().endsWith("." + extension);
      }
    };
  }
}
