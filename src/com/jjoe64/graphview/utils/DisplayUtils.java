package com.jjoe64.graphview.utils;

import java.util.Calendar;

public class DisplayUtils {
	public static final int ONE_SECOND = 1000;
	public static final int ONE_MINUTE = ONE_SECOND * 60;
	public static final int ONE_HOUR = ONE_MINUTE * 60;
	public static final int ONE_DAY = ONE_HOUR * 24;

	private static final String MINUTES_DISPLAY = "mm";
	private static final String HOUR_DISPLAY = "HH'h'";
	private static final String DAY_DISPLAY = "dd/MM";
	private static final String MONTH_DISPLAY = "MM/yy";
	private static final String YEAR_DISPLAY = "yyyy";

	public static final int LEVEL_MINUTE = 0;
	public static final int LEVEL_HOUR = 1;
	public static final int LEVEL_DAY = 2;
	public static final int LEVEL_MONTH = 3;
	public static final int LEVEL_YEAR = 4;

	public static enum DisplayMode {
		//
		MINUTE(MINUTES_DISPLAY, Calendar.HOUR_OF_DAY, 1, LEVEL_MINUTE),
		//
		HOUR(HOUR_DISPLAY, Calendar.HOUR_OF_DAY, 1, LEVEL_HOUR),
		//
		QUARTER_HOUR(HOUR_DISPLAY, Calendar.HOUR_OF_DAY, 4, LEVEL_HOUR),
		//
		DAY(DAY_DISPLAY, Calendar.DAY_OF_MONTH, 1, LEVEL_DAY),
		//
		WEEK(DAY_DISPLAY, Calendar.WEEK_OF_MONTH, 2, LEVEL_DAY),
		//
		MONTH(MONTH_DISPLAY, Calendar.MONTH, 1, LEVEL_MONTH),
		//
		YEAR(YEAR_DISPLAY, Calendar.YEAR, 1, LEVEL_YEAR);

		public String mFormatPattern;
		public int mCalendarField;
		public int mInterval;
		public int mLevel;

		DisplayMode(String formatPattern, int calendarField, int interval, int level) {
			this.mFormatPattern = formatPattern;
			this.mCalendarField = calendarField;
			this.mInterval = interval;
			this.mLevel = level;
		}
	}
}
