import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class WeatherWindow {
    private final Stage stage = new Stage();
    private final TextField cityField = new TextField();
    private final Label placeLbl = new Label("");
    private final Label tempLbl  = new Label("");   // current temperature
    private final Label condLbl  = new Label("");   // condition text
    private final Label windLbl  = new Label("");   // wind
    private final Label hiLoLbl  = new Label("");   // today high/low
    private final ProgressIndicator spinner = new ProgressIndicator();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public WeatherWindow(Stage owner) {
        stage.setTitle("Weather");
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setResizable(false);

        cityField.setPromptText("Enter city (e.g., San Diego)");
        cityField.setPrefColumnCount(18);

        Button go = new Button("Search");
        go.setDefaultButton(true);
        go.setOnAction(e -> search());

        HBox search = new HBox(8, new Label("City:"), cityField, go);
        search.setAlignment(Pos.CENTER_LEFT);

        tempLbl.setStyle("-fx-font-size: 48; -fx-font-weight: bold;");
        placeLbl.setStyle("-fx-font-size: 16; -fx-opacity: 0.9;");
        condLbl.setStyle("-fx-font-size: 16;");
        windLbl.setStyle("-fx-font-size: 14;");
        hiLoLbl.setStyle("-fx-font-size: 14;");

        VBox info = new VBox(6, placeLbl, tempLbl, condLbl, hiLoLbl, windLbl);
        info.setAlignment(Pos.CENTER_LEFT);

        spinner.setVisible(false);
        spinner.setPrefSize(28, 28);

        BorderPane root = new BorderPane();
        root.setTop(search);
        root.setCenter(new StackPane(info, spinner));
        BorderPane.setMargin(search, new Insets(12));
        BorderPane.setMargin(info, new Insets(12));

        stage.setScene(new Scene(root, 360, 220));
    }

    public void show() { stage.show(); }

    private void search() {
        String q = cityField.getText();
        if (q == null || q.isBlank()) {
            alert("Please enter a city name.");
            return;
        }
        spinner.setVisible(true);
        placeLbl.setText("Searching…");
        tempLbl.setText("");
        condLbl.setText("");
        windLbl.setText("");
        hiLoLbl.setText("");

        String geocodeUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                + urlEncode(q.trim()) + "&count=1&language=en&format=json";

        httpGET(geocodeUrl).thenCompose(geoJson -> {
            Double lat = findDouble(geoJson, "\"latitude\":");
            Double lon = findDouble(geoJson, "\"longitude\":");
            String name = findString(geoJson, "\"name\":\"");
            String country = findString(geoJson, "\"country\":\"");

            if (lat == null || lon == null) {
                throw new RuntimeException("City not found.");
            }
            String nicePlace = (name != null ? name : q) + (country != null ? ", " + country : "");
            final String placeText = nicePlace;

            // 2) Weather: current + today's hi/lo (Open-Meteo forecast)
            String wxUrl = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                    "&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=auto",
                    lat, lon);

            return httpGET(wxUrl).thenApply(wxJson -> placeText + "\n" + wxJson);
        }).whenComplete((combined, err) -> {
            Platform.runLater(() -> {
                spinner.setVisible(false);
                if (err != null) {
                    alert(err.getMessage() != null ? err.getMessage() : "Failed to fetch weather.");
                    placeLbl.setText("");
                    return;
                }
                // combined = "Place\n{json...}"
                int nl = combined.indexOf('\n');
                String place = combined.substring(0, nl);
                String wxJson = combined.substring(nl + 1);

                placeLbl.setText(place);

                // ----- Current (from "current_weather" only) -----
                String cw = jsonSection(wxJson, "current_weather");
                Double tempC = cw != null ? findDouble(cw, "\"temperature\":") : null;
                Integer code = cw != null ? findInt(cw, "\"weathercode\":")   : null;
                Double wind = cw != null ? findDouble(cw, "\"windspeed\":")   : null;

                if (tempC != null) tempLbl.setText(fmtTemp(tempC));
                if (code != null)  condLbl.setText(weatherCodeToText(code));
                if (wind != null)  windLbl.setText(String.format(Locale.US, "Wind: %.0f km/h", wind));

                // ----- Today's high/low -----
                Double tMax = findFirstArrayDouble(wxJson, "\"temperature_2m_max\":[");
                Double tMin = findFirstArrayDouble(wxJson, "\"temperature_2m_min\":[");
                if (tMax != null && tMin != null) {
                    hiLoLbl.setText("Today — High: " + fmtTemp(tMax) + "   Low: " + fmtTemp(tMin));
                } else {
                    hiLoLbl.setText("");
                }
            });
        });
    }

    
    private CompletableFuture<String> httpGET(String url) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "JavaFX-Phone/1.0")
                .GET().build();
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }


    private static Double findDouble(String json, String keyPrefix) {
        int i = json.indexOf(keyPrefix);
        if (i < 0) return null;
        i += keyPrefix.length();
        int j = i;
        while (j < json.length() && " -+.0123456789Ee".indexOf(json.charAt(j)) >= 0) j++;
        try { return Double.parseDouble(json.substring(i, j).trim()); }
        catch (Exception e) { return null; }
    }

    private static Integer findInt(String json, String keyPrefix) {
        int i = json.indexOf(keyPrefix);
        if (i < 0) return null;
        i += keyPrefix.length();
        int j = i;
        while (j < json.length() && " -+0123456789".indexOf(json.charAt(j)) >= 0) j++;
        try { return Integer.parseInt(json.substring(i, j).trim()); }
        catch (Exception e) { return null; }
    }

    private static String findString(String json, String keyPrefix) {
        int i = json.indexOf(keyPrefix);
        if (i < 0) return null;
        i += keyPrefix.length();
        int j = json.indexOf('"', i);
        if (j < 0) return null;
        return json.substring(i, j);
    }

    private static Double findFirstArrayDouble(String json, String keyPrefix) {
        int i = json.indexOf(keyPrefix);
        if (i < 0) return null;
        i += keyPrefix.length();
        int j = i;
        while (j < json.length() && " -+.0123456789Ee".indexOf(json.charAt(j)) >= 0) j++;
        try { return Double.parseDouble(json.substring(i, j).trim()); }
        catch (Exception e) { return null; }
    }

    private static String jsonSection(String json, String objectName) {
        int k = json.indexOf("\"" + objectName + "\"");
        if (k < 0) return null;
        int start = json.indexOf('{', k);
        if (start < 0) return null;
        int depth = 1, i = start + 1;
        while (i < json.length() && depth > 0) {
            char c = json.charAt(i++);
            if (c == '{') depth++;
            else if (c == '}') depth--;
        }
        return json.substring(start, i);
    }

    private static String fmtTemp(double c) {
        double f = c * 9 / 5 + 32;  // °F
        return String.format(Locale.US, "%.0f\u00B0F", f);
    }

    private static String weatherCodeToText(int code) {
        // Open-Meteo WMO weather codes (common ones)
        return switch (code) {
            case 0 -> "Clear";
            case 1, 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm w/ hail";
            default -> "Unknown";
        };
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.initOwner(stage);
        a.setHeaderText(null);
        a.setTitle("Weather");
        a.showAndWait();
    }
}
