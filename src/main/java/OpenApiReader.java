import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import exception.OpenApiReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Classe utilitaire pour lire et analyser une spécification OpenAPI.
 * Cette classe permet de regrouper les chemins (éléments "paths") de la spécification à partir d'une URL,
 * en extrayant les informations utiles comme les méthodes HTTP, descriptions, et rôles de sécurité.
 */
public class OpenApiReader {

    /**
     * Logger pour enregistrer les informations et les erreurs.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiReader.class);

    /**
     * Point d'entrée principal de l'application.
     *
     * @param args arguments de la ligne de commande. Le premier argument doit être l'URL de la spécification OpenAPI.
     */
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

    /**
     * Récupère la spécification OpenAPI à partir d'une URL donnée.
     *
     * @param url URL de la spécification OpenAPI.
     * @return La spécification OpenAPI sous forme de JSON.
     */
    private static String getOpenApiSpec(String url) {
        return new RestTemplate().getForObject(url, String.class);
    }

    /**
     * Analyse et regroupe les chemins de la spécification OpenAPI.
     *
     * @param openApiJson Chaîne JSON contenant la spécification OpenAPI.
     * @throws JsonProcessingException Si une erreur survient lors du traitement JSON.
     */
    private static void extractApi(String openApiJson) throws JsonProcessingException {
        // ObjectMapper pour la lecture JSON
        ObjectMapper objectMapper = new ObjectMapper();
        // Lire la racine du document
        JsonNode rootNode = objectMapper.readTree(openApiJson);
        // Lire les chemins (paths)
        JsonNode pathsNode = rootNode.get("paths");

        if (pathsNode != null) {
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

    /**
     * Regroupe les chemins de l'API par racine.
     *
     * @param pathsNode Noeud JSON contenant les chemins de la spécification OpenAPI.
     * @return Une map regroupant les chemins par racine avec leurs détails.
     */
    private static Map<String, ? extends List<String>> groupPathsByRoot(JsonNode pathsNode) {
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
            if (shouldExcludePath(path)) {
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
                } else if (methodDetails.get("operationId") != null) {
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

    /**
     * Vérifie si un chemin doit être exclu de l'analyse.
     *
     * @param path Le chemin à vérifier.
     * @return {@code true} si le chemin doit être exclu, {@code false} sinon.
     */
    private static boolean shouldExcludePath(String path) {
        // Chemin à exclure
        List<String> excludedPaths = List.of("test");
        return excludedPaths.stream().anyMatch(path::startsWith);
    }
}
