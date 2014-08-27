package com.jjoe64.graphview.utils;

import java.util.Calendar;

public class DisplayUtils {
	public static final long ONE_SECOND = 1000;
	public static final long ONE_MINUTE = ONE_SECOND * 60;
	public static final long ONE_HOUR = ONE_MINUTE * 60;
	public static final long ONE_DAY = ONE_HOUR * 24;
	public static final long ONE_MONTH = ONE_DAY * 30;

	private static final String MINUTES_DISPLAY = "mm";
	private static final String HOUR_DISPLAY = "HH'h'";
	private static final String DAY_DISPLAY = "dd/MM";
	private static final String MONTH_DISPLAY = "MM/yy";

	public static final int LEVEL_MINUTE = 0;
	public static final int LEVEL_HOUR = 1;
	public static final int LEVEL_DAY = 2;
	public static final int LEVEL_MONTH = 3;

	public static enum DisplayMode {
		//
		QUARTER_HOUR(HOUR_DISPLAY, Calendar.HOUR_OF_DAY, 4, LEVEL_HOUR, 4 * ONE_HOUR),
		//
		DAY(DAY_DISPLAY, Calendar.DAY_OF_MONTH, 1, LEVEL_DAY, ONE_DAY),
		//
		DAY_2(DAY_DISPLAY, Calendar.DAY_OF_MONTH, 2, LEVEL_DAY, 2 * ONE_DAY),
		//
		WEEK(DAY_DISPLAY, Calendar.DAY_OF_MONTH, 7, LEVEL_DAY, 7 * ONE_DAY),
		//
		WEEK_2(DAY_DISPLAY, Calendar.DAY_OF_MONTH, 15, LEVEL_DAY, 15 * ONE_DAY),
		//
		MONTH(MONTH_DISPLAY, Calendar.MONTH, 1, LEVEL_MONTH, ONE_MONTH);

		public String mFormatPattern;
		public int mCalendarField, mInterval, mLevel;
		public long deepInterval;

		DisplayMode(String formatPattern, int calendarField, int interval, int level, long deepInterval) {
			this.mFormatPattern = formatPattern;
			this.mCalendarField = calendarField;
			this.mInterval = interval;
			this.mLevel = level;
			this.deepInterval = deepInterval;
		}
	}
}
