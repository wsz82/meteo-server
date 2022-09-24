package io.szpikow.meteo.model.data;

import org.springframework.data.repository.CrudRepository;

public interface MeteoDataRepository extends CrudRepository<MeteoData, Integer> {
}
