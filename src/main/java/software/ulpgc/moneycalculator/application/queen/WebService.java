package software.ulpgc.moneycalculator.application.queen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import software.ulpgc.moneycalculator.architecture.model.Currency;
import software.ulpgc.moneycalculator.architecture.model.ExchangeRate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebService {
    private static final String ApiKey = "aeb1cd5ef6081142040d717f";
    private static final String ApiUrl = "https://v6.exchangerate-api.com/v6/" + ApiKey + "/";
    private static final Gson GSON = new Gson();

    public static class CurrencyLoader implements software.ulpgc.moneycalculator.architecture.io.CurrencyLoader {
        private static volatile List<Currency> cachedCurrencies = null;

        @Override
        public List<Currency> loadAll() {
            if (cachedCurrencies != null) {
                return cachedCurrencies;
            }
            synchronized (CurrencyLoader.class) {
                if (cachedCurrencies != null) {
                    return cachedCurrencies;
                }
                try {
                    cachedCurrencies = readCurrencies();
                    return cachedCurrencies;
                } catch (IOException e) {
                    return List.of();
                }
            }
        }

        private List<Currency> readCurrencies() throws IOException {
            try (InputStream is = openInputStream(createConnection())) {
                return readCurrenciesWith(jsonIn(is));
            }
        }

        private List<Currency> readCurrenciesWith(String json) {
            return readCurrenciesWith(jsonObjectIn(json));
        }

        private List<Currency> readCurrenciesWith(JsonObject jsonObject) {
            return readCurrenciesWith(jsonObject.get("supported_codes").getAsJsonArray());
        }

        private List<Currency> readCurrenciesWith(JsonArray jsonArray) {
            List<Currency> list = new ArrayList<>();
            for (JsonElement item : jsonArray)
                list.add(readCurrencyWith(item.getAsJsonArray()));
            return list;
        }

        private Currency readCurrencyWith(JsonArray tuple) {
            return new Currency(
                    tuple.get(0).getAsString(),
                    tuple.get(1).getAsString()
            );
        }

        private static String jsonIn(InputStream is) throws IOException {
            return new String(is.readAllBytes());
        }

        private static JsonObject jsonObjectIn(String json) {
            return GSON.fromJson(json, JsonObject.class);
        }

        private InputStream openInputStream(URLConnection connection) throws IOException {
            return connection.getInputStream();
        }

        private static URLConnection createConnection() throws IOException {
            URL url = new URL(ApiUrl + "codes");
            return url.openConnection();
        }
    }

    public static class ExchangeRateLoader implements software.ulpgc.moneycalculator.architecture.io.ExchangeRateLoader {
        private static final Map<String, CachedRate> rateCache = new ConcurrentHashMap<>();
        private static final long CACHE_EXPIRY_HOURS = 1;

        @Override
        public ExchangeRate load(Currency from, Currency to) {
            String cacheKey = String.format("%s-%s", from.code(), to.code());
            CachedRate cached = rateCache.get(cacheKey);
            
            if (cached != null && !cached.isExpired()) {
                return cached.rate;
            }
            
            try {
                ExchangeRate rate = new ExchangeRate(
                    LocalDate.now(),
                    from,
                    to,
                    readConversionRate(new URL(ApiUrl + "pair/" + from.code() + "/" + to.code()))
                );
                rateCache.put(cacheKey, new CachedRate(rate));
                return rate;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private double readConversionRate(URL url) throws IOException {
            return readConversionRate(url.openConnection());
        }

        private double readConversionRate(URLConnection connection) throws IOException {
            try (InputStream inputStream = connection.getInputStream()) {
                return readConversionRate(new String(new BufferedInputStream(inputStream).readAllBytes()));
            }
        }

        private double readConversionRate(String json) {
            return readConversionRate(GSON.fromJson(json, JsonObject.class));
        }

        private double readConversionRate(JsonObject object) {
            return object.get("conversion_rate").getAsDouble();
        }

        private static class CachedRate {
            final ExchangeRate rate;
            final LocalDateTime cacheTime;

            CachedRate(ExchangeRate rate) {
                this.rate = rate;
                this.cacheTime = LocalDateTime.now();
            }

            boolean isExpired() {
                return ChronoUnit.HOURS.between(cacheTime, LocalDateTime.now()) >= CACHE_EXPIRY_HOURS;
            }
        }
    }
}
