package org.az.web;

import java.util.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ClockRecord {
	private Calendar _date;
	private int _week = -1;
	private int _weekday=-1;
	static private String _df="MM/dd/yyyy HH:mm";
	private String[] _daynames={"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
	private String[] _fulldaynames={"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
	
	static public String getDateTimeFormat() {
		return _df;
	}
	public Calendar getDateTime()
	{
		return _date;
	}
	public String getDateText() {
		DateFormat df= new SimpleDateFormat(_df);
		String t = df.format(_date.getTime());
		return t.substring(0, t.indexOf(" "));
	}
	public String getTimeText() {
		DateFormat df= new SimpleDateFormat(_df);
		String t = df.format(_date.getTime());
		return t.substring(t.indexOf(" "));
	}
	
	public void setDateTimeText(String datetime) throws ParseException {
		DateFormat df= new SimpleDateFormat(_df);
		Calendar c = new GregorianCalendar();
		c.setTime(df.parse(datetime));
		_date = c;
	}
	public String getDateTimeText(){
		DateFormat df= new SimpleDateFormat(_df);
		return df.format(_date.getTime());
	}
	public void setValue(String date, String clock) throws ParseException {
	    setDateTimeText(date + " " + clock);
	}
	public void setValue(int week, String date, String clock) throws ParseException {
	    _week = week;
	    setDateTimeText(date + " " + clock);
	}
	public void setValue(int week, int weekday, String date, String clock) throws ParseException {
	    _week = week;
	    setDateTimeText(date + " " + clock);
	    _weekday = weekday;
	}

	public String getWeekdayName() {
		return _weekday >=0 ? _fulldaynames[_weekday] : "";
	}
	public void setWeekdayName(String weekday) throws ParseException {
		for(int i = 0 ; i < _daynames.length; i++ ){
			if(weekday.startsWith(_daynames[i])) {
				_weekday =  i;
			    return;
			}
		}
//		_weekday = Integer.parseInt(weekday);
		throw(new ParseException("Invalid weekday name", 0));
	}
	public int getWeekday(){
		return _weekday;
	}
	public void setWeek(int v){
		_week = v;
	}
	public int getWeek(){
		return _week;
	}
}
