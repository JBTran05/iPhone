import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Locale;

public class CalculatorWindow {
    private final Stage stage = new Stage();
    private final Label display = new Label("0");
    private Double acc = null; 
    private String op = null;  
    private boolean justEvaluated = false;

    public CalculatorWindow(Stage owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("Calculator");

        VBox root = new VBox(12);
        root.setPadding(new Insets(12));

        display.setStyle("-fx-font-size: 36; -fx-background-color: black; -fx-text-fill: white; -fx-padding: 12; -fx-background-radius: 16;");
        display.setAlignment(Pos.CENTER_RIGHT);
        display.setMinHeight(64);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);

        addBtn(grid, "C", 0,0, e -> clearAll());
        addBtn(grid, "+/−", 0,1, e -> toggleSign());
        addBtn(grid, "%", 0,2, e -> percent());
        addBtn(grid, "÷", 0,3, e -> setOp("÷"));

        addBtn(grid, "7", 1,0, e -> digit("7"));
        addBtn(grid, "8", 1,1, e -> digit("8"));
        addBtn(grid, "9", 1,2, e -> digit("9"));
        addBtn(grid, "×", 1,3, e -> setOp("×"));

        addBtn(grid, "4", 2,0, e -> digit("4"));
        addBtn(grid, "5", 2,1, e -> digit("5"));
        addBtn(grid, "6", 2,2, e -> digit("6"));
        addBtn(grid, "-", 2,3, e -> setOp("-"));

        addBtn(grid, "1", 3,0, e -> digit("1"));
        addBtn(grid, "2", 3,1, e -> digit("2"));
        addBtn(grid, "3", 3,2, e -> digit("3"));
        addBtn(grid, "+", 3,3, e -> setOp("+"));

        Button zero = addBtn(grid, "0", 4,0, e -> digit("0"));
        zero.setMaxWidth(Double.MAX_VALUE);
        GridPane.setColumnSpan(zero, 2);
        addBtn(grid, ".", 4,2, e -> digit("."));
        addBtn(grid, "=", 4,3, e -> equals());

        root.getChildren().addAll(display, grid);
        stage.setScene(new Scene(root, 320, 420));
    }

    private Button addBtn(GridPane g, String text, int r, int c, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(text);
        b.setOnAction(h);
        b.setPrefSize(64, 48);
        b.setStyle("-fx-background-radius: 12; -fx-background-color: #f3f4f6; -fx-font-size: 16;");
        g.add(b, c, r);
        return b;
    }

    private void digit(String d) {
        String cur = display.getText();
        if (justEvaluated) { justEvaluated = false; display.setText(d.equals(".") ? "0." : d); return; }
        if (".".equals(d) && cur.contains(".")) return;
        if (cur.equals("0") && !d.equals(".")) display.setText(d); else display.setText(cur + d);
    }

    private void setOp(String next) {
        justEvaluated = false;
        double current = Double.parseDouble(display.getText());
        if (acc == null) acc = current; else acc = compute(acc, current, op);
        op = next; display.setText("0");
    }

    private double compute(double a, double b, String operator) {
        if (operator == null) return b;
        return switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "×" -> a * b;
            case "÷" -> (b == 0 ? Double.NaN : a / b);
            default -> b;
        };
    }

    private void equals() {
        if (acc == null || op == null) return;
        double result = compute(acc, Double.parseDouble(display.getText()), op);
        if (!Double.isFinite(result)) display.setText("Error"); else display.setText(trim(result));
        acc = null; op = null; justEvaluated = true;
    }

    private static String trim(double x) {
        String s = String.format(Locale.US, "%.10f", x);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) s = s.substring(0, s.length()-1);
        return s;
    }

    private void clearAll() { display.setText("0"); acc = null; op = null; justEvaluated = false; }
    private void toggleSign() { String t = display.getText(); display.setText(t.startsWith("-") ? t.substring(1) : (t.equals("0")? t : "-" + t)); }
    private void percent() { double v = Double.parseDouble(display.getText()); display.setText(trim(v/100.0)); }

    public void show() { stage.show(); }
}
