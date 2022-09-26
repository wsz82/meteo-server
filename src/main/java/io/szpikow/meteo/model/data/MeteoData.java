package io.szpikow.meteo.model.data;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@NoArgsConstructor
@AllArgsConstructor

@Entity
public class MeteoData {
    @Id
    public int id;

    @Column(columnDefinition = "TEXT")
    public String value_data;
}
