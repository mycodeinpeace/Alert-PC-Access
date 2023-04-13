package com.security.notifypcaccess.systemcapture;

import java.sql.Timestamp;

import org.jctools.queues.MpscArrayQueue;

import com.security.notifypcaccess.main.SystemMonitorEvent;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemHookForKeyBoard implements Runnable {
	private static boolean run = true;
	MpscArrayQueue<SystemMonitorEvent> sharedEventQueue;
	String loggedKeys = "";

	public SystemHookForKeyBoard(MpscArrayQueue<SystemMonitorEvent> sharedeventqueue) {
		this.sharedEventQueue = sharedeventqueue;
	}

	public void run() {
		addKeyboardHook(sharedEventQueue);
	}

	public void addKeyboardHook(final MpscArrayQueue<SystemMonitorEvent> sharedEventQueue) {
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);
		logger.setUseParentHandlers(false);

		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException e) {
			System.err.println("Failed to register native hook");
			System.err.println(e.getMessage());
			System.exit(1);
		}

		GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
			@Override
			public void nativeKeyPressed(NativeKeyEvent e) {
				// Handle key pressed events here
			}

			@Override
			public void nativeKeyReleased(NativeKeyEvent e) {
				Timestamp now = new Timestamp(System.currentTimeMillis());
				SystemMonitorEvent monitorEvent = new SystemMonitorEvent(now, "keyboard", NativeKeyEvent.getKeyText(e.getKeyCode()));
				sharedEventQueue.offer(monitorEvent);
			}

			@Override
			public void nativeKeyTyped(NativeKeyEvent e) {
				// Handle key typed events here
			}
		});

		try {
			while (run) {
				Thread.sleep(128);
			}
		} catch (InterruptedException e) {
			// Do nothing
		} finally {
			try {
				GlobalScreen.unregisterNativeHook();
			} catch (NativeHookException e) {
				System.err.println("Failed to unregister native hook");
				System.err.println(e.getMessage());
			}
		}
	}
}
