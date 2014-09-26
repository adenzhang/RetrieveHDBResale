/**
 * History
 * @az 20111003: added the following:
 * 		client.setThrowExceptionOnFailingStatusCode(false);
 *		client.setThrowExceptionOnScriptError(false);
 *
 */

package org.az.web;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;

import java.sql.SQLException;
import java.sql.Statement;

public class SgPropertyResalePage {
	public static final String version = "20140926"; // 

	public static interface IRetrieveListerner{
		public void prt(String fmt, Object... args);
		
		public void onInitPageBegin();
		public void onInitPageEnd();
		public void onRetrieveBegin();
		public void onOnePageRetrieved(ArrayList<ArrayList<String>> records);
		public void onRetrieveEnd();
	}
	public IRetrieveListerner retrieveListener;
	
	final String url1 = "http://www.hdb.gov.sg/bb33/ispm051p.nsf/Search";
	final String url = "http://services2.hdb.gov.sg/webapp/BB33RTIS/BB33PReslTrans.jsp";
	final String DELIM="\t";
	
	final String uiFlatType = "FLAT_TYPE";
	final String uiHDBTown = "NME_NEWTOWN";
	final String uiStartDate = "DTE_APPROVAL_FROM";
	final String uiEndDate = "DTE_APPROVAL_TO";
	final String uiDateFormat = "MMM yyyy";
	
	public boolean      _webFormatError = false;
	
	public boolean bStoreDb, bStoreFile;
	public boolean bConcurrent;
	
	private boolean inited;
	private volatile boolean _stop;
	public void stop(boolean yes){
		_stop = yes;
	}
	public boolean stopped(){
		return _stop;
	}
	
	final String dbName = "sghdbresale.db";
	private java.sql.Connection _dbConn;
	final String dbMainTable = "sghdbresale";
	final String dbTempTable = "latestdata";
	final String[] columns = {"hdbtown", "flattype", "block", "streetname", "storey", "floorarea", "flatmodel", "builtyear", "price", "resaledate"};
	final Dictionary<String, Integer> colIndex = new Hashtable<String, Integer>(columns.length);
// 'hdbtown', 'flattype', 'block', 'streetname', 'storey', 'floorarea', 'flatmodel', 'builtyear', 'price', 'resaledate'
// hdbtown, flattype, block, streetname, storey, floorarea, flatmodel, builtyear, price, resaledate
//	final String dbColumns = "(hdbtown varchar(20), flattype varchar(20), block varchar(20), streetname varchar(20), " +
//			"storey varchar(20), floorarea varchar(20), flatmodel varchar(20), builtyear varchar(20), price varchar(20), resaledate varchar(20))";

	final NumberFormat currencyFmt = NumberFormat.getCurrencyInstance(Locale.US);
	
	final static DateFormat rawDateFmt = new SimpleDateFormat("MMM yyyy");
	final static DateFormat dbDateFmt = new SimpleDateFormat("yyyy/MM");
	public static String months[]={"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep","Oct", "Nov", "Dec"};
	public static int parseMonth(String s){
		for(int i = 0;i<months.length;++i){
			if(0 <= s.indexOf(months[i]))
				return i + 1;
		}
		return 0;
	}
	static String dbDateFormat(String raw){
		Date d ;
		try{
			d = rawDateFmt.parse(raw);
		}catch(ParseException e){
			int m = parseMonth(raw.substring(0, 3));
			if( m == 0) return raw;
			try{
				int y = Integer.parseInt(raw.substring(4, raw.length()));
				return String.format("%04d/%02d", y, m);
			}catch(NumberFormatException e1){
				return raw;
			}
		}
		return dbDateFmt.format(d);
	}
	
	BufferedWriter out;
	
	Object _dblock = new Object(), _datlock=new Object(), _prtlock=new Object();
	
	SgPropertyResalePage(){
		inited = false;
		bStoreDb = true;
		bStoreFile = true;
		for(int i=0;i<columns.length;++i){
			colIndex.put(columns[i], i);
		}
	}
	
	public void prt(String fmt, Object... args) {
		synchronized(_prtlock){
			if(retrieveListener == null){
				System.out.printf(fmt, args);
				System.out.printf("\n");
			}else{
				retrieveListener.prt(fmt, args);
			}
		}
	}
	
