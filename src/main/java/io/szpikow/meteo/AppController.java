package io.szpikow.meteo;

import io.szpikow.meteo.model.data.MeteoData;
import io.szpikow.meteo.model.data.MeteoDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AppController extends io.szpikow.meteo.Controller {
    private static final String INDEX = "index";
    private static final String ERROR = "error";

    private final Logger logger = LoggerFactory.getLogger(AppController.class);

    @Autowired
    public SimpMessagingTemplate simpMessagingTemplate;

    @GetMapping({"/", "/" + INDEX})
    public String get(Model model) {
        Map<String, String> meteoDataNameToValue = getMeteoDataNameToValue();
        if (meteoDataNameToValue.isEmpty()) {
            logger.info("Failed to receive data from database");
            return ERROR;
        }
        meteoDataNameToValue.forEach((k, v) -> model.addAttribute("_" + k, v));
        return INDEX;
    }

    @PostMapping({"/", "/" + INDEX})
    public ResponseEntity<String> post(HttpServletRequest request) {
        String password = System.getenv("METEO_AUTH");
        if (password == null) {
            logger.error("No environmental variable METEO_AUTH with password for web service");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String meteoAuth = request.getHeader("meteoauth");
        if (meteoAuth == null) {
            logger.error("No password sent with POST request");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        if (!password.equals(meteoAuth)) {
            logger.info("Unauthorized access");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try (BufferedReader reader = request.getReader()) {
            String rawData = reader.lines().collect(Collectors.joining());
            String data = rawData.substring(rawData.indexOf("(") + 1, rawData.indexOf(")"));
            String[] dataArr = data.split(";");
            Map<MeteoDataType, MeteoData> typeToMeteoData = new HashMap<>();
            for (String keyVal : dataArr) {
                String[] split = keyVal.split(":");
                String name = split[0];
                MeteoDataType type = MeteoDataType.valueOf(name);
                int ordinal = type.ordinal();
                String val = split[1];
                MeteoData meteoData = new MeteoData(ordinal, val);
                typeToMeteoData.put(type, meteoData);
            }
            MeteoData date = typeToMeteoData.get(MeteoDataType.D);
            MeteoData time = typeToMeteoData.get(MeteoDataType.T);
            if (date != null && time != null) {
                String format = "yyMMddHHmmss";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDateTime dateTime = LocalDateTime.parse(date.value + time.value, formatter);
                dateTime = dateTime.plusHours(2);
                String localDate = dateTime.toLocalDate().toString();
                date = new MeteoData(date.id, localDate);
                typeToMeteoData.put(MeteoDataType.D, date);
                String localTime = dateTime.toLocalTime().toString();
                time = new MeteoData(time.id, localTime);
                typeToMeteoData.put(MeteoDataType.T, time);
            }
            EnumMap<MeteoDataType, String> dataMap = typeToMeteoData.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, d -> d.getValue().value, (s, s2) -> s, () -> new EnumMap<>(MeteoDataType.class)));
            DataStorage.setData(dataMap);
            updateMeteoDataToUsers(dataMap);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateMeteoDataToUsers(EnumMap<MeteoDataType, String> dataMap) {
        Map<String, String> map = dataMap.entrySet().stream()
                .collect(Collectors.toMap(d -> d.getKey().name(), d -> d.getValue(), (s, s2) -> s, HashMap::new));
        simpMessagingTemplate.convertAndSend("/topic/meteo", map);
    }
}
