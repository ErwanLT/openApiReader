package formater;

import java.util.List;

public interface Formatter {
    String fomratTitle(String title);
    String formatHeader(String header);
    String formatEndpoint(String endpoint);
    String formatEndpointDetail(String method, String path, String description, List<String> roles);
}