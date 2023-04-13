package com.security.notifypcaccess.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.jctools.queues.MpscArrayQueue;

import com.security.notifypcaccess.alert.TelegramBotNotify;
import com.security.notifypcaccess.systemcapture.JNACapture;
import com.security.notifypcaccess.systemcapture.SystemHookForKeyBoard;

public class Main {

	// A Multi-Producer-Single-Consumer queue based on a org.jctools.queues.ConcurrentCircularArrayQueue. This implies that any thread may call the offer method, but only a single thread may call poll/peek for correctness tomaintained. 
	// This implementation follows patterns documented on the package level for False Sharing protection.
	// This implementation is using the Fast Flow method for polling from the queue (with minor change to correctly publish the index) and an extension of the Leslie Lamport concurrent queue algorithm (originated by Martin Thompson) on the producer side.
	private final MpscArrayQueue<SystemMonitorEvent> sharedEventQueue = new MpscArrayQueue<SystemMonitorEvent>(500);
	private final TelegramBotNotify telegramBot = new TelegramBotNotify();

	// Apps that are in this list will not be alerted.
	private final HashSet<String> whitelistedApps = new HashSet<>();

	long nextPopupTime;
	
	public final static Logger logger = Logger.getLogger(Main.class);
	public Main() {
		nextPopupTime = System.currentTimeMillis();

		//whitelistedApps.add()
	}
	
	public void run() throws IOException, InterruptedException {
		
		telegramBot.alertMessage("Program started.");
		logger.debug("Program started.");
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			  try {
				  
				  telegramBot.alertMessage("Program closed.");
				  logger.debug("Program closed.");
				  
			  } catch (Exception e) { // This exception is fine.
			    throw new RuntimeException(e);
			  }
			}));
		
		
		JNACapture jnaCapture = new JNACapture(sharedEventQueue);
		new Thread(jnaCapture).start();
		
		SystemHookForKeyBoard systemCapture = new SystemHookForKeyBoard(sharedEventQueue);
		new Thread(systemCapture).start();
		
		// Alerting
		while(true) {
			Thread.sleep(4000);
			if (sharedEventQueue.isEmpty()) continue; // There are no events to alert. Continue.
			
			String alertString = getAlertStringToSend(sharedEventQueue, sharedEventQueue.size());
			
			// telegramBot.alertDocument(alertString.getBytes(), "alert", ".txt");
			telegramBot.alertMessage(alertString);
			logger.info(alertString);
			
			/*CameraCapture cameraCapture = new CameraCapture();
			byte[] imageData = cameraCapture.capture();
			
			telegramBot.alertDocument(imageData, "alert", ".jpg");*/
			//FileUtils.writeByteArrayToFile(new File("test.jpg"), imageData);
			
			if (nextPopupTime < System.currentTimeMillis()) {
				JFrame jf=new JFrame();
				jf.setAlwaysOnTop(true);
				JOptionPane.showMessageDialog(jf, "This computer is being monitored. Please do not type any sensitive information.");
				nextPopupTime = nextPopupTime + 600000; // 10 minutes in milliseconds.
			}
		}
	}
		
	public static void main(String[] args) throws IOException, InterruptedException {
		
		Main main = new Main();
		main.run();

		// Do NOT put anything after here, because run ends with an endless loop.
	}
	
	/**
	 * 
	 * @param sharedEventQueue
	 * @param length We also need to pass the length at that time. Otherwise other threads may add items to the queue and we may end up in an infinite loop.
	 * @return
	 */
	private String getAlertStringToSend(MpscArrayQueue<SystemMonitorEvent> sharedEventQueue, int length) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < length; i++) {
			sb.append(sharedEventQueue.poll());
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
	}
}
