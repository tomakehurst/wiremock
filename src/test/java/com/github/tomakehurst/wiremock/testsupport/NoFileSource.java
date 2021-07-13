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
package com.github.tomakehurst.wiremock.testsupport;

import com.github.tomakehurst.wiremock.common.BinaryFile;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.TextFile;
import java.net.URI;
import java.util.List;

public class NoFileSource implements FileSource {

  public static NoFileSource noFileSource() {
    return new NoFileSource();
  }

  @Override
  public BinaryFile getBinaryFileNamed(String name) {
    return null;
  }

  @Override
  public TextFile getTextFileNamed(String name) {
    return null;
  }

  @Override
  public void createIfNecessary() {}

  @Override
  public FileSource child(String subDirectoryName) {
    return null;
  }

  @Override
  public String getPath() {
    return null;
  }

  @Override
  public URI getUri() {
    return null;
  }

  @Override
  public List<TextFile> listFilesRecursively() {
    return null;
  }

  @Override
  public void writeTextFile(String name, String contents) {}

  @Override
  public void writeBinaryFile(String name, byte[] contents) {}

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public void deleteFile(String name) {}
}
