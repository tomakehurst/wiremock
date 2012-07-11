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
package com.github.tomakehurst.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.any;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.write;
import static java.io.File.separator;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.io.Files;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

public class StandaloneAcceptanceTest {
	
	private static final String FILES = "__files";
	private static final String MAPPINGS = "mappings";

    private static final byte[] binaryCompressedContent = new byte []{31, -117, 8, 8, 72, -53, -8, 79, 0, 3, 103, 122, 105, 112, 100, 97, 116, 97, 45, 111, 117, 116, 0, -77, 41, 74, 45, 46, -56, -49, 43, 78, -75, -53, 72, -51, -55, -55, -73, -47, -121, -13, 1, 9, 69, -3, 52, 26, 0, 0, 0};
    private static final String binaryCompressedJsonString = printBase64Binary(binaryCompressedContent);
    private static final String binaryCompressedContentAsString = "<response>hello</response>";

    private static final File FILE_SOURCE_ROOT = new File("build/standalone-files");
	
	private WireMockServerRunner runner;
	private WireMockTestClient testClient;
	
	private WireMockServer otherServer;
	
	private final PrintStream stdOut = System.out;
	private ByteArrayOutputStream out;
	
	private File mappingsDirectory;
	
	@Before
	public void init() {
		if (FILE_SOURCE_ROOT.exists()) {
			FILE_SOURCE_ROOT.delete();
		}
		
		FILE_SOURCE_ROOT.mkdirs();
		
		mappingsDirectory = new File(FILE_SOURCE_ROOT, MAPPINGS);
		
		runner = new WireMockServerRunner();
		testClient = new WireMockTestClient();
		
		WireMock.configure();
	}
	
	@After
	public void stopServerRunner() {
		runner.stop();
		if (otherServer != null) {
			otherServer.stop();
		}
		System.setOut(stdOut);
	}

	@Test
	public void acceptsMappingRequestOnDefaultPort() throws Exception {
		startRunner();
		givenThat(get(urlEqualTo("/standalone/test/resource")).willReturn(aResponse().withStatus(200).withBody("Content")));
		assertThat(testClient.get("/standalone/test/resource").content(), is("Content"));
	}

    private static final String MAPPING_REQUEST_FOR_BYTE_BODY =
            "{ 													\n" +
                    "	\"request\": {									\n" +
                    "		\"method\": \"GET\",						\n" +
                    "		\"url\": \"/byte/resource/from/file\"			\n" +
                    "	},												\n" +
                    "	\"response\": {									\n" +
                    "		\"status\": 200,							\n" +
                    "		\"base64Body\": \""+printBase64Binary(new byte[]{65,66,67}) +"\"		\n" +
                    "	}												\n" +
                    "}													";

    @Test
    public void readsMapppingForByteBody() {
        writeMappingFile("test-mapping-bytearray-1.json", MAPPING_REQUEST_FOR_BYTE_BODY);
        startRunner();
        assertThat(testClient.get("/byte/resource/from/file").content(), is("ABC"));
    }

    private static final String MAPPING_REQUEST_FOR_BINARY_BYTE_BODY =
            "{ 													\n" +
                    "	\"request\": {									\n" +
                    "		\"method\": \"GET\",						\n" +
                    "		\"url\": \"/bytecompressed/resource/from/file\"			\n" +
                    "	},												\n" +
                    "	\"response\": {									\n" +
                    "		\"status\": 200,							\n" +
                    "		\"base64Body\": \""+binaryCompressedJsonString+"\"		\n" +
                    "	}												\n" +
                    "}													";


    @Test
    public void readsMapppingForByteBodyReturnsByteArray() {
        writeMappingFile("test-mapping-bytearray-2.json", MAPPING_REQUEST_FOR_BINARY_BYTE_BODY);
        startRunner();
        assertThat(testClient.get("/bytecompressed/resource/from/file").binaryContent(), is(binaryCompressedContent));
    }
	
	private static final String MAPPING_REQUEST =
		"{ 													\n" +
		"	\"request\": {									\n" +
		"		\"method\": \"GET\",						\n" +
		"		\"url\": \"/resource/from/file\"			\n" +
		"	},												\n" +
		"	\"response\": {									\n" +
		"		\"status\": 200,							\n" +
		"		\"body\": \"Body from mapping file\"		\n" +
		"	}												\n" +
		"}													";
	
	@Test
	public void readsMapppingFromMappingsDir() {
		writeMappingFile("test-mapping-1.json", MAPPING_REQUEST);
		startRunner();
		assertThat(testClient.get("/resource/from/file").content(), is("Body from mapping file"));
	}
	
	@Test
	public void servesFileFromFilesDir() {
		writeFileToFilesDir("test-1.xml", "<content>Blah</content>");
		startRunner();
		WireMockResponse response = testClient.get("/test-1.xml");
		assertThat(response.statusCode(), is(200));
		assertThat(response.content(), is("<content>Blah</content>"));
		assertThat(response.header("Content-Type"), is("application/xml"));
	}
	
