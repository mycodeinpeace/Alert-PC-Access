package com.security.notifypcaccess.systemcapture;

import java.sql.Timestamp;

import org.jctools.queues.MpscArrayQueue;

import com.security.notifypcaccess.main.SystemMonitorEvent;
import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.*;

public class JNACapture  implements Runnable {
	
	MpscArrayQueue<SystemMonitorEvent> sharedEventQueue;
	
	private String lastApplicationName;
	
	public JNACapture(MpscArrayQueue<SystemMonitorEvent> sharedeventqueue) {
		this.sharedEventQueue = sharedeventqueue;
		lastApplicationName = "";
	}

	public void run() {
		
		while (true) {
			try {
				String appName = getActiveApplicatoinName();
				if (!appName.equalsIgnoreCase(lastApplicationName)) { // Alert if the screen changes.
					Timestamp now = new Timestamp(System.currentTimeMillis());
					SystemMonitorEvent event = new SystemMonitorEvent(now, "jna", appName);
					sharedEventQueue.offer(event);
					//System.out.println(appName);
					lastApplicationName = appName;
				}
				
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

   public interface User32 extends StdCallLibrary {
      @SuppressWarnings("deprecation")
      User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);
      HWND GetForegroundWindow();
      int GetWindowTextA(PointerType hWnd, byte[] lpString, int nMaxCount);
   }

   public String getActiveApplicatoinName() throws InterruptedException {
      byte[] windowText = new byte[512];

      PointerType hwnd = User32.INSTANCE.GetForegroundWindow();
      User32.INSTANCE.GetWindowTextA(hwnd, windowText, 512);
      return Native.toString(windowText);
   }
}