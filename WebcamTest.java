import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.highgui.HighGui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

class ModularWebcamBLIP {

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        startFrameCapture();
    }

    // Capture frames from the webcam, display the feed, and process frames for captioning.
    public static void startFrameCapture() {
        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("Camera not found");
            return;
        }
        camera.set(3, 400);
        camera.set(4, 250);

        Mat frame = new Mat();
        System.out.println("Press Ctrl+C to exit");

        while (true) {
            if (camera.read(frame)) {
                HighGui.imshow("Webcam Feed", frame);
                HighGui.waitKey(1);
                try {
                    String caption = processFrameAndGetCaption(frame);
                    System.out.println("Caption: " + caption);
                    Thread.sleep(2000);
                } catch (Exception e) {
                    System.err.println("Error processing frame: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    // Process a frame and return a caption from the BLIP API.
    public static String processFrameAndGetCaption(Mat frame) throws IOException {
        BufferedImage image = matToBufferedImage(frame);
        if (image == null) throw new IOException("Failed to convert frame");
        File tempFile = new File("temp.jpg");
        ImageIO.write(image, "jpg", tempFile);
        String base64Image = encodeFileToBase64(tempFile.getAbsolutePath());
        System.out.println("Base64 string length: " + base64Image.length());
        tempFile.delete();
        String caption = sendToBLIP(base64Image, true);
        if (caption.contains("Error") || caption.equals("No caption generated")) {
            System.out.println("Trying without prefix...");
            caption = sendToBLIP(base64Image, false);
        }
        return caption;
    }

    // Convert Mat to BufferedImage.
    private static BufferedImage matToBufferedImage(Mat mat) throws IOException {
        MatOfByte mob = new MatOfByte();
        if (!Imgcodecs.imencode(".jpg", mat, mob)) return null;
        byte[] byteArray = mob.toArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        return ImageIO.read(bis);
    }

    // Encode file content to Base64.
    private static String encodeFileToBase64(String filePath) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        return Base64.getEncoder().encodeToString(fileContent);
    }

    // Send the Base64 image to the BLIP API.
    private static String sendToBLIP(String base64Image, boolean usePrefix) {
        String apiUrl = "https://api-inference.huggingface.co/models/Salesforce/blip-image-captioning-base";
        String token = "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";  // Replace with your actual API token

        String imageData = usePrefix ? "data:image/jpeg;base64," + base64Image : base64Image;
        String jsonInputString = "{\"inputs\": \"" + imageData + "\"}";

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + token);
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            InputStream is = (responseCode >= 200 && responseCode < 300) ? con.getInputStream() : con.getErrorStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is, "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line.trim());
            }
            in.close();

            String jsonResponse = response.toString();
            System.out.println("Full API Response: " + jsonResponse);

            int index = jsonResponse.indexOf("\"generated_text\":");
            if (index != -1) {
                int start = jsonResponse.indexOf("\"", index + 17) + 1;
                int end = jsonResponse.indexOf("\"", start);
                return jsonResponse.substring(start, end);
            }
            return "No caption generated";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling BLIP API";
        }
    }
}

















// To manually compile and run the program
// javac -cp "opencv-4110.jar" WebcamTest.java
// java -cp ".;opencv-4110.jar" WebcamTest



























































// one of the challenges here was that when we were running this using run switch,
// then it wasn't running as usually in vs code the classpath does not include this jar file bcz of which it wasn't able to detect the jar file,
// so we had to manually compile and run the file, where we specified the classpath.