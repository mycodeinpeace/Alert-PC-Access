package com.security.notifypcaccess.systemcapture;

import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
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
				String appName = getActiveExcecutableName() + " - " + getActiveApplicationName();
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

	   int GetWindowThreadProcessId(WinDef.HWND hWnd, IntByReference pref);
   }

	public interface Kernel32 extends StdCallLibrary {
		Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

		WinNT.HANDLE OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId);

		int GetModuleFileNameExA(WinNT.HANDLE hProcess, WinNT.HANDLE hModule, byte[] lpFilename, int nSize);

		boolean CloseHandle(WinNT.HANDLE hObject);
	}

	public interface Psapi extends StdCallLibrary {
		Psapi INSTANCE = (Psapi) Native.loadLibrary("psapi", Psapi.class);
		int GetModuleFileNameExA(WinNT.HANDLE hProcess, WinNT.HANDLE hModule, byte[] lpFilename, int nSize);
	}

   public String getActiveApplicationName() throws InterruptedException {
      byte[] windowText = new byte[512];

      PointerType hwnd = User32.INSTANCE.GetForegroundWindow();
      User32.INSTANCE.GetWindowTextA(hwnd, windowText, 512);
      return Native.toString(windowText);
   }

	public String getActiveExcecutableName() throws InterruptedException {
		HWND hwnd = User32.INSTANCE.GetForegroundWindow();
		IntByReference processId = new IntByReference();
		User32.INSTANCE.GetWindowThreadProcessId(hwnd, processId);

		WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(
				WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
				false,
				processId.getValue()
		);

		byte[] exePath = new byte[512];
		Psapi.INSTANCE.GetModuleFileNameExA(process, null, exePath, exePath.length);
		Kernel32.INSTANCE.CloseHandle(process);

		return getExecutableName(Native.toString(exePath));
	}

	public String getExecutableName(String fullPath) {
		String pattern = "[\\\\/][^\\\\/]*$";
		Pattern regexPattern = Pattern.compile(pattern);
		Matcher matcher = regexPattern.matcher(fullPath);

		if (matcher.find()) {
			return matcher.group().substring(1);
		} else {
			return fullPath;
		}
	}
}