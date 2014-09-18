package com.jjoe64.graphview.utils;

import java.util.Calendar;

public class DisplayUtils {
	public static final long ONE_SECOND = 1000;
	public static final long ONE_MINUTE = ONE_SECOND * 60;
	public static final long ONE_HOUR = ONE_MINUTE * 60;
	public static final long ONE_DAY = ONE_HOUR * 24;
	public static final long ONE_MONTH = ONE_DAY * 30;

	private static final String HOUR_DISPLAY = "HH'h'";
	private static final String DAY_DISPLAY = "dd/MM";
	private static final String MONTH_DISPLAY = "MMM";
	private static final String YEAR_DISPLAY = "yyyy";

	public static final int LEVEL_MINUTE = 0;
	public static final int LEVEL_HOUR = 1;
	public static final int LEVEL_DAY = 2;
	public static final int LEVEL_WEEK = 3;
	public static final int LEVEL_MONTH = 4;
	public static final int LEVEL_YEAR = 5;

	public static enum DisplayMode {

		//
		HOUR(HOUR_DISPLAY, Calendar.HOUR_OF_DAY, 1, LEVEL_HOUR, (double) ((double) 1 / 12)),

		//
		// QUARTER_HOUR(HOUR_DISPLAY, Calendar.HOUR_OF_DAY, 4, LEVEL_HOUR),
		//
		DAY(DAY_DISPLAY, Calendar.DAY_OF_MONTH, 1, LEVEL_DAY, 2.5),

		WEEK(DAY_DISPLAY, Calendar.DAY_OF_MONTH, 7, LEVEL_DAY, 8),

		MONTH(MONTH_DISPLAY, Calendar.MONTH, 1, LEVEL_MONTH, 45),
		//
		YEAR(YEAR_DISPLAY, Calendar.MONTH, 12, LEVEL_YEAR, 365),
		//
		MAX(YEAR_DISPLAY, Calendar.MONTH, 12, LEVEL_YEAR, 500);

		public String mFormatPattern;
		public int mCalendarField, mInterval, mLevel;
		public double mFloor;

		public double geTenFloorValue(boolean isMore) {
			final double tenFloor = (double) ((double) mFloor / 10);
			if (isMore) {
				return mFloor + tenFloor;
			} else {
				return mFloor - tenFloor;
			}

		}

		DisplayMode(String formatPattern, int calendarField, int interval, int level, double floor) {
			this.mFormatPattern = formatPattern;
			this.mCalendarField = calendarField;
			this.mInterval = interval;
			this.mLevel = level;
			this.mFloor = floor;
		}
	}
}
