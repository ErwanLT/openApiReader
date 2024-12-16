import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import exception.OpenApiReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class OpenApiReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiReader.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            LOGGER.error("Usage: java OpenApiReader <URL>");
            return;
        }

        String url = args[0];

        try {
            String jsonResponse = getOpenApiSpec(url);
            extractApi(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new OpenApiReaderException(e.getMessage());
        }
    }

    private static String getOpenApiSpec(String url) {
        return new RestTemplate().getForObject(url, String.class);
    }

    private static void extractApi(String openApiJson) throws JsonProcessingException {
        // ObjectMapper pour la lecture JSON
        ObjectMapper objectMapper = new ObjectMapper();
        // Lire la racine du document
        JsonNode rootNode = objectMapper.readTree(openApiJson);
        // Lire les chemins (paths)
        JsonNode pathsNode = rootNode.get("paths");

        if(pathsNode != null) {
            // Regrouper les chemins par racine
            Map<String, List<String>> groupedPaths = new TreeMap<>(groupPathsByRoot(pathsNode));
            // Afficher les APIs regroupées
            groupedPaths.forEach((apiRoot, endpoints) -> {
                // Afficher la racine de l'API
                LOGGER.info("=={}==", apiRoot);
                endpoints.forEach(LOGGER::info);
            });
        } else {
            LOGGER.error("Aucune API définie dans le fichier OpenAPI.");
        }
    }

    private static Map<String,? extends List<String>> groupPathsByRoot(JsonNode pathsNode) {
        // Créer un dictionnaire pour les chemins d'API indexés par leur racine
        Map<String, List<String>> groupedPaths = new HashMap<>();
        // Itère sur les chemins
        Iterator<Map.Entry<String, JsonNode>> pathIterator = pathsNode.fields();
        while (pathIterator.hasNext()) {
            // Récupère le chemin actuel
            Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
            String path = pathEntry.getKey();

            ArrayNode tags = (ArrayNode) pathEntry.getValue().fields().next().getValue().get("tags");
            var name = tags.get(0).asText();

            // Détermine si e chemin doit être exclu
            if(shouldExcludePath(path)) {
                // Ignore le chemin
                continue;
            }

            // Ajoute le chemin et ses méthodes au groupe
            JsonNode methodsNode = pathEntry.getValue();
            List<String> endpoints = groupedPaths.computeIfAbsent(name, k -> new ArrayList<>());
            // Itère sur les méthodes pour le chemin
            Iterator<Map.Entry<String, JsonNode>> methodsIterator = methodsNode.fields();
            while (methodsIterator.hasNext()) {
                // Récupération méthode actuelle
                Map.Entry<String, JsonNode> methodEntry = methodsIterator.next();
                String httpMethod = methodEntry.getKey().toUpperCase();

                // Récupération des détails de la méthode
                JsonNode methodDetails = methodEntry.getValue();
                String description;
                List<String> roles = new ArrayList<>();
                if (methodDetails.get("summary") != null) {
                    description = methodDetails.get("summary").asText();
                } else if(methodDetails.get("operationId") != null) {
                    description = methodDetails.get("operationId").asText();
                } else {
                    description = "Pas de nom";
                }
                if(methodDetails.get("security") != null) {
                    ArrayNode security = (ArrayNode) methodDetails.get("security").get(0).get("USER");
                    for (int i = 0; i < security.size(); i++) {
                        roles.add(security.get(i).asText());
                    }
                }
                String endpoint = String.format("  - %s %s : %s \n\t Roles : %s", httpMethod, path, description, String.join(",", roles));
                endpoints.add(endpoint);
            }
            Collections.sort(endpoints);
        }
        return groupedPaths;
    }

    private static boolean shouldExcludePath(String path) {
        // Chemin à exclure
        List<String> excludedPaths = List.of("test");
        return excludedPaths.stream().anyMatch(path::startsWith);
    }
}
