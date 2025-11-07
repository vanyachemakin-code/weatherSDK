package sdk;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SDKRegistration {

    private static final Map<String, WeatherSDK> activeSDK = new ConcurrentHashMap<>();

    public static WeatherSDK getInstant(String apiKey, WeatherSDK.OperationMode operationMode) {
        return activeSDK.computeIfAbsent(apiKey, key -> new WeatherSDK(key, operationMode));
    }

    public static void removeInstant(String apiKey) {
        WeatherSDK weatherSDK = activeSDK.remove(apiKey);
        if (weatherSDK != null) weatherSDK.stopUpdating();
    }

    public static int getActiveSDKCount() {
        return activeSDK.size();
    }

    public static Collection<String> getActiveAPIKeys() {
        return activeSDK.keySet();
    }
}