	@Test
	public void servesFileAsJsonWhenNoFileExtension() {
		writeFileToFilesDir("json/12345", "{ \"key\": \"value\" }");
		startRunner();
		WireMockResponse response = testClient.get("/json/12345");
		assertThat(response.statusCode(), is(200));
		assertThat(response.content(), is("{ \"key\": \"value\" }"));
		assertThat(response.header("Content-Type"), is("application/json"));
	}
	
	@Test
	public void shouldNotSend302WhenPathIsDirAndTrailingSlashNotPresent() {
	    writeFileToFilesDir("json/wire & mock directory/index.json", "{ \"key\": \"index page value\" }");
	    startRunner();
        WireMockResponse response = testClient.get("/json/wire%20&%20mock%20directory");
        assertThat(response.statusCode(), is(200));
        assertThat(response.content(), is("{ \"key\": \"index page value\" }"));
	}
	
	@Test
	public void servesJsonIndexFileWhenTrailingSlashPresent() {
		writeFileToFilesDir("json/23456/index.json", "{ \"key\": \"new value\" }");
		startRunner();
		WireMockResponse response = testClient.get("/json/23456/");
		assertThat(response.statusCode(), is(200));
		assertThat(response.content(), is("{ \"key\": \"new value\" }"));
		assertThat(response.header("Content-Type"), is("application/json"));
	}
	
	@Test
	public void servesXmlIndexFileWhenTrailingSlashPresent() {
		writeFileToFilesDir("json/34567/index.xml", "<blob>BLAB</blob>");
		startRunner();
		WireMockResponse response = testClient.get("/json/34567/");
		assertThat(response.statusCode(), is(200));
		assertThat(response.content(), is("<blob>BLAB</blob>"));
		assertThat(response.header("Content-Type"), is("application/xml"));
	}
	
	@Test
	public void doesNotServeFileFromFilesDirWhenNotGET() {
		writeFileToFilesDir("json/should-not-see-this.json", "{}");
		startRunner();
		WireMockResponse response = testClient.put("/json/should-not-see-this.json");
		assertThat(response.statusCode(), is(404)); //Default servlet returns 405 if PUT is forwarded to it
	}
	
	private static final String BODY_FILE_MAPPING_REQUEST =
		"{ 													\n" +
		"	\"request\": {									\n" +
		"		\"method\": \"GET\",						\n" +
		"		\"url\": \"/body/file\"						\n" +
		"	},												\n" +
		"	\"response\": {									\n" +
		"		\"status\": 200,							\n" +
		"		\"bodyFileName\": \"body-test.xml\"			\n" +
		"	}												\n" +
		"}													";
	
	@Test
	public void readsBodyFileFromFilesDir() {
		writeMappingFile("test-mapping-2.json", BODY_FILE_MAPPING_REQUEST);
		writeFileToFilesDir("body-test.xml", "<body>Content</body>");
		startRunner();
		assertThat(testClient.get("/body/file").content(), is("<body>Content</body>"));
	}

    @Test
    public void readsBinaryBodyFileFromFilesDir() {
        writeMappingFile("test-mapping-2.json", BODY_FILE_MAPPING_REQUEST);
        writeFileToFilesDir("body-test.xml", binaryCompressedContent);
        startRunner();
        byte[] returnedContent = testClient.get("/body/file").binaryContent();
        assertThat(returnedContent, is(binaryCompressedContent));
        assertThat(decompress(returnedContent), is(binaryCompressedContentAsString));
    }

    /**
     * Decompress the binary gzipped content into a String.
     *
     * @param content the gzipped content to decompress
     * @return decompressed String.
     */
    public String decompress(byte[] content) {
        GZIPInputStream gin = null;
        try {
            gin = new GZIPInputStream(new ByteArrayInputStream(content));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buf = new byte[8192];

            int read = -1;
            while((read = gin.read(buf))!=-1) {
                baos.write(buf,0,read);
            }

            return new String(baos.toByteArray(), Charset.forName(UTF_8.name()));

        } catch (IOException e) {
            return null;
        } finally {
            if(gin!=null) try {
                gin.close();
            } catch (IOException e) {

            }
        }
    }

	@Test
	public void logsVerboselyWhenVerboseSetInCommandLine() {
		startRecordingSystemOut();
		startRunner("--verbose");
		assertThat(systemOutText(), containsString("Verbose logging enabled"));
	}
	
	@Test
	public void doesNotLogVerboselyWhenVerboseNotSetInCommandLine() {
		startRecordingSystemOut();
		startRunner();
		assertThat(systemOutText(), not(containsString("Verbose logging enabled")));
	}
	
	@Test
	public void startsOnPortSpecifiedOnCommandLine() {
		startRunner("--port", "8086");
		WireMock client = new WireMock("localhost", 8086);
		client.verifyThat(0, getRequestedFor(urlEqualTo("/bling/blang/blong"))); //Would throw an exception if couldn't connect
	}
	
