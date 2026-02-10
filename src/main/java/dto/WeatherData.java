package dto;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import dto.data.SystemDetails;
import dto.data.TemperatureDetails;
import dto.data.WeatherDetails;
import dto.data.WindDetails;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class WeatherData {

    private List<WeatherDetails>  weather;

    @SerializedName("main")
    private TemperatureDetails temperature;

    private long visibility;
    private WindDetails wind;

    @SerializedName("dt")
    private long dateTime;

    private SystemDetails sys;
    private int timezone;
    private String name;
    private final long cacheTimestamp = System.currentTimeMillis();
    private static final long TEN_MINUTES_MS = 600_000L;

    public boolean isRelevant() {
        return (System.currentTimeMillis() - this.cacheTimestamp) < TEN_MINUTES_MS;
    }
}
