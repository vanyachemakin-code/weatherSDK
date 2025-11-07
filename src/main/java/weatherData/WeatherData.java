package weatherData;

import lombok.AllArgsConstructor;
import lombok.Data;
import weatherData.data.SystemDetails;
import weatherData.data.TemperatureDetails;
import weatherData.data.WeatherDetails;
import weatherData.data.WindDetails;

@Data
@AllArgsConstructor
public class WeatherData {

    private WeatherDetails weather;
    private TemperatureDetails temperature;
    private long visibility;
    private WindDetails wind;
    private long dateTime;
    private SystemDetails sys;
    private int timeZone;
    private String name;
    private final long timestamp = System.currentTimeMillis();

    public boolean isRelevant() {
        long TEN_MINUTES_MS = 600000;
        return (System.currentTimeMillis() - this.timestamp) < TEN_MINUTES_MS;
    }
}
