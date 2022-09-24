package io.szpikow.meteo;

import io.szpikow.meteo.model.data.MeteoData;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class AppController {
    private static final String INDEX = "index";
    private static final String ERROR = "error";

    private final Logger logger = LoggerFactory.getLogger(AppController.class);

    @Autowired
    MeteoDataRepository meteoDataRepository;

    @GetMapping({"/", "/" + INDEX})
    public String get(Model model) {
        int id = MeteoDataType.LAST_DATA.getId();
        Optional<MeteoData> data = meteoDataRepository.findById(id);

        if (data.isEmpty()) {
            logger.info("Failed to receive data from database");
            return ERROR;
        }

        String dataValue = data.get().meteo_data;
        model.addAttribute("data", dataValue);
        return INDEX;
    }

    @PostMapping({"/", "/" + INDEX})
    public ResponseEntity<String> post(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            String rawData = reader.lines().collect(Collectors.joining());
            MeteoData data = new MeteoData(MeteoDataType.LAST_DATA.getId(), rawData);
            meteoDataRepository.save(data);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