	public static <T>String join(final AbstractCollection<T> objs, final String delimiter) {
		if (objs == null || objs.isEmpty()) return "";
		Iterator<T> iter = objs.iterator();
		StringBuffer buffer = new StringBuffer(iter.next().toString());
		while (iter.hasNext()) buffer.append(delimiter).append(iter.next().toString());
		return buffer.toString();
	}
	// merge records:
	public void mergeRecords(ArrayList<ArrayList<String>> baseRecords, ArrayList<ArrayList<String>> newRecords) {
		// 1. find the latest month of oldRecords
		Set<String> baseDateSet = new HashSet<String>();
		int k = colIndex.get("resaledate");
		String endDate = baseRecords.get(0).get(k);
		String s;
		// find endDate
		for(ArrayList<String> record:baseRecords){
			s = record.get(k);
			baseDateSet.add(s);
			if(s.compareTo(endDate) > 0){
				endDate = s;
			}
		}
		// check if endDate in newRecords
		for(ArrayList<String> record:newRecords){
			s = record.get(k);
			if(s.equals(endDate)){
				break;
			}
		}
		// delete endDate of date in baseRecords
		Iterator<ArrayList<String>> it = baseRecords.iterator();
		while(it.hasNext()){
			if(it.next().get(k).equals(endDate))
				it.remove();
		}
		// delete newRecords that date exist in baseRecords
		it = newRecords.iterator();
		while(it.hasNext()){
			if(baseDateSet.contains(it.next().get(k)))
				it.remove();
		}
		// add newRecords to baseRecords
		baseRecords.addAll(newRecords);
	}
	public boolean mergeRecordsDb(){
		String dbColumns = join((AbstractCollection<String>) Arrays.asList(columns), ", ");
		String cmd, s, s1;
		java.sql.ResultSet rs;
		if(_dbConn == null)
			if(!initDb()) return false;
		prt("merge db tables...");
		try{
			Statement dbStat = _dbConn.createStatement(); 

			// remove all the data of which resaledate is in temp table
			cmd = "delete from " + dbMainTable + " where resaledate in (select resaledate from " + dbTempTable + ");";
			try{
				dbStat.executeUpdate(cmd);
			}catch(SQLException e){
				prt("exception when remove overlapped records.");
				return false;
			}
			// add temp table to main table if resale date not exists in main table
			cmd = "insert into " + dbMainTable + "(" + dbColumns + ") select * from " + dbTempTable + 
			" where resaledate not in (select resaledate from " + dbMainTable + ");";
			dbStat.executeUpdate(cmd);
		}catch(SQLException e){
			prt("merge table exception.");
			return false;
		}
		return true;
	}

