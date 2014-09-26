package org.az.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;

import java.util.Scanner;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

//import org.apache.commons.httpclient.*;

public class AsmClockPage {

	private final String url = "http://atsnts55/weekreport/indiv/Clock.aspx";
	private String      _domain="atsex";
	private String      _hostName="peejzhang";
	private String      _userName=null;
	private String      _password=null;
	
//	private LoginBean  _login; 
	
	private final String DELIM="\t";
	private List<ClockRecord> lstClock = new ArrayList<ClockRecord>();
	
	private void handleTable(HtmlTable hTable){
		Iterable<DomElement> hList = hTable.getChildElements();
		int i = 0;
		String ts, ts1;
		for(DomElement hElm : hList ) {
			if( hElm.getTagName().compareTo("tr") != 0) continue;
			++i;
			if(i == 1 || i > 21) continue;
			Iterable<DomElement> hList2 = hElm.getChildElements();
			Iterator<DomElement> hIt = hList2.iterator(); 
		    ClockRecord cr = new ClockRecord();
		    
		    Integer.parseInt(hIt.next().getTextContent());  // sequence
		    try {
			    cr.setWeek(Integer.parseInt(hIt.next().getTextContent())); // week
			    cr.setWeekdayName(hIt.next().getTextContent());  // week day
			    ts = hIt.next().getTextContent();
			    ts1 = hIt.next().getTextContent();
			    cr.setValue(ts, ts1);
		    }catch(Exception e) {
		        e.printStackTrace();
		        continue;
		    }
		    
		    lstClock.add(cr);
		    
		}
	}
//	public void setLogin(LoginBean value) {
//		String a = value.getDomain(); 
//		if(a!=null && a != "") {
//			_domain = a;
//		}
//		a = value.getPassword();
//		if(a!=null && a != "") {
//			_password = a;
//		}
//		a = value.getUserName();
//		if(a!=null && a != "") {
//			_userName = a;
//		}
//
//	}
	public void clear() {
		setUserName(null);
		setPassword(null);
		lstClock.clear();
	}
	public String logout() {
		clear();
		return "SUCCESS";
	}
	public void setDomain(String v) {
		_domain = v;
	}
	public void setUserName(String v) {
		_userName = v;
	}
	public String getUserName(){
		return _userName;
	}
	public void setPassword(String v) {
		_password = v;
	}
	public String getPassword(){
		return "";
	}
	public List<ClockRecord> getClockRecordList() {
		return lstClock;
//		ClockRecord[] a=null;
//		return lstClock.toArray( a );
	}
	public ClockRecord[] getClockRecords() {
		if(lstClock.size() == 0) return null;
		ClockRecord[] a= new ClockRecord[lstClock.size()];
		a = lstClock.toArray( a );
		return a;
	}
	public int getClockCount(){
		return lstClock.size();
	}
	public String submitForm()
	{
		HtmlPage page, page2;
		
		/// init result table
		lstClock.clear();
		
		WebClient client = new WebClient(BrowserVersion.FIREFOX_24);
		if( _userName != null && _password != null) {
			final DefaultCredentialsProvider credentialsProvider = new DefaultCredentialsProvider();
			credentialsProvider.addNTLMCredentials(_userName, _password, null, -1, _hostName, _domain);
			client.setCredentialsProvider(credentialsProvider);
		}
		
		client.getOptions().setRedirectEnabled(true); 

		try{
			page = client.getPage(url);
		}
		catch(FailingHttpStatusCodeException e) {  //authentication fail
			return "FAIL_GETPAGE";
		}catch(Exception e){
			e.printStackTrace();
		    return "FAIL_GETPAGE";
		}
		System.out.println(page.getTitleText());
		
		HtmlForm form;
		
		form = page.getForms().get(0);
		assert(null!=form);

		HtmlTable hTable = page.getHtmlElementById("DataGrid1");
		assert(null!=hTable);
		
		handleTable(hTable);
		
		
		for( DomElement hElm : hTable.getChildElements() ) {
		    if(hElm.getTagName().equalsIgnoreCase("a")){
		    	HtmlAnchor hAnc=(HtmlAnchor) hElm;
		    	try {
		    	    page2 = hAnc.click();
		    	}catch(Exception e){
		    		e.printStackTrace();
		    		continue;
		    	}
		    	hTable = page2.getHtmlElementById("DataGrid1");
		    	assert(null!=hTable);
		    	handleTable(hTable);
		    }
		}
		if(lstClock.size() == 0) {
			return "FAIL_GETDATA";
		}

		return "SUCCESS";
	}//submitForm()
	
	public ClockCompute getClockCompute() {
		ClockCompute cc = new ClockCompute(getClockRecordList());
		return cc;
	}

