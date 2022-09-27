package io.szpikow.meteo;

import io.szpikow.meteo.model.data.MeteoData;
import io.szpikow.meteo.model.data.MeteoDataRepository;
import io.szpikow.meteo.model.data.MeteoDataType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Controller {

    @Autowired
    MeteoDataRepository repo;

    protected Map<String, String> getMeteoDataNameToValue() {
        Iterable<MeteoData> meteoData = repo.findAll();
        List<MeteoData> meteoDataList = new ArrayList<>();
        meteoData.forEach(meteoDataList::add);
        meteoDataList.sort(Comparator.comparingInt(value -> value.id));
        MeteoDataType[] meteoDataTypes = MeteoDataType.values();
        return meteoDataList.stream()
                .collect(Collectors.toMap(d -> meteoDataTypes[d.id].toString(), d -> d.value_data, (s, s2) -> s, LinkedHashMap::new));
    }
}
