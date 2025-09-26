import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.effect.DropShadow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsWindow {
    private final Stage stage = new Stage();
    private final ImageView preview = new ImageView();

    private final Map<String, String> builtIn = new LinkedHashMap<>() {{
        put("Tiffy",  "/backgrounds/tiffany.png");
        put("Kitty",  "/backgrounds/boba.png");
        put("Homies", "/backgrounds/homies.png");
    }};

    public SettingsWindow(Phone phone) {
        stage.setTitle("Settings");
        stage.initModality(Modality.NONE);

        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(builtIn.keySet());
        combo.getSelectionModel().selectFirst();

        Button apply = new Button("Apply");
        apply.setOnAction(e -> {
            String key = combo.getValue();
            String path = builtIn.get(key);
            if (path != null) {
                phone.setWallpaper(path);
                preview.setImage(load(path));
            }
        });

        Button chooseFile = new Button("Choose Image…");
        chooseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose Wallpaper Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                phone.setWallpaperFile(f);
                preview.setImage(new Image(f.toURI().toString()));
            }
        });

        Slider xSlider = new Slider(-300, 300, phone.getWallpaperOffsetX());
        Slider ySlider = new Slider(-300, 300, phone.getWallpaperOffsetY());
        xSlider.setPrefWidth(220);
        ySlider.setPrefWidth(220);

        xSlider.valueProperty().addListener((obs, o, n) ->
                phone.setWallpaperOffset(xSlider.getValue(), ySlider.getValue()));
        ySlider.valueProperty().addListener((obs, o, n) ->
                phone.setWallpaperOffset(xSlider.getValue(), ySlider.getValue()));

        HBox offsets = new HBox(10,
                new Label("X:"), xSlider,
                new Label("Y:"), ySlider
        );
        offsets.setAlignment(Pos.CENTER_LEFT);

        HBox top = new HBox(10, new Label("Wallpaper:"), combo, apply, chooseFile);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10));

        preview.setFitWidth(220);
        preview.setFitHeight(420);
        preview.setPreserveRatio(true);

        String first = builtIn.values().iterator().next();
        preview.setImage(load(first));
        combo.setOnAction(e -> preview.setImage(load(builtIn.get(combo.getValue()))));

        StackPane previewPhone = new StackPane(preview);
        previewPhone.setPrefSize(220, 420);
        previewPhone.setMaxSize(220, 420);

        Rectangle clip = new Rectangle(220, 420);
        clip.setArcWidth(34 * 2);
        clip.setArcHeight(34 * 2);
        previewPhone.setClip(clip);

        DropShadow ds = new DropShadow(16, 0, 6, Color.rgb(0, 0, 0, 0.35));
        previewPhone.setEffect(ds);

        BorderPane root = new BorderPane();
        root.setTop(new VBox(top, offsets));
        BorderPane wrap = new BorderPane(previewPhone);
        wrap.setPadding(new Insets(10));
        wrap.setStyle("-fx-background-color: #111; -fx-background-radius: 16;");
        root.setCenter(wrap);
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root, 480, 600));
    }

    private static Image load(String classpath) {
        return new Image(Objects.requireNonNull(
                SettingsWindow.class.getResourceAsStream(classpath),
                "Missing wallpaper: " + classpath
        ));
    }

    public void show() { stage.show(); }
}
