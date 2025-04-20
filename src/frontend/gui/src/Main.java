import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;


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
    private Button startButton;
    private Button stopButton;


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Object Detection Platform");

        // Webcam feed view
        imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);

        // Detection log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefWidth(300);

        // Buttons
        startButton = new Button("Start");
        stopButton = new Button("Stop");
        startButton.setOnAction(e -> startCamera());
        stopButton.setOnAction(e -> stopCamera());

        Button captureButton = new Button("Capture");
        captureButton.setOnAction(e -> captureFrame());

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> {
            stopCamera(); // clean up resources
            Platform.exit(); // closes the JavaFX app
            System.exit(0); // force-kills the JVM (optional, but useful)
        });

        HBox buttonBox = new HBox(10, startButton, stopButton, captureButton, exitButton);
        buttonBox.setStyle("-fx-padding: 10; -fx-alignment: center;");

        // Layout
        HBox mainContent = new HBox(10, imageView, logArea);
        mainContent.setStyle("-fx-padding: 10;");

        VBox root = new VBox(10, mainContent, buttonBox);

        Scene scene = new Scene(root, 1000, 600);
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
                logArea.appendText("Error: Cannot open camera\n");
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
                logArea.appendText("Camera stopped\n");
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
                Platform.runLater(() -> logArea.appendText("Captured frame saved: " + filename + "\n"));
            } else {
                Platform.runLater(() -> logArea.appendText("Failed to capture frame\n"));
            }
        } else {
            Platform.runLater(() -> logArea.appendText("Camera not active, can't capture\n"));
        }
    }




}

