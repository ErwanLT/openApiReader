package formater;

import java.util.List;

public class WikiFormatter implements Formatter {
    @Override
    public String fomratTitle(String title) {
        return String.format("== Matrice des habilitations %s ==", title);
    }

    @Override
    public String formatHeader(String header) {
        return String.format("=== %s ===", header);
    }

    @Override
    public String formatEndpoint(String endpoint) {
        return endpoint;
    }

    @Override
    public String formatEndpointDetail(String method, String path, String description, List<String> roles) {
        String rolesText = roles.isEmpty() ? "Tous les rôles" : String.join(", ", roles);
        return String.format("  - '''%s''' %s : %s (Rôles : %s)", method, path, description, rolesText);
    }
}