	public void storeRecords(ArrayList<ArrayList<String>> records){
		String ss;
		Statement dbStat = null;
		boolean sqlError = false;
		if(bStoreDb && _dbConn != null){
			try{
				dbStat = _dbConn.createStatement();
			}catch(SQLException e){
				prt("storeRecords SQLException failed to createStatement");
				sqlError = true;
			}
			for(ArrayList<String> record:records) {
				if(sqlError) break;
				// format floorarea
				int i;
				i = colIndex.get("floorarea"); //5
				ss = record.get(i);
				try{
					record.set(i, String.format("%d", (int)Float.parseFloat(ss)));
				}catch(NumberFormatException e){
					prt("storeRecords NumberFormatException parse floorarea %s", ss);
				}
				// format price //8
				i = colIndex.get("price"); 
				ss = record.get(i);
				try{
					record.set(i, String.format("%d", (int)currencyFmt.parse(ss).intValue()));
				}catch(ParseException e){
					prt("storeRecords ParseException parse price %s", ss);
				}
				// format resaledate //9
				i = colIndex.get("resaledate");
				ss = record.get(i);
				record.set(i, dbDateFormat(ss));
				ss = join(record, "','");
				
				synchronized(_dblock) {
					try{
						dbStat.executeUpdate("insert into " + dbTempTable + " values('" + ss + "');");
					}catch(SQLException e){
						prt("storeRecords SQLException cannot write to database values:");
						prt(ss);
						sqlError = true;
					}
				}
			}
		}
		if(bStoreFile && out != null){
			try{
				for(ArrayList<String> record:records) {
					ss = join(record, DELIM);
					synchronized(_datlock) {
						out.write(ss+"\n");
					}
				}
			}catch(IOException e){
				prt("storeRecords IOException cannot write to dat file");
				return;
			}
		}
	}
	public ArrayList<ArrayList<String>> retrieveRecords(HtmlPage page, String flatType, String hdbTown){
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
		HtmlTable hTable=null; 
		try {
			hTable = page.getHtmlElementById("myScrollTable");//"sortabletable");
		}catch(ElementNotFoundException e){
			prt("retrieveRecords: ElementNotFoundException Table");
			return ret;
		}catch(Exception e) {
			prt("Exception while get sortabletable with town:" + hdbTown + " flat:" + flatType);
			prt("*** " + e.getMessage());//e.printStackTrace();
			return ret;
		}
		assert(null!=hTable);
		
		HtmlTableBody hBody = (HtmlTableBody)hTable.getBodies().get(0); //getElementsByTagName("tbody").item(0);
		
		for(final HtmlTableRow row : hBody.getRows() ) {
			ArrayList<String> record = new ArrayList<String>();
			record.add(hdbTown);
			record.add(flatType);
			for(final HtmlTableCell cell : row.getCells() ) {
				String[] sL = cell.asText().trim().split("\n");
				for(int i = 0; i < sL.length; ++i) {
					record.add(sL[i].trim().replace("'", ""));  // remove "'" as database doesnot support it
				}
			}
			ret.add(record);
		}
		return ret;
	}
	
	public List<Map.Entry<String, CheckableItem>> flatTypes = new LinkedList<Map.Entry<String, CheckableItem>>(), 
	    hdbTowns = new LinkedList<Map.Entry<String, CheckableItem>>();
	public String sStartDate, sEndDate, svStartDate, svEndDate;
	
	WebClient client = new WebClient(BrowserVersion.FIREFOX_24);
	
