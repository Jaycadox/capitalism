package xyz.jayphen.capitalism.helpers;

import java.util.ArrayList;

public class TimeHelper {
	public static String timeToString(long time) {
		time /= 1000;
		long days = time / 60 / 60 / 24;
		long hours = ((time - (days * (60*60*24))) / 60 / 60);
		long mins = ((time - (days * (60*60*24)) - (hours*(60*60))) / 60);
		long seconds = ((time - (days * (60*60*24)) - (hours*(60*60)) - (mins * (60))));
		ArrayList<String> parts = new ArrayList<>();
		if(days != 0) {
			parts.add(days + " day" + (days == 1 ? "" : "s"));
		}
		if(hours != 0) {
			parts.add(hours + " hour" + (hours == 1 ? "" : "s"));
		}
		if(mins != 0) {
			parts.add(mins + " minute" + (mins == 1 ? "" : "s"));
		}
		if(seconds != 0) {
			parts.add((parts.isEmpty() ? "" : "and ") + seconds + " second" + (seconds == 1 ? "" : "s"));
		}
		return String.join(", ", parts);
	}
	public static long toTime(String s) {
		s += " ";
		int days = 0;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;
		StringBuilder buffer = new StringBuilder();
		for(char c : s.toCharArray()) {
			buffer.append(c);
			if(c == ' ') {
				if(buffer.toString().endsWith("d ")) {
					days += Integer.parseInt(buffer.substring(0, buffer.length() - 2));
				}
				if(buffer.toString().endsWith("h ")) {
					hours += Integer.parseInt(buffer.substring(0, buffer.length() - 2));
				}
				if(buffer.toString().endsWith("min ")) {
					minutes += Integer.parseInt(buffer.substring(0, buffer.length() - 4));
				}
				if(buffer.toString().endsWith("s ")) {
					seconds += Integer.parseInt(buffer.substring(0, buffer.length() - 2));
				}
				buffer = new StringBuilder();
			}
		}
		return ((hours * (60 * 60)) + (days * (24 * 60 * 60)) + (minutes * 60L) + (seconds)) * 1000;
	}
	public static ArrayList<String> splitTime(String s) {
		ArrayList<String> parts = new ArrayList<>();
		StringBuilder first = new StringBuilder();
		StringBuilder buffer = new StringBuilder();
		for(char c : s.toCharArray()) {
			buffer.append(c);
			if(c == ' ') {
				if(!buffer.toString().endsWith("min") & !(buffer.toString().endsWith("s")) & !(buffer.toString().endsWith("h")) & !(buffer.toString().endsWith("d"))) {
					parts.add(first.toString());
					break;
				}
				buffer = new StringBuilder();
			}
			first.append(c);
		}
		if(parts.isEmpty()) {
			parts.add(first.toString());
		}
		parts.add(s.replaceFirst(first.toString(), "").trim());
		return parts;
	}
}
