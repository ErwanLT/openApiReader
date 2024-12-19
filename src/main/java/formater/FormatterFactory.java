package formater;

import exception.UnsupportedFormatException;

public class FormatterFactory {
    public static Formatter getFormatter(String format) {
        return switch (format.toLowerCase()) {
            case "wiki" -> new WikiFormatter();
            case "markdown" -> new MarkdownFormatter();
            default -> throw new UnsupportedFormatException("Format '" + format + "' non supporté.");
        };
    }
}