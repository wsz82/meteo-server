package io.szpikow.meteo.model.data;

public class MeteoData {
    public int id;
    public String value;

    public MeteoData(int id, String value_data) {
        this.id = id;
        this.value = value_data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
