package io.szpikow.meteo;

import io.szpikow.meteo.model.DataStorage;
import io.szpikow.meteo.model.MeteoDataType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Controller {

    protected Map<String, String> getMeteoDataNameToValue() {
        EnumMap<MeteoDataType, String> data = DataStorage.getData();
        return data.entrySet().stream()
                .collect(Collectors.toMap(d -> d.getKey().name(), Map.Entry::getValue, (s, s2) -> s, LinkedHashMap::new));
    }
}