	public class InputParam extends JDialog implements ActionListener {
		private JTextField txtName;
		private JPasswordField txtPassword;
		private JButton   btnOK, btnCancel;
		
		public InputParam(JFrame parent, String title) {
			super(parent, title);
//			super("Input User Name and Password");
			this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			JTextField txt;
			
			Container container = new Container();
			container.setLayout(new FlowLayout());
			
			txt = new JTextField("UserName:");
			txt.setEditable(false);
			container.add(txt);
			
			txt = new JTextField(10);
			txt.setEditable(true);
			container.add(txt);
			
			txt = new JTextField("Password:");
			txt.setEditable(false);
			container.add(txt);

			txt = new JTextField(10);
			txt.setEditable(true);
			container.add(txt);

			btnOK = new JButton("OK");
			btnOK.addActionListener(this);
			container.add(btnOK);
			
			btnCancel = new JButton("Cancel");
			btnCancel.addActionListener(this);
			container.add(btnCancel);
			
			setSize(500,100);
			setVisible(true);
		}
		public void actionPerformed(ActionEvent event) {
			Object ctrl = event.getSource();
			if( ctrl == btnOK ) {
				this.setVisible(false);
				dispose();
			}
		}
	}
	/**
	 * @param args
	 */
	public static void run(String[] args) {

		String name;
		String psw;
		
		DateFormat df = new SimpleDateFormat("yyyyMMdd_hhmmss");
		String  sNow = df.format(new Date());
		
		if( args.length < 2) {
			JTextField    nm = new JTextField();
			JPasswordField pwd = new JPasswordField();
			Object[] message = { "User Name: ", nm, "\nPassword:", pwd };
			if( args.length == 1 )
				nm.setText(args[0]);
			int resp = JOptionPane.showConfirmDialog(null, message, "Retrieve Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(resp == JOptionPane.OK_OPTION) {
			    psw = new String(pwd.getPassword());
			    name = nm.getText();
			}else{
				return;
			}
		}else{
			name = args[0];
			psw = args[1];
		}

		AsmClockPage web = new AsmClockPage();
		web.setPassword(psw);
		web.setUserName(name);
		
		BufferedWriter out=null;

		web.submitForm();
		
		try{
			out = new BufferedWriter(new FileWriter("AsmClockPage-"+sNow+".txt"));
//			out.write("" + web.getClockRecords().size()+"\n");
			for( ClockRecord cr: web.getClockRecords() ) {
			    out.write(String.format("%2d%s%s%s%s%s%s\n",
			    		cr.getWeek(), web.DELIM,
			    		cr.getWeekdayName(), web.DELIM,
			    		cr.getDateText(), web.DELIM,
			    		cr.getTimeText()
			    		));
			}
		    out.close();
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
		ClockCompute cc = new ClockCompute(web.getClockRecordList());
		
		String space = "----------";
		try{
			out = new BufferedWriter(new FileWriter("AsmClockPage_WorkTime-"+sNow+".txt"));
			DateFormat dfDate = new SimpleDateFormat("yyyy/MM/dd");
			DateFormat dfClock = new SimpleDateFormat("HH:mm:ss");
//			out.write("" + cc.getValidClockStats().size()+"\n");
			for( ClockStat cs: cc.getClockStats()) {
			    out.write(String.format("%4d%s%2d%s%s\n",
			    		cs.getYearWeek().getYear(), web.DELIM,
			    		cs.getYearWeek().getWeek(), web.DELIM,
			    		cs.getWeekdayName()+ web.DELIM
			    		+ dfDate.format(cs.getDate().getTime())+ web.DELIM
			    		+ (cs.getOnTime()==null? space:dfClock.format(cs.getOnTime().getTime())) + web.DELIM
			    		+ (cs.getOffTime()==null? space:dfClock.format(cs.getOffTime().getTime())) + web.DELIM
			    		+ (cs.getWorkTimeInCalendar()==null? space:dfClock.format(cs.getWorkTimeInCalendar().getTime())) + web.DELIM
			    		+ cs.getWorkTimeRatio()
			    		));
			}
		    out.close();
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
		}

		try{
			out = new BufferedWriter(new FileWriter("AsmClockPage_Ratio-"+sNow+".txt"));
			for( YearWeek w: cc.getYearWeeks()) {
			    out.write(String.format("%4d%s%2d%s%4s\n",
			    		w.getYear(), web.DELIM,
			    		w.getWeek(), web.DELIM,
			    		cc.getWeekWorkTimeRatio(w)
			    		));
			}
		    out.close();
		}catch(Exception e){
			System.err.println("Error: " + e.getMessage());
		}

        System.out.println("The End.");
	}

}
