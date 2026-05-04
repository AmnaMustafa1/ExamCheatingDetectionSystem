package com.project.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.project.report.PDFReport;

import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class MainApp extends Application {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private VideoCapture camera;
    private CascadeClassifier faceDetector;
    private CascadeClassifier eyeDetector;

    private int prevFaceX = -1;
    private int movementCounter = 0;

    private long lastCaptureTime = 0;

    private final String basePath = System.getProperty("user.dir");

    @Override
    public void start(Stage stage) {

        faceDetector = new CascadeClassifier(
                "C:\\Users\\ALPHA\\Downloads\\opencv\\sources\\data\\haarcascades\\haarcascade_frontalface_default.xml"
        );

        eyeDetector = new CascadeClassifier(
                "C:\\Users\\ALPHA\\Downloads\\opencv\\sources\\data\\haarcascades\\haarcascade_eye.xml"
        );

        Label title = new Label("AI Exam Proctoring System");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        ImageView cameraView = new ImageView();
        cameraView.setFitWidth(800);

        Label status = new Label("Status: Starting Camera...");
        status.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");

        // 🔥 PDF BUTTON
        Button generateBtn = new Button("Generate Report");
        generateBtn.setStyle("-fx-font-size: 16px; -fx-background-color: blue; -fx-text-fill: white;");

        generateBtn.setOnAction(e -> {
            PDFReport.generateReport();
            status.setText("📄 PDF Report Generated!");
        });

        VBox root = new VBox(10, title, cameraView, status, generateBtn);

        Scene scene = new Scene(root, 900, 700);
        stage.setTitle("Cheating Detection System");
        stage.setScene(scene);
        stage.show();

        startCamera(cameraView, status);
    }

    private void startCamera(ImageView view, Label status) {
        camera = new VideoCapture(0);

        new Thread(() -> {
            Mat frame = new Mat();

            if (!camera.isOpened()) {
                Platform.runLater(() -> status.setText("Camera not found!"));
                return;
            }

            while (true) {
                if (camera.read(frame)) {

                    MatOfRect faces = new MatOfRect();
                    faceDetector.detectMultiScale(frame, faces);

                    int totalEyes = 0;

                    for (Rect face : faces.toArray()) {

                        Imgproc.rectangle(frame,
                                new Point(face.x, face.y),
                                new Point(face.x + face.width, face.y + face.height),
                                new Scalar(0, 255, 0), 2);

                        int currentX = face.x;
                        if (prevFaceX != -1) {
                            int movement = Math.abs(currentX - prevFaceX);
                            if (movement > 20) {
                                movementCounter++;
                            }
                        }
                        prevFaceX = currentX;

                        Mat faceROI = frame.submat(face);
                        MatOfRect eyes = new MatOfRect();
                        eyeDetector.detectMultiScale(faceROI, eyes);

                        totalEyes += eyes.toArray().length;

                        for (Rect eye : eyes.toArray()) {
                            Imgproc.rectangle(faceROI,
                                    new Point(eye.x, eye.y),
                                    new Point(eye.x + eye.width, eye.y + eye.height),
                                    new Scalar(255, 0, 0), 2);
                        }
                    }

                    Image image = matToImage(frame);

                    int faceCount = faces.toArray().length;
                    int eyeCount = totalEyes;
                    int movement = movementCounter;

                    Platform.runLater(() -> {
                        view.setImage(image);

                        String message;

                        if (faceCount == 0) {
                            message = "⚠ No Face Detected!";
                        } else if (eyeCount < 2) {
                            message = "⚠ Suspicious: Eyes not visible!";
                        } else if (movement > 10) {
                            message = "⚠ Suspicious: Looking Around!";
                        } else {
                            message = "✅ Normal";
                        }

                        status.setText(message);

                        if (!message.equals("✅ Normal")) {
                            long currentTime = System.currentTimeMillis();

                            if (currentTime - lastCaptureTime > 3000) {
                                saveLog(message);
                                saveScreenshot(frame);
                                lastCaptureTime = currentTime;
                            }
                        }
                    });

                    if (movementCounter > 50) {
                        movementCounter = 0;
                    }
                }
            }
        }).start();
    }

    private void saveLog(String message) {
        try {
            String logPath = basePath + "\\logs\\";
            java.io.File dir = new java.io.File(logPath);
            if (!dir.exists()) dir.mkdirs();

            FileWriter writer = new FileWriter(logPath + "log.txt", true);
            writer.write(LocalDateTime.now() + " : " + message + "\n");
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveScreenshot(Mat frame) {
        String imgPath = basePath + "\\screenshots\\";

        java.io.File dir = new java.io.File(imgPath);
        if (!dir.exists()) dir.mkdirs();

        String filename = imgPath + System.currentTimeMillis() + ".png";
        Imgcodecs.imwrite(filename, frame);
    }

    private Image matToImage(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    public static void main(String[] args) {
        launch();
    }
}