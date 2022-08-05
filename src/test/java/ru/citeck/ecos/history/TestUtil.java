package ru.citeck.ecos.history;

import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for testing REST controllers.
 */
public final class TestUtil {

    public static final String URL_RECORDS_MUTATE = "/api/records/mutate";
    public static final String URL_RECORDS_QUERY = "/api/records/query";

    /** MediaType for JSON UTF8 */
    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

    private TestUtil() {}
}
