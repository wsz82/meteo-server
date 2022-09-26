package io.szpikow.meteo;

import io.szpikow.meteo.model.data.MeteoData;
import io.szpikow.meteo.model.data.MeteoDataDisplay;
import io.szpikow.meteo.model.data.MeteoDataRepository;
import io.szpikow.meteo.model.data.MeteoDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class AppController {
    private static final String INDEX = "index";
    private static final String ERROR = "error";

    private final Logger logger = LoggerFactory.getLogger(AppController.class);

    @Autowired
    MeteoDataRepository repo;

    @GetMapping({"/", "/" + INDEX})
    public String get(Model model) {
        Iterable<MeteoData> meteoData = repo.findAll();
        List<MeteoData> meteoDataList = new ArrayList<>();
        meteoData.forEach(meteoDataList::add);

        if (meteoDataList.isEmpty()) {
            logger.info("Failed to receive data from database");
            return ERROR;
        }

        meteoDataList.sort(Comparator.comparingInt(value -> value.id));

        MeteoDataType[] meteoDataTypes = MeteoDataType.values();
        List<MeteoDataDisplay> meteoDataDisplayList = meteoDataList.stream()
                .map(data -> new MeteoDataDisplay(meteoDataTypes[data.id].toString(), data.value_data))
                .collect(Collectors.toList());

        model.addAttribute("data", meteoDataDisplayList);

        for (int i = 0; i < meteoDataTypes.length; i++) {
            MeteoDataType meteoDataType = meteoDataTypes[i];
            MeteoDataDisplay data = meteoDataDisplayList.get(i);
            model.addAttribute(meteoDataType.name(), data);
        }
        return INDEX;
    }

    @PostMapping({"/", "/" + INDEX})
    public ResponseEntity<String> post(HttpServletRequest request) {
        String meteoAuth = request.getHeader("meteoauth");
        if (meteoAuth == null) {
            throw new NullPointerException("No password sent with POST request");
        }
        String password = System.getenv("METEO_AUTH");
        if (password == null) {
            throw new NullPointerException("No environmental variable METEO_AUTH with password for web service");
        }
        if (!password.equals(meteoAuth)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try (BufferedReader reader = request.getReader()) {
            String rawData = reader.lines().collect(Collectors.joining());
            String data = rawData.substring(rawData.indexOf("(") + 1, rawData.indexOf(")"));
            String[] dataArr = data.split(";");
            List<MeteoData> dataList = new ArrayList<>();
            for (String keyVal : dataArr) {
                String[] split = keyVal.split(":");
                String name = split[0];
                int ordinal = MeteoDataType.valueOf(name).ordinal();
                String val = split[1];
                MeteoData meteoData = new MeteoData(ordinal, val);
                dataList.add(meteoData);
            }
            for (MeteoData meteoData : dataList) {
                Optional<MeteoData> dataById = repo.findById(meteoData.id);
                if (dataById.isPresent()) {
                    MeteoData meteoDataInRepo = dataById.get();
                    meteoDataInRepo.value_data = meteoData.value_data;
                    repo.save(meteoDataInRepo);
                } else {
                    repo.save(meteoData);
                }
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
