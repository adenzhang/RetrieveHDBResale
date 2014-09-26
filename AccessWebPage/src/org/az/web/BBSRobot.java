
package org.az.web;

import java.awt.EventQueue;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.JList;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.RefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import java.net.URL;

public class BBSRobot extends JFrame {

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					BBSRobot frame = new BBSRobot();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	JButton btnStart, btnStop, btnExit, btnStartOne;
	JCheckBox chkRandom;
	JTextField txtInterval;
	JList lstContent;
	JList lstMsg;
	DefaultListModel modelContent, modelMsg;
	java.awt.Container container;

	final static String _configFileName = "bbsrobot.xml";
	boolean _stopUpload;

	boolean stopped() {
		return _stopUpload;
	}

	static public class UploadInfo {
		// read from file
		public UploadInfo() {
			contents = new ArrayList<String>();
		}

		public String url;
		public String user, password;

		public List<String> contents;

		int interval, count;
	};

	UploadInfo _upload;

	WebClient _webclient;
	HtmlPage threadPage;

	/**
	 * Create the frame.
	 */
	public BBSRobot() {
		initComponents();
	}

	public void prt(String fmt, Object... args) {
		String s = String.format(fmt, args);
		modelMsg.addElement(s);
	}

	BufferedWriter _out;

