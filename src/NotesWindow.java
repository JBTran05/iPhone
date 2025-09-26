import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NotesWindow {
    private final Stage stage = new Stage();
    private final TextArea input = new TextArea();
    private final ListView<String> list = new ListView<>();
    private final Path store = Paths.get(System.getProperty("user.home"), ".javafx_phone_demo", "notes.txt");

    public NotesWindow(Stage owner) {
        stage.initOwner(owner);
        stage.setTitle("Notes");
        stage.initModality(Modality.NONE);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        input.setPromptText("Write a note...");
        input.setWrapText(true);
        input.setPrefRowCount(3);

        Button add = new Button("Add");
        add.setOnAction(e -> addNote());

        HBox top = new HBox(8, input, add);
        HBox.setHgrow(input, Priority.ALWAYS);

        list.setPlaceholder(new Label("No notes yet."));

        Button del = new Button("Delete");
        del.setOnAction(e -> deleteSelected());
        Button save = new Button("Save to File");
        save.setOnAction(e -> saveToFile());
        HBox bottom = new HBox(8, del, save);

        root.setTop(top);
        root.setCenter(list);
        root.setBottom(bottom);

        loadFromFile();

        stage.setScene(new Scene(root, 420, 520));
    }

    private void addNote() {
        String t = input.getText().trim();
        if (t.isEmpty()) return;
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        list.getItems().add(0, stamp + " — " + t);
        input.clear();
        persist();
    }

    private void deleteSelected() {
        int idx = list.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        list.getItems().remove(idx);
        persist();
    }

    private void ensureStoreDir() throws IOException { Files.createDirectories(store.getParent()); }

    private void persist() {
        try {
            ensureStoreDir();
            Files.write(store, list.getItems(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save notes: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void loadFromFile() {
        try {
            if (Files.exists(store)) {
                var lines = Files.readAllLines(store, StandardCharsets.UTF_8);
                list.getItems().setAll(lines);
            }
        } catch (IOException ignored) { }
    }

    private void saveToFile() {
        persist();
        new Alert(Alert.AlertType.INFORMATION, "Notes saved.", ButtonType.OK).showAndWait();
    }

    public void show() { stage.show(); }
}