	void setWebOptions() {
		WebClientOptions webOptions = client.getOptions(); 
		webOptions.setRedirectEnabled(true); 
		webOptions.setThrowExceptionOnFailingStatusCode(false);
		webOptions.setThrowExceptionOnScriptError(false);
		webOptions.setJavaScriptEnabled(true);
		client.waitForBackgroundJavaScript(5000);
	}
	// populate flatTypes, hdbTowns, sStartDate, sEndDate
	public boolean fillInit()
	{
		setWebOptions();

		HtmlPage page ;
		HtmlForm form;
		HtmlSelect hSelect;
		List<HtmlOption> hOptionList;
		HtmlOption hOptionStartDate, hOptionEndDate;
		
		try{
			page = client.getPage(url);
		}catch(Exception e){
//			e.printStackTrace();
			prt("init: fail to get page %s ", url);
			return false;
		}
		assert(page!=null);
	
		form = page.getForms().get(0);
		assert(null!=form);
		
		hSelect = form.getSelectByName(uiFlatType);//"FlatType");
		hOptionList = hSelect.getOptions();
		Map<String, CheckableItem> mapFlatTypes = new TreeMap<String, CheckableItem>();
		for(HtmlOption I: hOptionList){
			if(!I.getValueAttribute().isEmpty() && !I.asText().isEmpty())
				mapFlatTypes.put(I.getValueAttribute(), new CheckableItem(I.asText()));
		}
		hSelect = form.getSelectByName(uiHDBTown);//"HDBTown");
		hOptionList = hSelect.getOptions();
		Map<String, CheckableItem> mapHdbTowns= new TreeMap<String, CheckableItem>();
		for(HtmlOption I: hOptionList){
			if(!I.getValueAttribute().isEmpty() && !I.asText().isEmpty()) 
				mapHdbTowns.put(I.getValueAttribute(), new CheckableItem(I.asText()));
		}
		flatTypes.clear();
		hdbTowns.clear();
		flatTypes.addAll(mapFlatTypes.entrySet());
		hdbTowns.addAll(mapHdbTowns.entrySet());
		Comparator<Map.Entry<String, CheckableItem>> comp = new Comparator<Map.Entry<String, CheckableItem>>() {
			public int compare( Map.Entry<String, CheckableItem> o1, Map.Entry<String, CheckableItem> o2 )
            {
                return (o1.getValue().toString()).compareTo( o2.getValue().toString() );
            }
		};
		Collections.sort(flatTypes, comp);
		
		DateFormat df = new SimpleDateFormat(uiDateFormat, Locale.ENGLISH);
		Date      date=null, date1=null;

		hSelect = form.getSelectByName(uiStartDate);//"StartDate");
		assert(null!=hSelect);
		hOptionList = hSelect.getOptions();
		hOptionStartDate = hOptionList.get(0);
		for(HtmlOption opt:hOptionList) {
			try {
				date1 = df.parse(opt.asText());
			}catch(ParseException e) {
			    continue;
			}
			if(date == null) {
			    date = date1;
			}else if(date.after(date1)){
				date = date1;
				hOptionStartDate = opt;
			}
		}
		sStartDate = hOptionStartDate.asText();
		svStartDate = hOptionStartDate.getValueAttribute();
			
		hSelect = form.getSelectByName(uiEndDate);//"EndDate");
		assert(null!=hSelect);
		hOptionList = hSelect.getOptions();
		hOptionEndDate = hOptionList.get(0);
		for(HtmlOption opt:hOptionList) {
			try {
				date1 = df.parse(opt.asText());
			}catch(ParseException e) {
			    continue;
			}
			if(date == null) {
			    date = date1;
			}else if(date.before(date1)){
				date = date1;
				hOptionEndDate = opt;
			}
		}
		sEndDate = hOptionEndDate.asText();
		svEndDate = hOptionEndDate.getValueAttribute();
		return true;
	}
	public HtmlPage query(String keyFlat, String keyTown) {
		setWebOptions();
		
		HtmlPage page = null, page2 = null;
		HtmlForm form;
		HtmlSelect hSelect;
		
		try{
			page = client.getPage(url);
		}catch(Exception e){
			prt("query: fail to get page %s ", url);
//			e.printStackTrace();
			return page2;
		}
		assert(page!=null);
		if(stopped()) return null;
		
		form = page.getForms().get(0);
		assert(null!=form);
		
//		chkInput = form.getInputByName("checkTC");
//		assert(null!=chkInput);
//		chkInput.setChecked(true);
		
		hSelect = form.getSelectByName(uiFlatType);
		assert(null!=hSelect);
		hSelect.setSelectedAttribute(keyFlat, true);
		
		hSelect = form.getSelectByName(uiHDBTown);
		assert(null!=hSelect);
		hSelect.setSelectedAttribute(keyTown, true);
		
		hSelect = form.getSelectByName(uiStartDate);
		assert(null!=hSelect);
		hSelect.setSelectedAttribute(svStartDate, true);
			
		hSelect = form.getSelectByName(uiEndDate);
		assert(null!=hSelect);
		hSelect.setSelectedAttribute(svEndDate, true);
		
		HtmlElement hElem;
		List<HtmlElement> hElemList;
		try {
			hElemList = form.getElementsByAttribute("input", "name", "Continue"); //getButtonByName("Continue");
		}catch(ElementNotFoundException e){
			prt("(%s, %s): Continue button not found", keyFlat, keyTown);
			//e.printStackTrace();
		    return page2;
		}
		hElem = hElemList.get(0);
		if(stopped()) return null;
		prt("submitting request...");
		try {
			page2 = (HtmlPage) hElem.click();
		} catch (IOException e) {
			prt("(%s, %s): Can not get result page", keyFlat, keyTown);
			//e.printStackTrace();
			return page2;
		}
		prt("got request result...");
		assert (null != page2);
		return page2;
	}

