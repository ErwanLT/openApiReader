import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenApiReaderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiReaderTest.class);

    @Test
    void testGetOpenApiSpec() {
        // Mock du RestTemplate
        RestTemplate restTemplateMock = mock(RestTemplate.class);
        String mockUrl = "https://mock-api.com";
        String mockResponse = "{\"paths\": {}}";

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
        String emptyPathsJson = "{\"paths\": {}}";

        assertDoesNotThrow(() -> {
            OpenApiReader.extractApi(emptyPathsJson);
        });
    }

    @Test
    void testGroupPathsByRoot() {
        // Mock d'un JSONNode pour "paths"
        String json = """
            {
                "paths": {
                    "/pet/findByStatus": {
                        "get": {
                            "tags": ["pet"],
                            "summary": "Find pets by status"
                        }
                    },
                    "/store/order": {
                        "post": {
                            "tags": ["store"],
                            "summary": "Place an order"
                        }
                    }
                }
            }
            """;

        assertDoesNotThrow(() -> {
            OpenApiReader.extractApi(json);
        });
    }
}
