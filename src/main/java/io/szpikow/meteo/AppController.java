package io.szpikow.meteo;

import io.szpikow.meteo.model.DataStorage;
import io.szpikow.meteo.model.MeteoDataType;
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
            EnumMap<MeteoDataType, String> typeToMeteoData = new EnumMap<>(MeteoDataType.class);
            for (String keyVal : dataArr) {
                String[] split = keyVal.split(":");
                String name = split[0];
                MeteoDataType type = MeteoDataType.valueOf(name);
                String val = split[1];
                typeToMeteoData.put(type, val);
            }
            String date = typeToMeteoData.get(MeteoDataType.D);
            String time = typeToMeteoData.get(MeteoDataType.T);
            if (date != null && time != null) {
                String format = "yyMMddHHmmss";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDateTime dateTime = LocalDateTime.parse(date + time, formatter);
                dateTime = dateTime.plusHours(2);
                String localDate = dateTime.toLocalDate().toString();
                typeToMeteoData.put(MeteoDataType.D, localDate);
                String localTime = dateTime.toLocalTime().toString();
                typeToMeteoData.put(MeteoDataType.T, localTime);
            }
            DataStorage.setData(typeToMeteoData);
            updateUsersWithCurrentData();
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateUsersWithCurrentData() {
        Map<String, String> meteoDataNameToValue = getMeteoDataNameToValue();
        simpMessagingTemplate.convertAndSend("/topic/meteo", meteoDataNameToValue);
    }
}