	public boolean initDb(){
		prt("Init Db...");
		try{
		    Class.forName("org.sqlite.JDBC");
		}catch(ClassNotFoundException e){
			prt("init db failed: class not found!");
			return false;
		}
		try{
			_dbConn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbName);
		}catch(SQLException e){
			prt("init db failed: can not connect to %s", dbName);
			return false;
		}
		return true;
	}
	public boolean resetDbTable(){
		Statement dbStat;
		String dbColumns = join((AbstractCollection<String>) Arrays.asList(columns), " varchar(20), ") + " varchar(20)";
		if(_dbConn == null)
			if(!initDb()) return false;
		try{
			dbStat = _dbConn.createStatement();
			dbStat.executeUpdate("drop table if exists " + dbTempTable + ";");
			dbStat.executeUpdate("create table "+ dbTempTable + "(" +dbColumns + ");");
		}catch(SQLException e){
			prt("init db failed: class not found!");
			return false;
		}
		try{
			dbStat.executeUpdate("create table "+ dbMainTable + "(" +dbColumns + ");");
		}catch(SQLException e){
		}
		
		return true;
	}
	public void purge(){
		flatTypes.clear();
		hdbTowns.clear();
	}
	public void retrievePages(){
		if(retrieveListener != null) retrieveListener.onRetrieveBegin();
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		prt("%s Begin retrieve pages...", df.format(new Date()));
		if(bStoreFile){
			try{
			    out = new BufferedWriter(new FileWriter("SgPropertyResale-" + df.format(new Date()) + ".txt"));
			}catch(Exception e){
				prt("Error: " + e.getMessage());
			}
		}
		int total = 0;
		for(Map.Entry<String, CheckableItem> entryTown: hdbTowns) {
			if( stopped() ) break;
			String sTown = entryTown.getValue().toString();
			if(!entryTown.getValue().isSelected()) continue;
			int townTotal = 0;
			for(Map.Entry<String, CheckableItem> entryFlat: flatTypes) {
				if( stopped() ) break;
				String sFlat = entryFlat.getValue().toString(); 
				if (sFlat.equalsIgnoreCase("1 Room") || sFlat.equalsIgnoreCase("2 Room")) {
					entryFlat.getValue().setSelected(false);
					continue;
				}
				prt("(%s, %s): start retrieve...", sFlat,  sTown);
				HtmlPage page = query(entryFlat.getKey(), entryTown.getKey());
				if( page == null) break;
				if(!entryFlat.getValue().isSelected()) continue;
				if(!entryTown.getValue().isSelected()) break;
				ArrayList<ArrayList<String>> records = retrieveRecords(page, sFlat, sTown);
				if( records.size() == 0 ){
					prt("(%s, %s): records not found", sFlat, sTown);
					continue;
				}
				prt("(%s, %s): %d records found", sFlat, sTown, records.size());
				storeRecords(records);
				townTotal += records.size();
				total += records.size();
				if(retrieveListener != null) retrieveListener.onOnePageRetrieved(records);
			}
			prt("(%s): %d records found", sTown, townTotal);
		}
		prt("total records count: %d", total);
		if(bStoreFile){
			try{
				out.flush();
			    out.close();
			}catch(Exception e){
				prt("Error: " + e.getMessage());
			}
		}
		prt("%s end retrieve pages", df.format(new Date()));
		if(retrieveListener != null) retrieveListener.onRetrieveEnd();
	}
	public void initPage(){
		if(retrieveListener != null) retrieveListener.onInitPageBegin();
		if(bStoreDb && _dbConn==null){
			prt("resetDbTable...");
			resetDbTable();
		}
		if(!inited){
			prt("initializing page...");
			if(!fillInit()) return;
			inited = true;
		}
		if(retrieveListener != null) retrieveListener.onInitPageEnd();
	}
	public void startAll(){
		initPage();
		if(!stopped()) retrievePages();
		if(!stopped()) mergeRecordsDb();
	}
	public static void run(String[] args) {
		SgPropertyResalePage web = new SgPropertyResalePage();
		web.startAll();
	}

}
class MyGui extends JFrame implements SgPropertyResalePage.IRetrieveListerner {
	JButton btnStart, btnStop, btnExit;
	JButton btnInit, btnRetrieve, btnMergeDb;
	JCheckBox chkStoreDb, chkStoreFile,chkConcurrent;
	JList lstMsg, lstFlatType, lstHdbTown;
	DefaultListModel modelMsg, modelFlatType, modelHdbTown;
	java.awt.Container container;
	
	SgPropertyResalePage retrieve;
	
