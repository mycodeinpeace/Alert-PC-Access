package com.security.notifypcaccess.systemcapture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.jctools.queues.MpscArrayQueue;

import com.security.notifypcaccess.main.SystemMonitorEvent;
import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.*;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;

public class JNACapture  implements Runnable {
	
	MpscArrayQueue<SystemMonitorEvent> sharedEventQueue;
	private String lastApplicationName;
	
	public JNACapture(MpscArrayQueue<SystemMonitorEvent> sharedeventqueue) {
		this.sharedEventQueue = sharedeventqueue;
		lastApplicationName = "";
	}

	public void run() {

		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
		System.out.println("isWindows: " + isWindows);

		while (true) {
			try {
				String appName;
				if (isWindows) {
					appName = getActiveExecutableNameWindows() + " - " + getActiveApplicationNameWindows();
				} else {
					appName = getActiveApplicationNameMac();
				}

				if (appName == null) continue;

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

   public String getActiveApplicationNameWindows() throws InterruptedException {
      byte[] windowText = new byte[512];

      PointerType hwnd = User32.INSTANCE.GetForegroundWindow();
      User32.INSTANCE.GetWindowTextA(hwnd, windowText, 512);
      return Native.toString(windowText);
   }

	public String getActiveExecutableNameWindows() throws InterruptedException {
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

	public interface MacOSXUtils extends CoreFoundation {
		MacOSXUtils INSTANCE = Native.loadLibrary("CoreFoundation", MacOSXUtils.class);

		Pointer kCFAllocatorDefault = INSTANCE.CFAllocatorGetDefault().getPointer();

		CFArrayRef CFRunLoopCopyAllModes(Pointer rl);
	}

	public static String getActiveApplicationNameMac() {
		String appName = "";
		String[] script = {
				"osascript",
				"-e", "tell application \"System Events\"",
				"-e", "set frontApp to first application process whose frontmost is true",
				"-e", "set appName to name of frontApp",
				"-e", "set windowName to name of first window of frontApp",
				"-e", "return appName & \", \" & windowName",
				"-e", "end tell"
		};

		try {
			Process process = new ProcessBuilder(script).start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			appName = reader.readLine();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return appName;
	}

	public static String executeShellCommand(String command) {
		StringBuilder output = new StringBuilder();

		try {
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output.toString().trim();
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