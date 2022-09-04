import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;

public class Fix {
    public static class LocalDateDeserializer implements JsonDeserializer<LocalDate> {
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            String strDate = json.isJsonObject() ? "" : json.getAsString();
            if (json.isJsonObject()) {
                JsonObject j = json.getAsJsonObject();
                int[] date = new int[3];
                String[] params = new String[] {"year", "month", "day"};
                for (int i = 0; i < date.length; i++) {
                    date[i] = Integer.parseInt(j.get(params[i]).getAsString());
                }
                return LocalDate.of(date[0], date[1], date[2]);
            }

            return LocalDate.parse(strDate);
        }
    }

    public static class LocalDateSerializer implements JsonSerializer<LocalDate> {
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    public static Gson getGsonWithSerAdapter() {
        return new GsonBuilder().registerTypeAdapter(LocalDate.class, new Fix.LocalDateSerializer())
                .create();
    }

    public static Gson getGsonWithDeSerAdapter() {
        return new GsonBuilder().registerTypeAdapter(LocalDate.class, new Fix.LocalDateDeserializer())
                .create();
    }
}