	public MyGui(){
		initComponents();
	}
	public MyGui(SgPropertyResalePage retriever){
		retrieve = retriever;
		retrieve.retrieveListener = this;
		initComponents();
	}
	public void initComponents(){
		btnInit = new JButton("Init");
		btnRetrieve = new JButton("Retrieve");
		btnMergeDb = new JButton("MergeDb");
		
		btnStart = new JButton("StartAll");
		btnStop = new JButton("Stop");
		btnExit = new JButton("Exit");
		
		modelMsg = new DefaultListModel();
		modelFlatType = new DefaultListModel();
		modelHdbTown = new DefaultListModel();
	
		JPanel panelButtons = new JPanel();
		panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.LINE_AXIS));
		panelButtons.add(btnInit);
		panelButtons.add(btnRetrieve);
		panelButtons.add(btnMergeDb);
		panelButtons.add(new JLabel("    "));
		panelButtons.add(btnStart);
		panelButtons.add(btnStop);
		panelButtons.add(btnExit);
		
		JPanel panelOptions = new JPanel();
		panelOptions.setLayout(new BoxLayout(panelOptions, BoxLayout.LINE_AXIS));
		chkStoreDb = new JCheckBox("Store in Database");
		chkStoreFile = new JCheckBox("Store in txt File");
		chkConcurrent = new JCheckBox("Concurrently");
		panelOptions.add(new JLabel("    "));
		panelOptions.add(chkStoreDb);
		panelOptions.add(chkStoreFile);
		panelOptions.add(chkConcurrent);
		chkStoreDb.setSelected(true);
		chkStoreFile.setSelected(true);
		chkConcurrent.setSelected(false);
		
		JPanel panelCmd = new JPanel();
		panelCmd.setLayout(new BoxLayout(panelCmd, BoxLayout.LINE_AXIS));
		panelCmd.add(panelButtons);
		panelCmd.add(panelOptions);
		
		JPanel panelLists = new JPanel();
		panelLists.setLayout(new BoxLayout(panelLists, BoxLayout.LINE_AXIS));
		lstMsg = new JList(modelMsg);
		lstFlatType = new JList(modelFlatType);
		lstHdbTown = new JList(modelHdbTown);
		panelLists.add(new JScrollPane(lstMsg));
		panelLists.add(new JScrollPane(lstFlatType));
		panelLists.add(new JScrollPane(lstHdbTown));
		
		MouseAdapter listMouseAdapter = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JList list = (JList) e.getSource();
				int index = list.locationToIndex(e.getPoint());
				CheckableItem item = (CheckableItem) list.getModel().getElementAt(index);
				item.setSelected(!item.isSelected());
				list.repaint(list.getCellBounds(index, index));
			}
		};
		lstFlatType.addMouseListener(listMouseAdapter);
		lstHdbTown.addMouseListener(listMouseAdapter);
		lstFlatType.setCellRenderer(new CheckListRenderer());
		lstHdbTown.setCellRenderer(new CheckListRenderer());
		
		chkStoreDb.addItemListener(
		    new ItemListener() {
		        public void itemStateChanged(ItemEvent e) {
		        	if(retrieve != null)
		        		retrieve.bStoreDb = (e.getStateChange() == ItemEvent.SELECTED);
		        }
		    });
		chkStoreFile.addItemListener(
			    new ItemListener() {
			        public void itemStateChanged(ItemEvent e) {
			        	if(retrieve != null)
			        		retrieve.bStoreFile = (e.getStateChange() == ItemEvent.SELECTED);
			        }
			    });
		chkConcurrent.addItemListener(
			    new ItemListener() {
			        public void itemStateChanged(ItemEvent e) {
			        	if(retrieve != null)
			        		retrieve.bConcurrent = (e.getStateChange() == ItemEvent.SELECTED);
			        }
			    });

		container = getContentPane();
		container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
		container.add(panelCmd);
		container.add(panelLists);
		
		//-- ui events
		btnStart.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent event){
			prt("*** Cmd StartAll");
			modelMsg.clear();
			if(retrieve != null) {
				retrieve.stop(false);
        		new SwingWorker<Void, Void>() {
        			public Void doInBackground() {
        				retrieve.startAll();
        				return null;
        			}
        		}.execute();
			}
		}});
		btnStop.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent event){
			prt("*** Cmd Stop");
			if(retrieve != null) 
				retrieve.stop(true);
		}});
		btnExit.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent event){
			System.exit(0);
		}});
		
		btnInit.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent event){
			prt("*** Cmd Init");
			if(retrieve != null) {
				retrieve.stop(false);
        		new SwingWorker<Void, Void>() {
        			public Void doInBackground() {
        				retrieve.initPage();
        				return null;
        			}
        		}.execute();
			}
		}});
		btnRetrieve.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent event){
			prt("*** Cmd Retrieve");
			if(retrieve != null) {
				retrieve.stop(false);
        		new SwingWorker<Void, Void>() {
        			public Void doInBackground() {
        				retrieve.retrievePages();
        				return null;
        			}
        		}.execute();
			}
		}});
		btnMergeDb.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent event){
			prt("*** Cmd Merge");
			if(retrieve != null) {
   				retrieve.mergeRecordsDb();
			}
		}});
		
		//-- frame appearance
		onRetrieveEnd();	
		
		setTitle("Retrieve HDB Resale Data " + SgPropertyResalePage.version);
		setSize(800, 700);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setVisible(true);
