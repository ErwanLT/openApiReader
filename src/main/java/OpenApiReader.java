import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import exception.OpenApiReaderException;
import exception.UnsupportedFormatException;
import formater.Formatter;
import formater.FormatterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Classe utilitaire pour lire et analyser une spécification OpenAPI.
 * Cette classe permet de regrouper les chemins (éléments "paths") de la spécification à partir d'une URL,
 * en extrayant les informations utiles comme les méthodes HTTP, descriptions, et rôles de sécurité.
 */
public class OpenApiReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiReader.class);
    private static RestTemplate restTemplate = new RestTemplate();

    public OpenApiReader(RestTemplate restTemplate) {
        OpenApiReader.restTemplate = restTemplate;
    }

    /**
     * Point d'entrée principal de l'application.
     *
     * @param args arguments de la ligne de commande. Le premier argument doit être l'URL de la spécification OpenAPI.
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            LOGGER.error("Usage: java OpenApiReader <URL> [format]");
            return;
        }

        String url = args[0];
        String format = args.length == 2 ? args[1] : "wiki";

        try {
            String jsonResponse = getOpenApiSpec(url);
            Formatter formatter = FormatterFactory.getFormatter(format);
            extractApi(jsonResponse, formatter);
        } catch (UnsupportedFormatException e) {
            LOGGER.error("Format non supporté : {}", e.getMessage());
        } catch (JsonProcessingException e) {
            throw new OpenApiReaderException(e.getMessage());
        }
    }

    /**
     * Récupère la spécification OpenAPI à partir d'une URL donnée.
     *
     * @param url URL de la spécification OpenAPI.
     * @return La spécification OpenAPI sous forme de JSON.
     */
    static String getOpenApiSpec(String url) {
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * Analyse et regroupe les chemins de la spécification OpenAPI.
     *
     * @param openApiJson Chaîne JSON contenant la spécification OpenAPI.
     * @param formatter Formatter pour formater les détails des endpoints.
     * @throws JsonProcessingException Si une erreur survient lors du traitement JSON.
     */
    static void extractApi(String openApiJson, Formatter formatter) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(openApiJson);
        var title = rootNode.get("info").get("title").asText();

        JsonNode pathsNode = rootNode.get("paths");
        if (pathsNode != null) {
            LOGGER.info(formatter.fomratTitle(title));
            Map<String, List<String>> groupedPaths = new TreeMap<>(groupPathsByRoot(pathsNode, formatter));
            groupedPaths.forEach((apiRoot, endpoints) -> {
                LOGGER.info(formatter.formatHeader(apiRoot));
                endpoints.forEach(endpoint -> LOGGER.info(formatter.formatEndpoint(endpoint)));
            });
        } else {
            LOGGER.error("Aucune API définie dans le fichier OpenAPI.");
        }
    }

    /**
     * Regroupe les chemins de l'API par racine.
     *
     * @param pathsNode Noeud JSON contenant les chemins de la spécification OpenAPI.
     * @param formatter Formatter pour formater les détails des endpoints.
     * @return Une map regroupant les chemins par racine avec leurs détails.
     */
    static Map<String, List<String>> groupPathsByRoot(JsonNode pathsNode, Formatter formatter) {
        Map<String, List<String>> groupedPaths = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> pathIterator = pathsNode.fields();
        while (pathIterator.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
            String path = pathEntry.getKey();

            ArrayNode tags = (ArrayNode) pathEntry.getValue().fields().next().getValue().get("tags");
            var name = tags.get(0).asText();

            if (shouldExcludePath(path)) {
                continue;
            }

            JsonNode methodsNode = pathEntry.getValue();
            List<String> endpoints = groupedPaths.computeIfAbsent(name, k -> new ArrayList<>());
            Iterator<Map.Entry<String, JsonNode>> methodsIterator = methodsNode.fields();
            while (methodsIterator.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methodsIterator.next();
                String httpMethod = methodEntry.getKey().toUpperCase();

                JsonNode methodDetails = methodEntry.getValue();
                String description = methodDetails.has("summary")
                        ? methodDetails.get("summary").asText()
                        : methodDetails.has("operationId")
                        ? methodDetails.get("operationId").asText()
                        : "Pas de description";

                // Extraction des rôles
                List<String> roles = extractRoles(methodDetails);

                endpoints.add(formatter.formatEndpointDetail(httpMethod, path, description, roles));
            }
            Collections.sort(endpoints);
        }
        return groupedPaths;
    }

    /**
     * Extrait les rôles de sécurité d'une méthode.
     * @param methodDetails Détails de la méthode.
     * @return La liste des rôles de sécurité.
     */
    private static List<String> extractRoles(JsonNode methodDetails) {
        List<String> roles = new ArrayList<>();
        if (methodDetails.has("security")) {
            ArrayNode security = (ArrayNode) methodDetails.get("security");
            for (JsonNode securityRequirement : security) {
                securityRequirement.fieldNames().forEachRemaining(roles::add);
            }
        }
        return roles;
    }

    /**
     * Vérifie si un chemin doit être exclu de l'analyse.
     *
     * @param path Le chemin à vérifier.
     * @return {@code true} si le chemin doit être exclu, {@code false} sinon.
     */
    static boolean shouldExcludePath(String path) {
        List<String> excludedPaths = List.of("test");
        return excludedPaths.stream().anyMatch(path::startsWith);
    }
}
