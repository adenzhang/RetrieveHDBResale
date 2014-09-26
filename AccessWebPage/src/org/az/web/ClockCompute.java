package org.az.web;

import java.util.*;

/// regard half day work as a whole day work 
public class ClockCompute {

	private List<ClockStat> _clockStat = new ArrayList<ClockStat>();
	
	public double getWeekWorkTimeRatio(YearWeek aweek) {
		List<ClockStat> crs = getClockStatsByWeek(aweek);
		long sumtime=0;
		int  numDays = 0;
		for(ClockStat cs : crs) {
			if(! cs.canCalculate() ) continue;
			long L = cs.getWorkTimeMillis();
			if( L < 0 ) continue;
			sumtime += L;
			if( cs.isWorkDay() )
			    numDays ++;
		}
		if(sumtime == 0 )
			return -1.0;
//		System.out.println(""+ aweek.getWeek() + "\t"+ numDays + "\t" + sumtime/1000);
		return sumtime*1.0/(numDays*ClockStat.DAY_WORK_TIMESPAN);
	}
	public static boolean isInSameDate(Calendar c1, Calendar c2) {
		
		if(    c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
			&& c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
			&& c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH)
		  ) {
			return true;
		}else
			return false;
	}
	
	public List<ClockStat> getClockStats() {
		return _clockStat;
	}
	public List<ClockStat> getValidClockStats() {
		List<ClockStat> css = new ArrayList<ClockStat>();
		for(ClockStat cs: _clockStat) {
			if(cs.canCalculate())
				css.add(cs);
		}
		return css;
	}
	public List<ClockStat> getClockStatsByWeek(YearWeek week) {
		List<ClockStat> css = new ArrayList<ClockStat>();
		for(ClockStat cs : _clockStat) {
			if(cs.getYearWeek().compareTo(week) == 0 ) {
				css.add(cs);
			}
		}
		return css;
	}
	public class YearWeekRatio {
		YearWeek _yw;
		double   _r;
		public YearWeekRatio(int year, int week, double ratio) {
			_yw = new YearWeek(year, week);
			_r = ratio;
		}
		public int getYear() {
			return _yw.getYear();
		}
		public int getWeek() {
			return _yw.getWeek();
		}
		public double getRatio() {
			return _r;
		}
	}
	public YearWeekRatio[] getYearWeekRatios() {
		YearWeek[] yw = getYearWeeks();
		YearWeekRatio[]   r= new YearWeekRatio[yw.length];
		for(int k = 0 ;k < yw.length; k ++) {
			r[k] = new YearWeekRatio(yw[k].getYear(), yw[k].getWeek(), getWeekWorkTimeRatio(yw[k]));
		}
		return r;
	}
	/// @return sorted array
	public YearWeek[] getYearWeeks() {
		SortedSet<YearWeek> weeks = new TreeSet<YearWeek>();
		for(ClockStat cs: _clockStat) {
			YearWeek k = cs.getYearWeek();
			weeks.add(k);
		}
		YearWeek[] ret = new YearWeek[weeks.size()];
		ret = weeks.toArray(ret);
		return ret;
	}
	public ClockStat getClockStatByDate(Calendar dt) {
		for(ClockStat cs : _clockStat) {
			if( isInSameDate(cs.getDate(), dt))
				return cs;
		}
		return null;
	}
	public void setClockRecords(List<ClockRecord> crs) {
	    
	    ClockStat cs ;
	    _clockStat.clear();
	    for(ClockRecord c: crs) {
	    	cs = getClockStatByDate(c.getDateTime());
	    	if(cs == null) {
	    	    cs = new ClockStat();
	    	    cs.setValue(c);
	    	    _clockStat.add(cs);
	    	}else{
	    		cs.setValue(c);
	    	}
	    }
//	    System.out.println("All: " + crs.size()+ "\t Add ClockRecords: " + _clockStat.size() + "\t Valid:" + getValidClockStats().size() );
	}
	public ClockCompute(List<ClockRecord> crs) {
		setClockRecords(crs);
	}
}
