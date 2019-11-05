package ru.citeck.ecos.history.converter;

import java.util.ArrayList;
import java.util.List;

public interface Converter<SOURCE, RESULT> {

    RESULT convert(SOURCE source);

    default List<RESULT> convertAll(List<SOURCE> sources) {
        List<RESULT> results = new ArrayList<>(sources.size());
        for (SOURCE source : sources) {
            results.add(convert(source));
        }
        return results;
    }
}
