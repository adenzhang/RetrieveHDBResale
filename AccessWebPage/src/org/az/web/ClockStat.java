package org.az.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ClockStat {
	public static final Calendar BEFORE_LUNCH_CLOCK = new GregorianCalendar(1970, Calendar.JANUARY, 1, 12, 30, 00);
	public static final Calendar AFTER_LUNCH_CLOCK = new GregorianCalendar(1970, Calendar.JANUARY, 1, 13, 15, 00);
	
	public static final long LUNCH_TIMESPAN = 45*60*1000;
	public static final long DAY_WORK_TIMESPAN = (44*3600*1000)/5;
	public static final String CLOCK_FORMAT="HH:mm:ss";
	public static final DateFormat DF_CLOCK = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat DF_DATE = new SimpleDateFormat("MM/dd/yyyy");
	private static final String[] _fullWeekdayNames={"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

	private Calendar date;
	private Calendar onTime;
	private Calendar offTime;
	
	public ClockStat() {
	}
	
	public boolean setClock(Calendar dt){
		Calendar[] d={getOnTime(), getOffTime()};
		
		if(dt == null ) return false;
		
		if( d[0] == null && d[1] == null ) {
			onTime = (Calendar) dt.clone();
			return true;
		}
			
		/// get min & max ;
		Calendar dmin = dt, dmax = dt;
		for(Calendar di : d) {
			if(di == null) continue;
			if(SubOnlyClock(dmin, di) > 0)
				dmin = di;
			if(SubOnlyClock(dmax, di) < 0)
				dmax = di;
			
		}
		
		onTime = (Calendar) dmin.clone();
		offTime = (Calendar) dmax.clone();
		
		return true;
	}
	public boolean canCalculate() {
		if(
				date == null ||
			    onTime == null ||
			    offTime == null )
			return false;
		else
			return true;
	}
	// sub only time clock
	public static int SubOnlyClock(Calendar c1, Calendar c2) {
		int h, m, s, ms;
		h = c1.get(Calendar.HOUR_OF_DAY) - c2.get(Calendar.HOUR_OF_DAY);
		m = c1.get(Calendar.MINUTE) - c2.get(Calendar.MINUTE);
		s = c1.get(Calendar.SECOND) - c2.get(Calendar.SECOND);
		ms = c1.get(Calendar.MILLISECOND) - c2.get(Calendar.MILLISECOND);
		return (3600*h + m*60 + s) * 1000 + ms;
	}
	// sub only date, return days
	public static int SubOnlyDate(Calendar c1, Calendar c2) {
		Calendar t1 = new GregorianCalendar(c1.get(Calendar.YEAR), c1.get(Calendar.MONTH), c1.get(Calendar.DAY_OF_MONTH));
		Calendar t2 = new GregorianCalendar(c2.get(Calendar.YEAR), c2.get(Calendar.MONTH), c2.get(Calendar.DAY_OF_MONTH));
		return (int) (t1.getTimeInMillis() /(24*3600*1000)- t2.getTimeInMillis()/(24*3600*1000));
	}
	public boolean isInSameDate(Calendar c) {
		return SubOnlyDate(date, c)==0;
	}
	/// millisecond based on 00:00:00 AM
	public static long GetOnlyClockInMillis(Calendar c) {
		long h = c.get(Calendar.HOUR_OF_DAY);
		long m = c.get(Calendar.MINUTE);
		long s = c.get(Calendar.SECOND);
		long ms = c.get(Calendar.MILLISECOND);
		return ((h*60 + m)*60+s)*1000 + ms;
	}
	public static Calendar GetClockBase(){
		Calendar c = new GregorianCalendar(1970,Calendar.JANUARY, 1, 0,0);
		return c;
	}
	/// convert to Date based on TimeSpanBase
	public static Calendar ConvertTimeSpan(long timespan){
		Calendar c = GetClockBase();
		c.add(Calendar.MILLISECOND, (int) timespan);
		return c;
	}
	public boolean setValue(ClockRecord cr) {
		if(cr == null ) return false;

		if(date == null) {
		    date = (Calendar) cr.getDateTime().clone();
		}else if(SubOnlyDate(date, cr.getDateTime()) != 0){
			return false;
		}
	    setClock(cr.getDateTime());
	    return true;
	}
	/// @return value < 0 if invalid
	public long getWorkTimeMillis() {
		if( ! canCalculate() )
			return -1;

		if(SubOnlyClock(onTime, BEFORE_LUNCH_CLOCK) < 0 && SubOnlyClock(offTime, AFTER_LUNCH_CLOCK) > 0)
			return SubOnlyClock(offTime, onTime) - LUNCH_TIMESPAN;
		else
			return SubOnlyClock(offTime, onTime);
	}
	/// @return null if invalid
	public Calendar getWorkTimeInCalendar(){
		long t = getWorkTimeMillis();
		if(t<0) return null;
		return ConvertTimeSpan(t);
	}
	public String getWorkTimeText() {
		Calendar c = getWorkTimeInCalendar();
		if( c == null) return "";
		return DF_CLOCK.format(c.getTime());
	}
	public double getWorkTimeRatio() {
		long t = getWorkTimeMillis();
		if( t < 0 ) return t;
		return t*1.0/DAY_WORK_TIMESPAN;
	}
	public Calendar getDate() {
		return date;
	}
	public String getDateText() {
		Calendar c = getDate();
		if( c == null) return "";
		return DF_DATE.format(c.getTime());
		
	}
	/// @return null if invalid
	public Calendar getOffTime() {
		return offTime;
	}
	/// @return null if invalid
	public String getOffTimeText() {
		Calendar c = getOffTime();
		if( c == null) return "";
		return DF_CLOCK.format(c.getTime());
		
	}
	public Calendar getOnTime() {
		return onTime;
	}
	public String getOnTimeText() {
		Calendar c = getOnTime();
		if( c == null) return "";
		return DF_CLOCK.format(c.getTime());
		
	}
	public boolean isWorkDay() {
		int k = date.get(Calendar.DAY_OF_WEEK);
		return !( k == 1 || k == 7);
	}
	public YearWeek getYearWeek(){
		return new YearWeek(date);
	}
	public int getYear() {
		return getYearWeek().getYear();
	}
	public int getWeek() {
		return getYearWeek().getWeek();
	}
	public static String GetWeekDayName(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return _fullWeekdayNames[c.get(Calendar.DAY_OF_WEEK)-1];
	}
	public String getWeekdayName(){
		return GetWeekDayName(date.getTime()); 
	}
}