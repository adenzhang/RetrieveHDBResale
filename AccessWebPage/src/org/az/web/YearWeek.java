package org.az.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

class YearWeek implements Comparable<YearWeek> {
	int _year;
	int _week;
	public YearWeek() {
	    _year = 0;
	    _week = 0;
	}
	public YearWeek(int year, int week) {
		_year = year;
		_week = week;
	}
	public void setValue(long date) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(date);
		setValue(c);
	}
	public void setValue(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		setValue(c);
	}
	public void setValue(Calendar date) {
		Calendar c = (Calendar)date.clone();
		if( c.get(Calendar.DAY_OF_WEEK) == 7 ) // sunday
			c.add(Calendar.DATE, -1);
		_year = c.get(Calendar.YEAR);
		_week = c.get(Calendar.WEEK_OF_YEAR);
	}
	public YearWeek(long date) {
		Date dt = new Date();
		dt.setTime(date);
		setValue(dt);
	}
	public YearWeek(Date date) {
		setValue(date);
	}
	public YearWeek(Calendar date) {
		setValue(date);
	}
	public void setValue(String date, String format) throws ParseException {
		DateFormat df = new SimpleDateFormat(format);
		Date dt;
    	dt = df.parse(date); 
		setValue(dt);
	}
	public int getYear() {
		return _year;
	}
	public int getWeek() {
		return _week;
	}
	public void setYear(int year) {
		_year = year;
	}
	public void setWeek(int week) {
		_week = week;
	}
	public int compareTo(YearWeek arg0) {
		int w = arg0.getWeek() ;
		int y = arg0.getYear() ;
		if( y == _year ) {
			if( w == _week ) {
			    return 0;	
			}else{
				return w > _week ? 1 : -1;
			}
		}else{
			return y > _year ? 1 : -1;
		}
	}
}