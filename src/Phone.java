import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class Phone extends Application {
    private static final double PHONE_W = 420;
    private static final double PHONE_H = 820;

    // live wallpaper offsets (px)
    private double wallpaperOffsetX = 0;
    private double wallpaperOffsetY = 0;

    // persistence (settings file in user home)
    private final Path settingsFile = Paths.get(
            System.getProperty("user.home"),
            ".javafx_phone_demo", "settings.txt"
    );

    // UI
    private VBox phoneBody;
    private ImageView wallpaperView;      // background layer
    private StackPane layeredPhone;       // clipped rounded-rect "device"

    @Override
    public void start(Stage stage) {
        Pane phone = buildPhone(stage);
        Scene scene = new Scene(phone);
        stage.setTitle("Jordan's IPhone");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        // Load last wallpaper AFTER UI is built
        loadLastWallpaper();
    }

    private Pane buildPhone(Stage owner) {
        StackPane root = new StackPane();
        root.setPadding(new Insets(20));

        phoneBody = new VBox(16);
        phoneBody.setPrefSize(PHONE_W, PHONE_H);
        phoneBody.setMaxSize(PHONE_W, PHONE_H);
        phoneBody.setStyle("-fx-background-color: transparent;"); // transparent so wallpaper shows

        // ---- wallpaper as real ImageView (cover + center + nudges) ----
        wallpaperView = new ImageView();
        wallpaperView.setPreserveRatio(true);
        wallpaperView.setSmooth(true);
        StackPane.setAlignment(wallpaperView, Pos.CENTER);

        // status time
        Label timeLbl = new Label(nowHHmm());
        timeLbl.setStyle("-fx-text-fill: white; -fx-font-size: 48; -fx-font-weight: bold;");
        HBox status = new HBox(timeLbl);
        status.setAlignment(Pos.CENTER);

        GridPane grid = new GridPane();
        grid.setHgap(22);
        grid.setVgap(22);
        grid.setAlignment(Pos.TOP_CENTER);

        // Icons from /resources/icons/*.png
        grid.add(appIcon("/icons/calculator.png", "Calculator", () -> new CalculatorWindow(owner).show()), 0, 0);
        grid.add(appIcon("/icons/notes.png",      "Notes",      () -> new NotesWindow(owner).show()),      1, 0);
        grid.add(appIcon("/icons/clock.png",      "Clock",      () -> info(owner, "Demo only")),           2, 0);
        grid.add(appIcon("/icons/weather.png", "Weather",       () -> new WeatherWindow(owner).show()),    3, 0);
        grid.add(appIcon("/icons/music.png",      "Music",      () -> info(owner, "Demo only")),           0, 1);
        grid.add(appIcon("/icons/mail.png",       "Mail",       () -> info(owner, "Demo only")),           1, 1);
        grid.add(appIcon("/icons/settings.png",   "Settings",   () -> new SettingsWindow(this).show()),    2, 1);
        grid.add(appIcon("/icons/photos.png",     "Photos",     () -> info(owner, "Demo only")),           3, 1);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region homeIndicator = new Region();
        homeIndicator.setPrefSize(160, 6);
        homeIndicator.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 4;");
        StackPane homeWrap = new StackPane(homeIndicator);
        homeWrap.setPadding(new Insets(8));

        phoneBody.getChildren().addAll(status, grid, spacer, homeWrap);

        // stack: wallpaper (back) + content (front)
        layeredPhone = new StackPane(wallpaperView, phoneBody);
        layeredPhone.setPrefSize(PHONE_W, PHONE_H);
        layeredPhone.setMaxSize(PHONE_W, PHONE_H);

        // rounded-corner clip for whole device (affects wallpaper & content)
        Rectangle clip = new Rectangle();
        clip.setArcWidth(34 * 2);
        clip.setArcHeight(34 * 2);
        clip.widthProperty().bind(layeredPhone.widthProperty());
        clip.heightProperty().bind(layeredPhone.heightProperty());
        layeredPhone.setClip(clip);

        // drop shadow for the device
        DropShadow ds = new DropShadow(24, 0, 8, Color.rgb(0, 0, 0, 0.35));
        layeredPhone.setEffect(ds);

        // recompute wallpaper "cover" layout on size/image change
        layeredPhone.widthProperty().addListener((o, ov, nv) -> layoutWallpaperCover());
        layeredPhone.heightProperty().addListener((o, ov, nv) -> layoutWallpaperCover());
        wallpaperView.imageProperty().addListener((o, ov, nv) -> layoutWallpaperCover());

        root.getChildren().add(layeredPhone);

        // update time every 30s
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(30), e -> timeLbl.setText(nowHHmm())));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();

        return root;
    }

    private static String nowHHmm() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }

    /** Create a launcher tile using a PNG from the classpath, cropping out any transparent padding. */
    private VBox appIcon(String imgClasspath, String label, Runnable onOpen) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.TOP_CENTER);

        Image img = new Image(Objects.requireNonNull(
            getClass().getResourceAsStream(imgClasspath),
            "Missing resource: " + imgClasspath
        ));

        ImageView iv = iconViewCropped(img);
        iv.setFitWidth(64);
        iv.setFitHeight(64);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        Button btn = new Button();
        btn.setGraphic(iv);
        btn.setMinSize(64, 64);
        btn.setPrefSize(64, 64);
        btn.setStyle("-fx-padding: 0; -fx-background-color: transparent; -fx-background-radius: 16; -fx-border-radius: 16;");
        btn.setOnAction(e -> onOpen.run());

        Label text = new Label(label);
        text.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 2, 0, 0, 1);");

        box.getChildren().addAll(btn, text);
        return box;
    }

    /** Crop out transparent borders from an image using its alpha channel. */
    private ImageView iconViewCropped(Image img) {
        PixelReader pr = img.getPixelReader();
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = pr.getArgb(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a != 0) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        ImageView iv = new ImageView(img);
        if (maxX >= minX && maxY >= minY) {
            int vw = maxX - minX + 1;
            int vh = maxY - minY + 1;
            iv.setViewport(new Rectangle2D(minX, minY, vw, vh));
        }
        return iv;
    }

    // --------- Wallpaper API (cover + center + offsets) ---------

    /** Set wallpaper from a classpath resource and save choice. */
    public void setWallpaper(String resourcePath) {
        Image bg = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(resourcePath),
                "Missing background: " + resourcePath
        ));
        applyWallpaperView(bg);
        saveWallpaper(resourcePath);
    }

    /** Set wallpaper from a file on disk and save absolute path. */
    public void setWallpaperFile(File file) {
        Image bg = new Image(file.toURI().toString());
        applyWallpaperView(bg);
        saveWallpaper(file.getAbsolutePath());
    }

    /** Apply image and re-apply offsets + cover layout. */
    private void applyWallpaperView(Image bg) {
        wallpaperView.setImage(bg);
        wallpaperView.setTranslateX(wallpaperOffsetX);
        wallpaperView.setTranslateY(wallpaperOffsetY);
        layoutWallpaperCover();
    }

    /** Adjust wallpaper offsets live (used by Settings sliders). */
    public void setWallpaperOffset(double offsetX, double offsetY) {
        this.wallpaperOffsetX = offsetX;
        this.wallpaperOffsetY = offsetY;
        if (wallpaperView != null) {
            wallpaperView.setTranslateX(offsetX);
            wallpaperView.setTranslateY(offsetY);
        }
    }

    public double getWallpaperOffsetX() { return wallpaperOffsetX; }
    public double getWallpaperOffsetY() { return wallpaperOffsetY; }

    /** Emulate CSS background-size: cover; background-position: center; */
    private void layoutWallpaperCover() {
        Image img = wallpaperView.getImage();
        if (img == null) return;

        double pw = PHONE_W;  // if you allow resizing, use layeredPhone.getWidth()/getHeight()
        double ph = PHONE_H;

        double iw = img.getWidth();
        double ih = img.getHeight();
        if (iw <= 0 || ih <= 0) return;

        double phoneAspect = pw / ph;
        double imageAspect = iw / ih;

        wallpaperView.setPreserveRatio(true);
        if (phoneAspect > imageAspect) {
            // phone is "wider" -> fill width, height will overflow (crop top/bottom)
            wallpaperView.setFitWidth(pw);
            wallpaperView.setFitHeight(0); // unset height so width drives scaling
        } else {
            // phone is "taller" -> fill height, width will overflow (crop sides)
            wallpaperView.setFitHeight(ph);
            wallpaperView.setFitWidth(0);  // unset width so height drives scaling
        }
        // keep current nudges
        wallpaperView.setTranslateX(wallpaperOffsetX);
        wallpaperView.setTranslateY(wallpaperOffsetY);
    }

    private void saveWallpaper(String value) {
        try {
            Files.createDirectories(settingsFile.getParent());
            Files.writeString(settingsFile, value, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save wallpaper setting: " + e.getMessage());
        }
    }

    private void loadLastWallpaper() {
        try {
            if (Files.exists(settingsFile)) {
                String value = Files.readString(settingsFile, StandardCharsets.UTF_8).trim();
                if (value.isEmpty()) return;
                if (value.startsWith("/")) {
                    Image bg = new Image(Objects.requireNonNull(
                            getClass().getResourceAsStream(value),
                            "Missing saved resource: " + value
                    ));
                    applyWallpaperView(bg);
                } else {
                    File f = new File(value);
                    if (f.exists()) applyWallpaperView(new Image(f.toURI().toString()));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load wallpaper setting: " + e.getMessage());
        }
    }

    private static void info(Stage owner, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.initOwner(owner);
        a.initModality(Modality.WINDOW_MODAL);
        a.setHeaderText(null);
        a.setTitle("Info");
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
