package com.github.tomakehurst.wiremock.common;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class TextFileTest {
    @Test
    public void returnsPathToFileOnLinuxSystems() throws Exception {
        TextFile textFile = new TextFile(new URI("file://home/bob/myfile.txt"));

        String path = textFile.getPath();

        assertEquals("/home/bob/myfile.txt", path);
    }

    @Test
    public void returnsPathToFileOnWindowsSystems() throws Exception {
        assumeTrue("This test can only be run on Windows " +
                "because File uses FileSystem in its constructor " +
                "and its behaviour is OS specific", SystemUtils.IS_OS_WINDOWS);

        TextFile textFile = new TextFile(new URI("file:/C:/Users/bob/myfile.txt"));

        String path = textFile.getPath();

        assertEquals("C:/Users/bob/myfile.txt", path);
    }
}