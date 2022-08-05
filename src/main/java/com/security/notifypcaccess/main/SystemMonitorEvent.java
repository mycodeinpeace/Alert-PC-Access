package com.security.notifypcaccess.main;

import java.sql.Timestamp;

public class SystemMonitorEvent {
	
	private Timestamp date; // Time when that event occured
	private String type; // Types under systemcapture module
	private String value; // Captured value

	/**
	 * 
	 * @param date Event date. Pass the current time. Timestamp now = new Timestamp(System.currentTimeMillis());
	 * @param type Event type. Can be jna, keyboard
	 * @param value Event value
	 */
	public SystemMonitorEvent(Timestamp date, String type, String value) {
		super();
		this.date = date;
		this.type = type;
		this.value = value;
	}
	
	public Timestamp getDate() {
		return date;
	}
	public void setDate(Timestamp date) {
		this.date = date;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "SystemMonitorEvent [date=" + date + ", type=" + type + ", value=" + value + "]";
	}
}
