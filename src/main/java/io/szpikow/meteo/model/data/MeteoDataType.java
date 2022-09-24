package io.szpikow.meteo.model.data;

public enum MeteoDataType {
    LAST_DATA(1);

    private final int id;

    MeteoDataType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
