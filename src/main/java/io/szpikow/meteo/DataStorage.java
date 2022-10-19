package io.szpikow.meteo;

import io.szpikow.meteo.model.data.MeteoDataType;

import java.util.EnumMap;

public class DataStorage {
    private static EnumMap<MeteoDataType, String> data = new EnumMap<>(MeteoDataType.class);

    public static EnumMap<MeteoDataType, String> getData() {
        return data;
    }

    public static void setData(EnumMap<MeteoDataType, String> data) {
        DataStorage.data = data;
    }
}
