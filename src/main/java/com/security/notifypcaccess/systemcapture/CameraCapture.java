package com.security.notifypcaccess.systemcapture;

import java.io.File;
import java.io.IOException;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;        
import org.opencv.videoio.VideoCapture;        


/**
 * 
 * @author Deniz
	OpenCV setup
	https://davutdenizyavuz.atlassian.net/wiki/spaces/MINERPROGR/pages/2228284/OpenCV
 */
public class CameraCapture {
	
	public CameraCapture() {
		File file = new File("./Opencv-4.5.4/opencv/build/java/x64/opencv_java454.dll");
    	System.load(file.getAbsolutePath());
	}
    
    public byte[] capture() throws IOException {

    	// Start camera
    	VideoCapture capture = new VideoCapture(0);
    	
    	// Store image as 2D matrix
    	Mat image = new Mat();
        byte[] imageData;
  
        while(true) {
            // read image to matrix
            capture.read(image);
  
            // convert matrix to byte
            final MatOfByte buf = new MatOfByte();
            Imgcodecs.imencode(".jpg", image, buf);
  
            imageData = buf.toArray();
            
            image.release();
            capture.release();
            return imageData;
        }
    }
}