	public void log(String fmt) {
		try {
//			String s = String.format(fmt, args);
			_out.write(fmt);
			_out.flush();
		} catch (IOException e) {
			prt("Failed to write into logfile.");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void initComponents() {
		FileWriter fstream = null;
		String logFileName = "out.log";
		File logf = new File(logFileName);
		logf.delete();
		try {
			fstream = new FileWriter(logFileName);
			_out = new BufferedWriter(fstream);
		} catch (IOException e) {
			prt("Failed to create log file %s", logFileName);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 700);
		container = getContentPane();
		container
				.setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		//
		JPanel panelButtons = new JPanel();
		panelButtons
				.setLayout(new BoxLayout(panelButtons, BoxLayout.LINE_AXIS));
		panelButtons.add(btnStart = new JButton("Auto upload"));
		// panelButtons.add(txtInterval = new JTextField(""));
		// panelButtons.add(new JLabel("second interval "));
		panelButtons.add(btnStop = new JButton("Stop upload"));
		panelButtons.add(btnStartOne = new JButton("Upload selected"));
		panelButtons.add(btnExit = new JButton("Exit"));
		container.add(panelButtons);

		JPanel panelContents = new JPanel();
		panelContents.setLayout(new BoxLayout(panelContents,
				BoxLayout.PAGE_AXIS));
		panelContents.add(new JLabel("Contents to be updated:"));
		// panelContents.add(new JScrollPane(txtContents = new JTextArea("")));
		panelContents.add(new JScrollPane(lstContent = new JList(
				modelContent = new DefaultListModel())));
		container.add(panelContents);

		JPanel panelMsg = new JPanel();
		panelMsg.setLayout(new BoxLayout(panelMsg, BoxLayout.PAGE_AXIS));
		panelContents.add(new JLabel("Messages:"));
		panelContents.add(new JScrollPane(lstMsg = new JList(
				modelMsg = new DefaultListModel())));
		container.add(panelMsg);

		boolean loadOk = loadContent();

		for (String msg : _upload.contents) {
			modelContent.addElement(msg);
		}
		if (!loadOk) {
			prt("Failed to load config file %s. Please check it and relaunch me.",
					_configFileName);
			btnStart.setEnabled(false);
			btnStartOne.setEnabled(false);
		}
		btnStop.setEnabled(false);
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				prt("*** Cmd StartAll");
				_stopUpload = false;
				new SwingWorker<Void, Void>() {
					public Void doInBackground() {
						btnStop.setEnabled(true);
						btnStartOne.setEnabled(false);
						btnStart.setEnabled(false);
						btnExit.setEnabled(false);
						startUpload(_upload);
						btnStop.setEnabled(false);
						btnStart.setEnabled(true);
						btnStartOne.setEnabled(true);
						btnExit.setEnabled(true);
						return null;
					}
				}.execute();
			}
		});
		btnStartOne.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				prt("*** Cmd StartOne");
				_stopUpload = false;
				new SwingWorker<Void, Void>() {
					public Void doInBackground() {
						btnStop.setEnabled(true);
						btnStartOne.setEnabled(false);
						btnStart.setEnabled(false);
						btnExit.setEnabled(false);
						int oldCount = _upload.count;
						_upload.count = 1;
						startUpload(_upload);
						_upload.count = oldCount;
						btnStop.setEnabled(false);
						btnStart.setEnabled(true);
						btnStartOne.setEnabled(true);
						btnExit.setEnabled(true);
						return null;
					}
				}.execute();
			}
		});
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				prt("*** Cmd Stop");
				_stopUpload = true;
			}
		});

		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});

	}

	public static String FileToString(String filename)
			throws FileNotFoundException, IOException {
		String retval = "";
		String buf;
		BufferedReader in = new BufferedReader(new FileReader(filename));
		while ((buf = in.readLine()) != null) {
			retval += buf + "\n";
		}
		return retval;
	}

	public static Document StringToDocument(String xmlString)
			throws SAXException, IOException, ParserConfigurationException {

		Document doc = null;

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
				.newInstance();

		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

		ByteArrayInputStream bais = new ByteArrayInputStream(
				xmlString.getBytes());

		doc = docBuilder.parse((InputStream) bais);

		return doc;

	}

	boolean loadContent() {
		_upload = new UploadInfo();
		// todo
		try {
			String xmlString = FileToString(_configFileName);
			Document doc = StringToDocument(xmlString);
			System.out.println("Root element "
					+ doc.getDocumentElement().getNodeName());
			NodeList nodeLst = doc.getElementsByTagName("thread");

			for (int s = 0; s < nodeLst.getLength(); s++) {
				Node threadNode = nodeLst.item(s);
				if (threadNode.getNodeType() != Node.ELEMENT_NODE)
					continue;
				Element threadElem = (Element) threadNode;
				Element urlElem = (Element) threadElem.getElementsByTagName(
						"url").item(0);
				// _upload.url =(threadElem.getAttribute("url"));
				_upload.url = urlElem.getTextContent().trim();

				Element loginElem = (Element) threadElem.getElementsByTagName(
						"login").item(0);
				_upload.user = loginElem.getAttribute("user");
				_upload.password = loginElem.getAttribute("password");

				Element freqElem = (Element) threadElem.getElementsByTagName(
						"frequency").item(0);
				String sInterval = freqElem.getAttribute("interval");
				String sCount = freqElem.getAttribute("count");
				try {
					_upload.interval = Integer.parseInt(sInterval);
				} catch (NumberFormatException e) {
					prt("Failed to convert interval %s to integer!", sInterval);
					return false;
				}
				try {
					_upload.count = Integer.parseInt(sCount);
				} catch (NumberFormatException e) {
					prt("Failed to convert sCount %s to integer!", sCount);
					return false;
				}

				Element msgsElem = (Element) threadElem.getElementsByTagName(
						"msgs").item(0);
				NodeList msgsNode = msgsElem.getElementsByTagName("msg");
				for (int k = 0; k < msgsNode.getLength(); k++) {
					Element msgElem = (Element) msgsNode.item(k);
					_upload.contents.add(msgElem.getTextContent().trim());
				}
			}
		} catch (FileNotFoundException e) {
			prt("Filed to find file %s ", _configFileName);
			return false;
		} catch (SAXParseException e) {
			prt("File format error %s", _configFileName);
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			// prt("File format error %s" , _configFileName);
			e.printStackTrace();
			return false;
		}
		return true;
	}

	HtmlPage getInitialPage(String url) {
		_webclient = new WebClient(BrowserVersion.FIREFOX_24);// FIREFOX_3_6);
		_webclient.getOptions().setRedirectEnabled(true);
		_webclient.setAjaxController(new NicelyResynchronizingAjaxController());
		_webclient.setRefreshHandler(new RefreshHandler() {
			public void handleRefresh(Page page, URL url, int arg)
					throws IOException {
				System.out.println("handleRefresh");
			}

		});

		HtmlPage page = null;

		try {
			page = _webclient.getPage(url);
		} catch (Exception e) {
			prt("query: fail to get page %s ", url);
			e.printStackTrace();
		}

		return page;
	}

	// return the first found page put on by the author
	HtmlPage searchPage(HtmlPage pageIn, String title, String author,
			String boardName) {
		HtmlPage pageOut = null;

		HtmlElement page_main = pageIn.getHtmlElementById("page_main");
		HtmlElement page_header = pageIn.getHtmlElementById("page_header");
		HtmlElement menu_box = pageIn.getHtmlElementById("menu_box");
		HtmlElement mn_search = pageIn.getHtmlElementById("mn_Nccf6"); // search
																		// menu
																		// item

		HtmlPage pageSearch;
		try {
			pageSearch = mn_search.click();
		} catch (IOException e) {
			prt("Failed to get search page!");
			return null;
		}

		// HtmlElement nv_search = pageSearch.getElementById("nv_search");
		// HtmlElement ht = nv_search.getElementById("ct").getElementBy("bm_c");
		HtmlForm searchForm = pageSearch.getForms().get(1);

		// search by text
		HtmlInput textInput = searchForm.getInputByName("srchtxt_1");
		textInput.setValueAttribute(title);
		// search by author
		HtmlInput authorInput = searchForm.getInputByName("srchname");
		authorInput.setValueAttribute(author);

		// search by board name
		HtmlSelect boardSelect = searchForm.getSelectsByName("srchfid").get(0);
		boardSelect.getOptions();
		for (HtmlOption it : boardSelect.getOptions()) {
			String s = it.asText();
			if (0 <= s.trim().indexOf(boardName)) {
				((HtmlOption) it).setSelected(true);
				break;
			}
		}

		// submit form
		HtmlElement hElem;
		List<HtmlElement> hElemList;
		try {
			hElemList = searchForm.getElementsByAttribute("input", "name",
					"searchsubmit"); // getButtonByName("Continue");
		} catch (ElementNotFoundException e) {
			prt("searchsubmit button not found");
			// e.printStackTrace();
			return null;
		}
		hElem = hElemList.get(0);
		if (stopped())
			return null;
		prt("submitting search form...");
		HtmlPage searchedPage;
		try {
			searchedPage = (HtmlPage) hElem.click();
		} catch (IOException e) {
			prt("Failed to submit search form");
			// e.printStackTrace();
			return null;
		}
		prt("got search result");
		assert (null != searchedPage);

		HtmlElement threadList = searchedPage.getHtmlElementById("threadlist");
		DomNodeList<HtmlElement> anchors = threadList.getElementsByTagName("a"); // get
																					// all
																					// anchors
		if (0 == anchors.getLength()) {
			prt("Found nothhing");
			return null;
		}
		HtmlAnchor anchor = (HtmlAnchor) anchors.get(0);
		try {
			pageOut = anchor.click();
		} catch (IOException e) {
			prt("IOException while click anchor");
			e.printStackTrace();
		}

		return pageOut;
	}

	// return null if thread not found.
	HtmlPage puton(HtmlPage pageIn, String msg) {
		HtmlPage pageOut = null;

		HtmlElement page_main = pageIn.getHtmlElementById("page_main");
		HtmlElement page_content = pageIn.getHtmlElementById("page_content");
		HtmlElement page_ct = pageIn.getHtmlElementById("ct");

		String txt = pageIn.asXml();
		HtmlForm replyForm;
		//
		try {
			replyForm = (HtmlForm) pageIn.getElementById("fastpostform");
			txt = replyForm.asXml();
		} catch (ElementNotFoundException e) {
			prt("Failed to find fastpostform");
			return null;
		}
		if (false) {
			HtmlElement pgt;
			try {
				pgt = (HtmlElement) pageIn.getElementById("pgt");
			} catch (ElementNotFoundException e) {
				prt("Thread not found. Login again");
				return null;
			}
			HtmlAnchor replyAnchor = (HtmlAnchor) pageIn
					.getHtmlElementById("post_reply");
			HtmlPage replyPage = null;
			try {
				replyPage = replyAnchor.click();
			} catch (IOException e) {
				prt("Failed to goto replay page");
				e.printStackTrace();
				return null;
			}
			page_main = replyPage.getHtmlElementById("page_main");
			page_content = replyPage.getHtmlElementById("page_content");
		}
		// HtmlForm replyForm = page_content.getElementById("postform");
		HtmlTextArea textArea = (HtmlTextArea) replyForm.getElementsByTagName(
				"textarea").item(0);
		textArea.setText(msg);
		HtmlButton submitBtn = replyForm.getButtonByName("fastpostsubmit");
		try {
			pageOut = submitBtn.click();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pageOut;
	}

	boolean hasLoggedIn(HtmlPage page) {
		HtmlElement page_main = page.getHtmlElementById("page_main");
		HtmlElement page_header = page.getHtmlElementById("page_header");
		try {
			HtmlElement onlineMenu = page.getHtmlElementById("um");
			return true;
		} catch (ElementNotFoundException e) {
			return false;
		}
	}

	String getLoggedInUserName(HtmlPage page) {
		HtmlElement page_main = page.getHtmlElementById("page_main");
		HtmlElement page_header = page.getHtmlElementById("page_header");
		try {
			HtmlElement onlineMenu = page.getHtmlElementById("um");
			// has logged in
			DomNodeList<HtmlElement> anchors = onlineMenu
					.getElementsByTagName("a");
			HtmlElement anchor = anchors.get(0);
			return anchor.asText().trim();
		} catch (ElementNotFoundException e) {
			return "";
		}
	}

	HtmlPage logout(HtmlPage page) {
		// todo
		return page;
	}

	HtmlPage TryLogIn(HtmlPage page, String user, String password) {
		HtmlPage pageOut = null;
		String txt;
		//
		if (hasLoggedIn(page))
			return page;

		// offline status, and login
		HtmlElement loginForm = page.getHtmlElementById("lsform");

		HtmlInput userInput = page.getHtmlElementById("ls_username");
		userInput.setValueAttribute(user);
		HtmlInput passwordInput = page.getHtmlElementById("ls_password");
		passwordInput.setValueAttribute(password);

		HtmlButton loginButton = (HtmlButton) loginForm.getElementsByTagName(
				"button").get(0);
		try {
			
			pageOut = loginButton.click();
		} catch (IOException e) {
			prt("Failed to login IOException");
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			prt("Failed to login Exception");
			e.printStackTrace();
			return null;
		}
		for (int n = 0; 0 != (n = _webclient.waitForBackgroundJavaScript(6000));) {
			prt("There're %d jobs running background", n);
			if (stopped()) {
				prt("User stopped. exit!");
				return null;
			}
		}
		if (!hasLoggedIn(pageOut)) {
			prt("Failed to log in");
			txt = pageOut.asXml();
			if (!txt.isEmpty()) {
				log(txt);
			}
			return null;
		}

		return pageOut;
	}

	void startUpload(UploadInfo upload) {
		HtmlPage page;
		if (null == threadPage) {
			threadPage = getInitialPage(upload.url);
		}
		if (null != threadPage) {
			int N = upload.contents.size();
			for (int count = 0;; ++count) {
				int k = count % N;
				String content = upload.contents.get(k);
				page = TryLogIn(threadPage, upload.user, upload.password);
				if (null == page) {
					prt("Please verify %s", upload.url);
					break;
				}
				threadPage = page;
				if (stopped()) {
					prt("User stopped.");
					break;
				}
				page = puton(threadPage, content);
				if (null == page) {
					prt("Please verify %s", upload.url);
					break;
				}
				threadPage = page;
				prt("Finihsed putting on %s", content);
				if (upload.count > 0 && upload.count <= count) {
					break;
				}

				try {
					Thread.sleep(upload.interval);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break;
				}
				if (stopped()) {
					prt("User stopped.");
					break;
				}
			}
		}
		_stopUpload = false;
	}
}
