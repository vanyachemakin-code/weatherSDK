package sdk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import exception.WeatherException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import weatherData.WeatherData;
import weatherData.data.SystemDetails;
import weatherData.data.TemperatureDetails;
import weatherData.data.WeatherDetails;
import weatherData.data.WindDetails;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WeatherSDK {

    private static final String URL = "https://openweathermap.org/api";
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Map<String, WeatherData> weatherCache = new ConcurrentHashMap<>(10);
    private final Gson gson = new Gson();
    private ScheduledExecutorService service;
    private final String apiKey;

    WeatherSDK(String apiKey, OperationMode operationMode) {
        this.apiKey = apiKey;

        if (operationMode == OperationMode.POLLING) {
            startPollingUpdates();
        }
    }


    private String executeRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new WeatherException("Ошибка API: " + response.code() + " " + response.message());
            }
            return response.body() != null ? response.body().string() : null;
        } catch (IOException e) {
            throw new WeatherException("Сетевая ошибка при выполнении запроса");
        }
    }

    private WeatherData parseWeatherResponse(String jsonResponse) {
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);

        JsonObject weather = jsonObject.getAsJsonObject("weather");
        String main = weather.get("main").getAsString();
        String description = weather.get("description").getAsString();
        WeatherDetails weatherDetails = new WeatherDetails(main, description);

        JsonObject temperature = jsonObject.getAsJsonObject("temperature");
        double temp = temperature.get("temp").getAsDouble();
        double feelsLike = temperature.get("feels_like").getAsDouble();
        TemperatureDetails temperatureDetails = new TemperatureDetails(temp, feelsLike);

        long visibility = jsonObject.get("visibility").getAsLong();

        JsonObject wind = jsonObject.getAsJsonObject("wind");
        double speed = wind.get("speed").getAsDouble();
        WindDetails windDetails = new WindDetails(speed);

        long dateTime = jsonObject.get("datetime").getAsLong();

        JsonObject sys = jsonObject.getAsJsonObject("sys");
        long sunrise = sys.get("sunrise").getAsLong();
        long sunset = sys.get("sunset").getAsLong();
        SystemDetails systemDetails = new SystemDetails(sunrise, sunset);

        int timeZone = jsonObject.get("timezone").getAsInt();
        String name = jsonObject.get("name").getAsString();

        return new WeatherData(
                weatherDetails,
                temperatureDetails,
                visibility,
                windDetails,
                dateTime,
                systemDetails,
                timeZone,
                name
        );
    }

    public WeatherData getCurrentWeatherByCity(String cityName) throws WeatherException {
        if (weatherCache.containsKey(cityName) && weatherCache.get(cityName).isRelevant()) {
            return weatherCache.get(cityName);
        }

        String url = MessageFormat.format("{0}/{1}/{2}", URL, cityName, apiKey);
        String jsonResponse = executeRequest(url);

        WeatherData weatherData = parseWeatherResponse(jsonResponse);
        storeInformation(weatherData);
        return weatherData;
    }

    private synchronized void storeInformation(WeatherData weatherData) {
        for (WeatherData data: weatherCache.values()) {
            if (!data.isRelevant()) weatherCache.remove(data.getName());
        }
        if (weatherCache.size() < 10) weatherCache.put(weatherData.getName(), weatherData);
    }

    private void startPollingUpdates() {
        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(
                () -> {
                    for (WeatherData data: weatherCache.values()) {
                        if (!data.isRelevant()) {
                            WeatherData weatherData = getCurrentWeatherByCity(data.getName());
                            weatherCache.replace(weatherData.getName(), weatherData);
                        }
                    }
                },
                0,
                10,
                TimeUnit.MINUTES
        );
    }

    public void stopUpdating() {
        if (!service.isShutdown() && service != null) {
            service.shutdown();
            service.close();
        }
    }

    public enum OperationMode {
        REQUEST, POLLING
    }
}