	@Test
	public void proxiesToHostSpecifiedOnCommandLine() {
		WireMock otherServerClient = start8084ServerAndCreateClient();
		otherServerClient.register(get(urlEqualTo("/proxy/ok?working=yes")).willReturn(aResponse().withStatus(HTTP_OK)));
		startRunner("--proxy-all", "http://localhost:8084");
		
		WireMockResponse response = testClient.get("/proxy/ok?working=yes");
		assertThat(response.statusCode(), is(HTTP_OK));
	}

	@Test
	public void recordsProxiedRequestsWhenSpecifiedOnCommandLine() throws Exception {
	    WireMock otherServerClient = start8084ServerAndCreateClient();
		startRunner("--record-mappings");
		givenThat(get(urlEqualTo("/please/record-this"))
		        .willReturn(aResponse().proxiedFrom("http://localhost:8084")));
		otherServerClient.register(
		        get(urlEqualTo("/please/record-this"))
		        .willReturn(aResponse().withStatus(HTTP_OK).withBody("Proxied body")));

		testClient.get("/please/record-this");
		
		assertThat(mappingsDirectory, containsAFileContaining("/please/record-this"));
		assertThat(contentsOfFirstFileNamedLike("please-record-this"),
		        containsString("bodyFileName\":\"body-please-record-this"));
	}
	
	@Test
	public void performsBrowserProxyingWhenEnabled() {
		WireMock otherServerClient = start8084ServerAndCreateClient();
		startRunner("--enable-browser-proxying");
		otherServerClient.register(
		        get(urlEqualTo("/from/browser/proxy"))
		        .willReturn(aResponse().withStatus(HTTP_OK).withBody("Proxied body")));

		assertThat(testClient.getViaProxy("http://localhost:8084/from/browser/proxy").content(), is("Proxied body"));
	}
	
	@Test
	public void doesNotRecordRequestWhenNotProxied() {
	    startRunner("--record-mappings");
	    testClient.get("/try-to/record-this");
	    assertThat(mappingsDirectory, doesNotContainAFileWithNameContaining("try-to-record"));
	}

    private String contentsOfFirstFileNamedLike(String namePart) throws IOException {
        return Files.toString(firstFileWithNameLike(mappingsDirectory, namePart), UTF_8);
    }
	
	private File firstFileWithNameLike(File directory, String namePart) {
	    for (File file: directory.listFiles(namedLike(namePart))) {
	        return file;
	    }
	    
	    fail(String.format("Couldn't find a file under %s named like %s", directory.getPath(), namePart));
	    return null;
	}
	
	private FilenameFilter namedLike(final String namePart) {
	    return new FilenameFilter() {
            @Override
			public boolean accept(File file, String name) {
                return name.contains(namePart);
            }
        };
	}
	
	private WireMock start8084ServerAndCreateClient() {
        otherServer = new WireMockServer(8084);
        otherServer.start();
        WireMock otherServerClient = new WireMock("localhost", 8084);
        return otherServerClient;
    }
	
	private void writeFileToFilesDir(String name, String contents) {
		writeFileUnderFileSourceRoot(FILES + separator + name, contents);
	}

    private void writeFileToFilesDir(String name, byte[] contents) {
        writeFileUnderFileSourceRoot(FILES + separator + name, contents);
    }
	
	private void writeMappingFile(String name, String contents) {
		writeFileUnderFileSourceRoot(MAPPINGS + separator + name, contents);
	}
	
	private void writeFileUnderFileSourceRoot(String relativePath, String contents) {
		try {
			String filePath = FILE_SOURCE_ROOT + separator + relativePath;
			File file = new File(filePath);
			createParentDirs(file);
			write(contents, file, Charsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    private void writeFileUnderFileSourceRoot(String relativePath, byte[] contents) {
        try {
            String filePath = FILE_SOURCE_ROOT + separator + relativePath;
            File file = new File(filePath);
            createParentDirs(file);
            write(contents, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private void startRunner(String... args) {
		runner.run(FILE_SOURCE_ROOT.getPath(), args);
	}
	
	private void startRecordingSystemOut() {
		out = new ByteArrayOutputStream();
		System.setOut(new PrintStream(out));
	}
	
	private String systemOutText() {
		return new String(out.toByteArray());
	}
	
	private Matcher<File> containsAFileContaining(final String expectedContents) {
		return new TypeSafeMatcher<File>() {

			@Override
			public void describeTo(Description desc) {
				desc.appendText("a file containing " + expectedContents);
				
			}

			@Override
			public boolean matchesSafely(File dir) {
				for (File file: dir.listFiles()) {
					try {
						if (Files.toString(file, Charsets.UTF_8).contains(expectedContents)) {
							return true;
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				
				return false;
			}
			
		};
	}
	
	private Matcher<File> doesNotContainAFileWithNameContaining(final String namePart) {
        return new TypeSafeMatcher<File>() {

            @Override
            public void describeTo(Description desc) {
                desc.appendText("no file named like " + namePart);
                
            }

            @Override
            public boolean matchesSafely(File dir) {
                return !any(Arrays.<String>asList(dir.list()), new Predicate<String>() {
                    @Override
					public boolean apply(String input) {
                        return input.contains(namePart);
                    }
                });
            }
            
        };
    }
}