//		setUndecorated(true);
//		getRootPane().setWindowDecorationStyle(JRootPane.NONE);
	}
	public void onInitPageBegin(){
		btnInit.setEnabled(false);
		btnRetrieve.setEnabled(false);
		btnMergeDb.setEnabled(false);
		//--
		btnStart.setEnabled(false);
		btnStop.setEnabled(true);
		btnExit.setEnabled(false);
	}
	public void onInitPageEnd(){
		btnInit.setEnabled(true);
		btnRetrieve.setEnabled(true);
		btnMergeDb.setEnabled(true);
		//--
		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
		btnExit.setEnabled(true);
		
		modelFlatType.clear();
		modelHdbTown.clear();
		if(retrieve != null) {
			prt("%d hdb towns got.", retrieve.hdbTowns.size());
			for(Map.Entry<String, CheckableItem> entryTown: retrieve.hdbTowns) {
				modelHdbTown.addElement(entryTown.getValue());
			}
			prt("%d flat types got.", retrieve.flatTypes.size());
			for(Map.Entry<String, CheckableItem> entryFlat: retrieve.flatTypes) {
				modelFlatType.addElement(entryFlat.getValue());
			}
			prt("from %s to %s", retrieve.svStartDate, retrieve.svEndDate);
		}
	}
	public void onRetrieveBegin(){
		btnInit.setEnabled(false);
		btnRetrieve.setEnabled(false);
		btnMergeDb.setEnabled(false);
		//--
		btnStart.setEnabled(false);
		btnStop.setEnabled(true);
		btnExit.setEnabled(false);
	}
	public void onRetrieveEnd(){
		btnInit.setEnabled(true);
		btnRetrieve.setEnabled(true);
		btnMergeDb.setEnabled(true);
		//--
		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
		btnExit.setEnabled(true);
	}
	public void onOnePageRetrieved(ArrayList<ArrayList<String>> records){
		
	}

	public void prt(String fmt, Object... args){
		String s = String.format(fmt, args);
		modelMsg.addElement(s);
	}
}
//------- CheckList
class CheckListRenderer extends JCheckBox implements ListCellRenderer {

    public CheckListRenderer() {
      setBackground(UIManager.getColor("List.textBackground"));
      setForeground(UIManager.getColor("List.textForeground"));
    }

    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean hasFocus) {
      setEnabled(list.isEnabled());
      setSelected(((CheckableItem) value).isSelected());
      setFont(list.getFont());
      setText(value.toString());
      return this;
    }
}
class CheckableItem {
    private String str;
    private boolean isSelected;
    public CheckableItem(String str) {
      this.str = str;
      isSelected = true;
    }
    public CheckableItem(String str, boolean checked) {
        this.str = str;
        isSelected = checked;
      }

    public void setSelected(boolean b) {
      isSelected = b;
    }
    public boolean isSelected() {
      return isSelected;
    }
    public String toString() {
      return str;
    }
 }