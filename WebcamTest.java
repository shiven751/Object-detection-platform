import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.Videoio;

class WebcamTest {
    public static void main(String[] args) {

       

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);  // Load OpenCV
        VideoCapture camera = new VideoCapture(0);     // Open default camera (0)
        
        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);

        System.out.println("Width: " + camera.get(Videoio.CAP_PROP_FRAME_WIDTH));
        System.out.println("Height: " + camera.get(Videoio.CAP_PROP_FRAME_HEIGHT));

        if (!camera.isOpened()) {
            System.out.println("Camera not found");
            return;
        }

        Mat frame = new Mat();
        System.out.println("Ctrl+C to exit");

        while (true) {
            if (camera.read(frame)) {
                HighGui.imshow("Webcam Feed", frame);
                HighGui.waitKey(30);  // this will refresh every 30ms
            }
        }
    }
}






// javac -cp "opencv-4110.jar" WebcamTest.java
// java -cp ".;opencv-4110.jar" WebcamTest











// one of the challenges here was that when we were running this using run switch,
// then it wasn't running as usually in vs code the classpath does not include this jar file bcz of which it wasn't able to detect the jar file,
// so we had to manually compile and run the file, where we specified the classpath.