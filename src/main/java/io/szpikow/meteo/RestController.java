package io.szpikow.meteo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("api/v1/data")
public class RestController extends Controller {

    @GetMapping()
    public Map<String, String> getData() {
        return getMeteoDataNameToValue();
    }

    @GetMapping("/{name}")
    public Map<String, String> getParameter(@PathVariable(value = "name") String name) {
        String value = getMeteoDataNameToValue().get(name);
        Map<String, String> response = new HashMap<>();
        response.put(name, value);
        return response;
    }
}
