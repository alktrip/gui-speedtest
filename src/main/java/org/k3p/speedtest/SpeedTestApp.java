package org.k3p.speedtest;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.k3p.speedtest.component.InfoBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

public class SpeedTestApp extends Application {

    InfoBox pingBox = new InfoBox("📶", "Ping", "-- ms");
    InfoBox downloadBox = new InfoBox("⬇️", "Descarga", "-- Mbps");
    InfoBox uploadBox = new InfoBox("⬆️", "Carga", "-- Mbps");
    InfoBox ispBox = new InfoBox("🌐", "Proveedor", "--");

    private Label statusLabel;

    private Button startButton;
    private Hyperlink resultUrlLink;

    private XYChart.Series<Number, Number> downloadSeries;
    private XYChart.Series<Number, Number> uploadSeries;

    private LineChart<Number, Number> speedUploadChart;
    private LineChart<Number, Number> speedChart;

    private TextArea outputArea;  // Nueva área de texto para la salida

    @Override
    public void start(Stage primaryStage) {

        MenuBar menuBar = new MenuBar();
        Menu helpMenu = new Menu("Ayuda");

        MenuItem aboutItem = new MenuItem("Acerca de...");
        aboutItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Acerca de Monitor de Velocidad");
            alert.setHeaderText("SpeedTest GUI (JavaFX)");
            alert.setContentText("Desarrollado por Catrip (alktrip)\n" +
                    "Versión 0.0.1 Alpha\n\n" +
                    "Repositorio: https://github.com/tu_usuario/tu_repositorio\n" +
                    "© 2025");
            alert.showAndWait();
        });

        helpMenu.getItems().add(aboutItem);
        menuBar.getMenus().add(helpMenu);

        statusLabel = new Label("Listo para validar velocidad de internet");
        statusLabel.getStyleClass().add("status-label");

        resultUrlLink = new Hyperlink("Link de resultados: --");
        resultUrlLink.getStyleClass().add("link-label");

        resultUrlLink.setOnAction(e -> {
            if (!resultUrlLink.getText().equals("Link de resultados: --")) {
                getHostServices().showDocument(resultUrlLink.getText().replace("Link de resultados: ", ""));
            }
        });

        startButton = new Button("Iniciar Prueba");
        startButton.getStyleClass().add("start-button");
        startButton.setOnAction(e -> runSpeedTest());

        // Set up the chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Tiempo de descarga (milisegundos)");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Velocidad de descarga (Mbps)");

        NumberAxis xUploadAxis = new NumberAxis();
        xUploadAxis.setLabel("Tiempo de carga (milisegundos)");

        NumberAxis yUploadAxis = new NumberAxis();
        yUploadAxis.setLabel("Velocidad de carga (Mbps)");

        speedChart = new LineChart<>(xAxis, yAxis);
        speedChart.setTitle("Velocidad de descarga");

        speedUploadChart = new LineChart<>(xUploadAxis, yUploadAxis);
        speedUploadChart.setTitle("Velocidad de carga");

        downloadSeries = new XYChart.Series<>();
        downloadSeries.setName("Descarga");

        uploadSeries = new XYChart.Series<>();
        uploadSeries.setName("Carga");

        speedChart.getData().add(downloadSeries);
        speedUploadChart.getData().add(uploadSeries);

        downloadSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: red;");
        uploadSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: blue;");

        // Nueva área de texto para la salida
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefWidth(800);
        outputArea.setWrapText(true);
        outputArea.setPromptText("Salida en tiempo real de speedtest...");
        outputArea.setPrefWidth(700);

        HBox topRow = new HBox(20, pingBox, ispBox, downloadBox, uploadBox);
        topRow.setAlignment(Pos.CENTER);

        VBox infoSection = new VBox(20, topRow);
        infoSection.setAlignment(Pos.CENTER);

        VBox leftPane = new VBox(20, menuBar, statusLabel, infoSection, startButton, speedChart, speedUploadChart, resultUrlLink);
        leftPane.setPrefWidth(700);
        leftPane.setAlignment(Pos.TOP_CENTER);
        leftPane.setStyle("-fx-padding: 20;");

        HBox root = new HBox(20, leftPane, outputArea);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");

        leftPane.getStyleClass().add("left-pane");
        outputArea.getStyleClass().add("output-area");

        Scene scene = new Scene(root, 1500, 1500);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());

        primaryStage.setTitle("SpeedTest GUI (JavaFX)");
        primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("/logo.png")).toExternalForm()));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void runSpeedTest() {

        pingBox.setValue("-- ms");
        downloadBox.setValue("-- Mbps");
        uploadBox.setValue("-- Mbps");
        ispBox.setValue("--");
        statusLabel.setText("Iniciando prueba de velocidad...");
        resultUrlLink.setVisited(false);
        resultUrlLink.setText("Link de resultados: --");
        outputArea.clear();

        startButton.setDisable(true);

        Thread thread = new Thread(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(
                        "/usr/bin/speedtest",
                        "--accept-license",
                        "--accept-gdpr",
                        "--progress=yes"
                );
                builder.redirectErrorStream(true);
                Process process = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                long startTime = System.currentTimeMillis();

                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> {
                        outputArea.appendText(finalLine + "\n");
                        outputArea.setScrollTop(Double.MAX_VALUE);  // Auto scroll
                    });

                    if (line.contains("Latency")) {
                        String ping = extractNumber(line) + " ms";
                        Platform.runLater(() -> pingBox.setValue(ping));
                    } else if (line.contains("Download")) {
                        double downloadValue = extractNumber(line);
                        String downloadText = downloadValue + " Mbps";
                        double elapsedMilliseconds = (System.currentTimeMillis() - startTime);
                        Platform.runLater(() -> {
                            downloadBox.setValue(downloadText);
                            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(elapsedMilliseconds, downloadValue);
                            downloadSeries.getData().add(dataPoint);
                            dataPoint.getNode().setStyle("-fx-background-color: white, red; -fx-padding: 5px;");
                        });
                    } else if (line.contains("Upload")) {
                        double uploadValue = extractNumber(line);
                        String uploadText = uploadValue + " Mbps";
                        double elapsedMilliseconds = (System.currentTimeMillis() - startTime);
                        Platform.runLater(() -> {
                            uploadBox.setValue(uploadText);
                            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(elapsedMilliseconds, uploadValue);
                            uploadSeries.getData().add(dataPoint);
                            dataPoint.getNode().setStyle("-fx-background-color: white, blue; -fx-padding: 5px;");
                        });
                    } else if (line.contains("Result URL")) {
                        String url = line.substring(line.indexOf("https")).trim();
                        Platform.runLater(() -> resultUrlLink.setText("Link de resultados: " + url));
                    } else if (line.contains("ISP")) {
                        String isp = extractMessage(line);
                        Platform.runLater(() -> ispBox.setValue(isp));
                    }
                }
                process.waitFor();
                Platform.runLater(() -> statusLabel.setText("Test completo"));
                startButton.setDisable(false);
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
                Platform.runLater(() -> {
                    outputArea.appendText("Error: " + ex.getMessage() + "\n");
                    outputArea.setScrollTop(Double.MAX_VALUE);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private double extractNumber(String text) {
        String[] parts = text.split(" ");
        for (String part : parts) {
            try {
                return Double.parseDouble(part);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private String extractMessage(String text) {
        List<String> parts = List.of(text.split(" "));
        return parts.get(parts.size() - 1);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
