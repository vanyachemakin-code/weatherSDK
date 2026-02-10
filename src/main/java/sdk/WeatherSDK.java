package sdk;

import com.google.gson.Gson;
import dto.WeatherData;
import dto.data.OperationMode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;

public class WeatherSDK {
    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    private final Map<String, WeatherData> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, WeatherData>(11, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, WeatherData> eldest) {
                    return size() > 10;
                }
            }
    );

    private ScheduledExecutorService scheduler;

    protected WeatherSDK(String apiKey, OperationMode mode) {
        this.apiKey = apiKey;

        validateApiKey();

        if (mode == OperationMode.POLLING) {
            startPolling();
        }
    }

    private void validateApiKey() {
        try {
            String url = "https://api.openweathermap.org/" + apiKey;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new RuntimeException("Invalid API Key");
            }
        } catch (Exception e) {
            throw new RuntimeException("API Validation failed: " + e.getMessage());
        }
    }

    private void startPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Auto-updating cache...");
            Set<String> cities = new HashSet<>(cache.keySet());
            for (String city : cities) {
                updateWeatherData(city);
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    public WeatherData getCityWeather(String city) {
        String cityKey = city.toLowerCase();
        WeatherData data = cache.get(cityKey);

        if (data == null || !data.isRelevant()) {
            data = updateWeatherData(cityKey);
        }
        return data;
    }

    private WeatherData updateWeatherData(String city) {
        try {
            String url = String.format("https://api.openweathermap.org/%S/%S", city, apiKey);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                WeatherData data = gson.fromJson(response.body(), WeatherData.class);
                cache.put(city, data);
                return data;
            }
        } catch (Exception e) {
            System.err.println("Error updating city " + city + ": " + e.getMessage());
        }
        return cache.get(city);
    }

    public void stopUpdating() {
        if (scheduler != null) scheduler.shutdownNow();
    }
}
