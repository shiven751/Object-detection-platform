package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends Application {
    static {
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    private ImageView imageView;
    private TextArea logArea;
    private VideoCapture capture;
    private Timer timer;
    private boolean cameraActive = false;
    private boolean isDarkMode = false;

    private BorderPane root;
    private Scene scene;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Object Detection Platform");

        // Image View with rounded corners
        imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);
        Rectangle clip = new Rectangle(640, 480);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageView.setClip(clip);

        // Detection log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefWidth(340);
        logArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13;");

        // Buttons
        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");
        Button captureButton = new Button("Capture");
        Button saveButton = new Button("Save Caption");
        Button clearButton = new Button("Clear Output");
        Button themeButton = new Button("Toggle Theme");
        Button exitButton = new Button("Exit");

        startButton.setOnAction(e -> {
            startCamera();
            updateStatus("Camera started");
        });
        stopButton.setOnAction(e -> {
            stopCamera();
            updateStatus("Camera stopped");
        });
        captureButton.setOnAction(e -> captureFrame());
        saveButton.setOnAction(e -> saveCaption());
        clearButton.setOnAction(e -> logArea.clear());
        themeButton.setOnAction(e -> toggleTheme());
        exitButton.setOnAction(e -> {
            stopCamera();
            Platform.exit();
            System.exit(0);
        });

        HBox buttonBox = new HBox(10, startButton, stopButton, captureButton, saveButton, clearButton, themeButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        HBox mainContent = new HBox(20, imageView, logArea);
        mainContent.setPadding(new Insets(15));

        statusLabel = new Label("Ready");
        statusLabel.setId("status-label");
        statusLabel.setPadding(new Insets(5));
        statusLabel.setStyle("-fx-font-size: 12;");

        HBox statusBar = new HBox(statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5, 10, 5, 10));

        root = new BorderPane();
        root.setTop(buttonBox);
        root.setCenter(mainContent);
        root.setBottom(statusBar);

        scene = new Scene(root, 1080, 620);
        scene.getStylesheets().add(getClass().getResource("/application/style.css").toExternalForm());


        applyLightTheme(); // default theme

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void startCamera() {
        if (!cameraActive) {
            capture = new VideoCapture(0);
            if (capture.isOpened()) {
                cameraActive = true;
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Mat frame = new Mat();
                        if (capture.read(frame)) {
                            Image fxImage = matToImage(frame);
                            Platform.runLater(() -> imageView.setImage(fxImage));
                        }
                    }
                }, 0, 33);
            } else {
                log("Error: Cannot open camera");
            }
        }
    }

    private void stopCamera() {
        if (cameraActive) {
            cameraActive = false;
            if (timer != null) {
                timer.cancel();
            }
            if (capture != null) {
                capture.release();
            }
            Platform.runLater(() -> {
                imageView.setImage(null);
                log("Camera stopped");
            });
        }
    }

    private Image matToImage(Mat frame) {
        Mat converted = new Mat();
        Imgproc.cvtColor(frame, converted, Imgproc.COLOR_BGR2RGB);
        byte[] data = new byte[converted.rows() * converted.cols() * (int)(converted.elemSize())];
        converted.get(0, 0, data);
        BufferedImage bufferedImage = new BufferedImage(converted.width(), converted.height(), BufferedImage.TYPE_3BYTE_BGR);
        bufferedImage.getRaster().setDataElements(0, 0, converted.cols(), converted.rows(), data);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private void captureFrame() {
        if (cameraActive && capture != null && capture.isOpened()) {
            Mat frame = new Mat();
            if (capture.read(frame)) {
                String filename = "capture_" + System.currentTimeMillis() + ".png";
                Imgcodecs.imwrite(filename, frame);
                log("Captured frame saved: " + filename);
                updateStatus("Frame saved");
            } else {
                log("Failed to capture frame");
            }
        } else {
            log("Camera not active, can't capture");
        }
    }

    private void saveCaption() {
        String content = logArea.getText();
        if (content.isEmpty()) {
            log("No caption to save");
            return;
        }
        String filename = "caption_" + System.currentTimeMillis() + ".txt";
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(filename), content.getBytes());
            log("Caption saved to: " + filename);
            updateStatus("Caption saved");
        } catch (Exception e) {
            log("Failed to save caption");
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            applyDarkTheme();
        } else {
            applyLightTheme();
        }
    }

    private void applyLightTheme() {
        root.getStyleClass().remove("dark-mode");
    }

    private void applyDarkTheme() {
        if (!root.getStyleClass().contains("dark-mode")) {
            root.getStyleClass().add("dark-mode");
        }
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
}