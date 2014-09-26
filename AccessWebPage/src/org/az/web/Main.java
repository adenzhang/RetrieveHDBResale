package org.az.web;

import java.awt.Component;
import java.awt.Container;
import java.lang.String;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class Main {

    private static void prt(String s) {
        System.out.println(s);
    }
    private void testDateFormat() {
//		Date d = new Date();
//		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//		long tm=0;
//		try {
//		    d = df.parse("2009/04/15 00:00:02");
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//		tm = d.getTime() ;
//		System.out.println(String.format("%d", tm));
//		Calendar c = Calendar.getInstance();
//		c.setTimeInMillis(tm);
//		long h = c.get(Calendar.YEAR);
//		long m = c.get(Calendar.MONTH);
//		long s = c.get(Calendar.DAY_OF_MONTH);
//		long ms = c.get(Calendar.MILLISECOND);
//		System.out.println(String.format("%d:%d:%d:%d", h, m, s, ms));
//		
//		d.setTime(tm);
//		System.out.println(df.format(d));
		
//		boolean b;
//		SortedSet<YearWeek> aL = new TreeSet<YearWeek>();
//		b = aL.add(new YearWeek(2009, 1));
//		aL.add(new YearWeek(2009, 2));
//		if(! aL.contains(new YearWeek(2009, 1)))
//			aL.add(new YearWeek(2009, 1));
//		aL.add(new YearWeek(2009, 2));
//		for(YearWeek k: aL) {
//			System.out.println("" + k.getYear() + " " + k.getWeek());
//		}

//		Calendar c = new GregorianCalendar(2009,Calendar.APRIL,12, 13, 0);
//		prt("" + ClockStat.ConvertTimeSpan(3600*1000));
//		c.set(2009, Calendar.APRIL, 16);
//        prt("  YEAR                 : " + c.get(Calendar.YEAR));
//        prt("  MONTH                : " + c.get(Calendar.MONTH));
//        prt("  DAY_OF_MONTH         : " + c.get(Calendar.DAY_OF_MONTH));
//        prt("  DAY_OF_WEEK          : " + c.get(Calendar.DAY_OF_WEEK));
//        prt("  DAY_OF_YEAR          : " + c.get(Calendar.DAY_OF_YEAR));
//        prt("  WEEK_OF_YEAR         : " + c.get(Calendar.WEEK_OF_YEAR));
//        prt("  WEEK_OF_MONTH        : " + c.get(Calendar.WEEK_OF_MONTH));
//        prt("  DAY_OF_WEEK_IN_MONTH : " + c.get(Calendar.DAY_OF_WEEK_IN_MONTH));
		
//		String[] choices = {"Retrieve Clock", "Retrieve SgProperty Sales Record", "Exit"};
//		int   opt;
//		opt = JOptionPane.showOptionDialog(null, "What do you want to do?", "Choose one",
//				JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, "Exit");
//		switch( opt ) {
//		case 0:
//			AsmClockPage.run(new String[] {"peejzhang"});
//			break;
//		case 1:
//			SgPropertyResalePage.run(null);
//			break;
//		case 2:
//		case -1:
//			return;
//		default:
//			return;
//		}	
    }
    public static void addComponentsToPane(Container pane) {
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

        addAButton("Button 1", pane);
        addAButton("Button 2", pane);
        addAButton("Button 3", pane);
        addAButton("Long-Named Button 4", pane);
        addAButton("5", pane);
    }

    private static void addAButton(String text, Container container) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(button);
    }
    public static void testUi(){
        //Create and set up the window.
        JFrame frame = new JFrame("BoxLayoutDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Set up the content pane.
        addComponentsToPane(frame.getContentPane());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String s = "May 2010";
//		String s1 = SgPropertyResalePage.dbDateFormat(s);
        
		MyGui ui = new MyGui(new SgPropertyResalePage());
	}

}
