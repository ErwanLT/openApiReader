import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import formater.Formatter;
import formater.MarkdownFormatter;
import formater.WikiFormatter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenApiReaderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiReaderTest.class);

    private static final String SPEC_FILE_PATH = "src/test/resources/open-api.json";

    private String loadSpecFile() throws IOException {
        return new String(Files.readAllBytes(Paths.get(SPEC_FILE_PATH)));
    }

    @Test
    void testGetOpenApiSpec() throws IOException {
        // Mock du RestTemplate
        RestTemplate restTemplateMock = mock(RestTemplate.class);
        String mockUrl = "https://mock-api.com";
        String mockResponse = loadSpecFile();

        // Définir le comportement du mock
        when(restTemplateMock.getForObject(mockUrl, String.class)).thenReturn(mockResponse);

        // Créer une instance de OpenApiReader avec le mock
        OpenApiReader reader = new OpenApiReader(restTemplateMock);

        // Appeler la méthode
        String response = OpenApiReader.getOpenApiSpec(mockUrl);

        // Assertions
        assertNotNull(response);
        assertEquals(mockResponse, response);
    }

    @Test
    void testShouldExcludePath() {
        assertTrue(OpenApiReader.shouldExcludePath("test/example"));
        assertFalse(OpenApiReader.shouldExcludePath("api/example"));
    }

    @Test
    void testExtractApiWithEmptyPaths() {
        String emptyPathsJson = """
                {
                  "openapi": "3.0.1",
                  "info": {
                    "title": "Human cloning API",
                    "description": "API for creating clone who will fight in the clones wars",
                    "contact": {
                      "name": "LE TUTOUR Erwan",
                      "url": "https://github.com/ErwanLT",
                      "email": "erwanletutour.elt@gmail.com"
                    },
                    "license": {
                      "name": "MIT Licence",
                      "url": "https://opensource.org/licenses/mit-license.php"
                    },
                    "version": "2.0"
                  },
                  "servers": [
                    {
                      "url": "http://localhost:8080",
                      "description": "Generated server url"
                    }
                  ],
                  "paths": {
                  }
                }
                """;
        Formatter wikiFormatter = new WikiFormatter();

        assertDoesNotThrow(() -> {
            OpenApiReader.extractApi(emptyPathsJson, wikiFormatter);
        });
    }

    @Test
    void testGroupPathsByRoot() throws IOException {
        String mockResponse = loadSpecFile();
        Formatter wikiFormatter = new WikiFormatter();
        Formatter markdownFormatter = new MarkdownFormatter();

        Map<String, List<String>> groupedPaths = OpenApiReader.groupPathsByRoot(
                new ObjectMapper().readTree(mockResponse).get("paths"), wikiFormatter
        );

        assertNotNull(groupedPaths);
        assertEquals(2, groupedPaths.size());
        assertTrue(groupedPaths.containsKey("pet"));
        assertTrue(groupedPaths.containsKey("store"));

        List<String> petEndpoints = groupedPaths.get("pet");
        assertEquals(1, petEndpoints.size());
        assertTrue(petEndpoints.get(0).contains("Rôles : user"));

        List<String> storeEndpoints = groupedPaths.get("store");
        assertEquals(1, storeEndpoints.size());
        assertTrue(storeEndpoints.get(0).contains("Rôles : admin"));

        // Test Markdown Formatter
        groupedPaths = OpenApiReader.groupPathsByRoot(
                new ObjectMapper().readTree(mockResponse).get("paths"), markdownFormatter
        );

        assertNotNull(groupedPaths);
        assertEquals(2, groupedPaths.size());
        assertTrue(groupedPaths.get("pet").get(0).contains("Rôles : user"));
        assertTrue(groupedPaths.get("store").get(0).contains("Rôles : admin"));
    }

    @Test
    void testExtractApiWithMultipleFormats() throws IOException {
        Formatter wikiFormatter = new WikiFormatter();
        Formatter markdownFormatter = new MarkdownFormatter();
        String mockResponse = loadSpecFile();

        assertDoesNotThrow(() -> {
            OpenApiReader.extractApi(mockResponse, wikiFormatter);
            OpenApiReader.extractApi(mockResponse, markdownFormatter);
        });
    }
}