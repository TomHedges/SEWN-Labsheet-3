package code;
// WRITTEN BY: Martin O'Shea.

// With thanks to: http://www.javaalmanac.com/egs/javax.swing.text.html/GetLinks.html.

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class ParseHtmlLinks {

	// ******************************************************************************************
	// ** INSTANCE VARIABLES.                                                                  **
	// ******************************************************************************************

	private String urlToParse;
	private String baseRelativeURL;
	private String currentRelativeURL;
	private ArrayList<String> htmlLinksFound;

	private ArrayList<String> visitedURLs;
	private ArrayList<Integer> visitedURLsVisitedPageCount;
	private ArrayList<String> disallowedURLs;
	
	private int arrayPointerToLinkCurrentlyBeingParsed;
	
	final static String urlToIgnore = "DO NOT USE THIS LINK FOR ANYTHING";

	// private Hashtable<String, Integer> urlsToParseOLD;

	// ******************************************************************************************
	// ** PROGRAM.                                                                             **
	// ******************************************************************************************

	public static void main(String args[]) {

		String urlToParse = ("http://www.dcs.bbk.ac.uk/~martin/sewn/ls3/index.html");
		ParseHtmlLinks phl = new ParseHtmlLinks(urlToParse);

		//READ ROBOTS.TXT file
		try {
			//URL url;
			//url = new URL(phl.getBaseRelativeURL() + "robots.txt");
			//Scanner s = new Scanner(url.openStream());

			//URL url = new URI(phl.getBaseRelativeURL() + "robots.txt").toURL();
			//URLConnection conn = url.openConnection();
			//Reader rd = new InputStreamReader(conn.getInputStream());
			//Scanner s = new Scanner(rd).

			URL url = new URI(phl.getBaseRelativeURL() + "robots.txt").toURL();
			//Reader rd = new InputStreamReader(conn.getInputStream());
			InputStream in = url.openStream();
			Scanner s = new Scanner(in);//.useDelimiter("\\A");

			while (s.hasNextLine()) {
				String test = s.nextLine();
				if (test.contains("/")) {
					test = test.substring(test.indexOf("/")+1);
					phl.disallowedURLs.add(phl.getBaseRelativeURL() + test);
				}
			}

//		TEST	url = new URI(phl.getBaseRelativeURL() + "robots.txt").toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int loopCounter = 1;
		do
		{
			String nextURL = phl.getUrlToParse(loopCounter);
			System.out.println(loopCounter + ".) Parsing links from: " + nextURL);
			phl.setCurrentRelativeURL(nextURL);
			phl.parseHtmlLinks(nextURL);
			System.out.println("Links found: ");
			phl.listHtmlLinksFound();
			phl.htmlLinksFound.clear(); //- NOT NECESSARY?
			loopCounter++;
		} while (phl.getUrlsToParseSize()>=loopCounter);
		
		System.out.println();
		
		phl.listVisitedPagesPerVisitedURL();
	}

	// ******************************************************************************************
	// ** CONSTRUCTOR.                                                                         **
	// ******************************************************************************************

	public ParseHtmlLinks(String urlToParse) {
		this.urlToParse = urlToParse;
		this.baseRelativeURL = parseURLToRelativeForm(urlToParse);
		this.htmlLinksFound = new ArrayList<String>();
		this.disallowedURLs = new ArrayList<String>();

		// Use arraylists to store list of links to be visited and number of links to each
		this.visitedURLs = new ArrayList<String>();
		this.visitedURLsVisitedPageCount = new ArrayList<Integer>();

		// add initial link
		this.visitedURLs.add(urlToParse);
		this.visitedURLsVisitedPageCount.add(0);
		
		this.setArrayPointerToLinkCurrentlyBeingParsed(0);

		//this.urlsToParseOLD = new Hashtable<String, Integer>();
		//this.urlsToParseOLD.put(urlToParse, new Integer(0));
	}

	// ******************************************************************************************
	// ** ACCESSOR AND MUTATOR METHODS.                                                        **
	// ******************************************************************************************

	public String getUrlToParse(int loopCounter) {

		//int innerLoopCounter = 0;
		//String nextURL = "";

		//Enumeration<String> enumKey = urlsToParseOLD.keys();
		//do {
		//	innerLoopCounter++;
		//    nextURL = enumKey.nextElement();
		//} while (enumKey.hasMoreElements() && innerLoopCounter<loopCounter);

		// Set pointer for which link i nthe "to parse" array is currently being looked at, so that links to be visited can be added to correct total
		this.setArrayPointerToLinkCurrentlyBeingParsed(loopCounter-1);
		
		return this.visitedURLs.get(loopCounter-1);
	}

	public void setUrlToParse(String urlToParse) {
		this.urlToParse = urlToParse;
	}

	public void setBaseRelativeURL(String url) {
		this.baseRelativeURL = parseURLToRelativeForm(url);
	} 
	
	public String getBaseRelativeURL() {
		return baseRelativeURL;
	} 

	public void setCurrentRelativeURL(String url) {
		this.currentRelativeURL = parseURLToRelativeForm(url);
	}

	public String getCurrentRelativeURL() {
		return currentRelativeURL;
	}

	public int getUrlsToParseSize() {
		//return urlsToParseOLD.size();
		return visitedURLs.size();
	}

	public ArrayList<String> getHtmlLinksFound() {
		return htmlLinksFound;
	}

	public void setHtmlLinksFound(ArrayList<String> htmlLinks) {
		this.htmlLinksFound = htmlLinks;
	}

	private void setArrayPointerToLinkCurrentlyBeingParsed(
			int arrayPointerToLinkCurrentlyBeingParsed) {
		this.arrayPointerToLinkCurrentlyBeingParsed = arrayPointerToLinkCurrentlyBeingParsed;
	}

	private int getArrayPointerToLinkCurrentlyBeingParsed() {
		return arrayPointerToLinkCurrentlyBeingParsed;
	}
	
	// ******************************************************************************************
	// ** HELPER METHODS.                                                                      **
	// ******************************************************************************************

	// Checks format of 'urlToParse' and returns version suitable for use as a base relative URL 
	private String parseURLToRelativeForm(String originalURL) {
		String baseRelativeURL = originalURL;
		char[] charactersInURL;
		charactersInURL = originalURL.toCharArray();
		int loopCounter = charactersInURL.length-1;
		boolean finishedChecks = false;
		boolean dotEncountered = false;

		do
		{
			if (charactersInURL[loopCounter] == '/')
			{
				// checks if last character in array is a slash
				if (loopCounter == charactersInURL.length-1)
				{
					// then 'originalURL' is already base relative path
					baseRelativeURL = originalURL;
					finishedChecks = true;
				}

				// if dot not yet encountered, then 'originalURL' was base relative path, just needing '/'
				if (!dotEncountered)
				{
					baseRelativeURL = originalURL + "/";
					finishedChecks = true;
				}
				else
				{
					//'originalURL' was a full filepath - so need to perform sum to get correct base URL
					baseRelativeURL = originalURL.substring(0, loopCounter+1);
					finishedChecks = true;
				}
			}

			if (charactersInURL[loopCounter] == '.')
			{
				dotEncountered=true;
			}

			loopCounter--;
		} while (loopCounter>0 && !finishedChecks);

		return baseRelativeURL;
	}

	// Checks the format of a URL, and returns full version if it is relative
	private String returnFullURL(String htmlLink) {
		// OLD 1 - Check whether url has key component meaning it is full rather than relative (not absolute measure, but good enough for this exercise?)
		//if (!htmlLink.toLowerCase().contains("://"))
		//{
		//	// if "url" has a colon, then it isn't really a URL! so make sure it isn't made realtive, and won't get parsed
		//	if (!htmlLink.toLowerCase().contains(":")) {
		//		htmlLink = this.getBaseRelativeURL() + htmlLink;
		//	}
		//}
		
		// NEW - Only return links beginning with 'http://' - so remove anything with other forms of link
		if (!htmlLink.toLowerCase().substring(0, 7).equals("http://")  && !htmlLink.toLowerCase().substring(0, 6).equals("ftp://") && htmlLink.toLowerCase().contains(":")) {
			htmlLink = urlToIgnore;
		} else {

			// needs to be made absolute
			if (!htmlLink.toLowerCase().substring(0, 7).equals("http://") && !htmlLink.toLowerCase().substring(0, 6).equals("ftp://")) {
				htmlLink = this.getCurrentRelativeURL() + htmlLink;
			}
			
			
			// PUT THESE LINES BACK IN (COMPLETED!)
			// Some of these URLs contain ./ and ../ references, so we need to edit them out!
			while (htmlLink.contains("../")) {
				int pointer = htmlLink.indexOf("../")-1;
				String linkManipulator = htmlLink.substring(0, pointer);
				linkManipulator = linkManipulator.substring(0, linkManipulator.lastIndexOf("/"));
				htmlLink = linkManipulator + htmlLink.substring(pointer+3);
			}
			
			//PUT THIS BACK IN ONCE THE ABOVE IS WORKING...
			htmlLink = htmlLink.replace("./", "");
			
			// OLD 2 - Only return links beginning with 'http://' - so remove
			//if (!htmlLink.toLowerCase().substring(0, 7).equals("http://")) {
			//	// if htmlLink has a colon, then it isn't a URL we want for this exercise! so make sure it isn't used for anything
			//	if (htmlLink.toLowerCase().contains(":")) {
			//		htmlLink = urlToIgnore;
			//	} else {
			//		
			//		//PUT THIS BACK IN ONCE THE ABOVE IS WORKING...
			//		htmlLink = htmlLink.replace("./", "");
			//		htmlLink = this.getCurrentRelativeURL() + htmlLink;
			//	}
			//}
		}

		
		return htmlLink;
	}

	// Adds a HTML link found in the webpage to collection htmlLinks. and adds/updates link in ArrayList urlsToParse
	public void addHtmlLinkFound(String htmlLink) {
		// Get full html version of link
		String fullURL = returnFullURL(htmlLink);
		
		// Check that found URL is within baseRelativeURL
		if (!(fullURL.equals(urlToIgnore)) && fullURL.length()>=baseRelativeURL.length() && fullURL.substring(0, baseRelativeURL.length()).equals(baseRelativeURL)) {
			// Because this link is within the universe we are interested in, check whether it is DISALLOWED
			int disallowLoopCounter = 0;
			boolean bDisallowed = false;
			do
			{
				int smallestLength = 0;
				if (fullURL.length()<disallowedURLs.get(disallowLoopCounter).length())
				{
					smallestLength = fullURL.length();
				}
				else
				{
					smallestLength = disallowedURLs.get(disallowLoopCounter).length();
				}
				
				if (fullURL.substring(0, smallestLength).equals(disallowedURLs.get(disallowLoopCounter).substring(0, smallestLength))) {
					bDisallowed = true;
				}
				disallowLoopCounter++;
			} while ((disallowLoopCounter<disallowedURLs.size()) && (bDisallowed==false));

			if (!bDisallowed) {
				// Because this link is within the universe we are interested in, check whether it is in our arraylist
				int loopCounter = 0;
				boolean bLinkFound = false;
				do
				{
					if (fullURL.equals(visitedURLs.get(loopCounter))) {
						bLinkFound = true;
						
						//This line was trackign links TO pages in the visited set, rather than FROM them
						//visitedURLsVisitedPageCount.set(loopCounter,visitedURLsVisitedPageCount.get(loopCounter)+1);
					}
					loopCounter++;
				} while ((loopCounter<visitedURLs.size()) && (bLinkFound==false));

				if (!bLinkFound) {
					visitedURLs.add(fullURL);
					visitedURLsVisitedPageCount.add(0);
					
					//This line was trackign links TO pages in the visited set, rather than FROM them
					//visitedURLsVisitedPageCount.add(1);
				}

				// 
				visitedURLsVisitedPageCount.set(getArrayPointerToLinkCurrentlyBeingParsed(),visitedURLsVisitedPageCount.get(getArrayPointerToLinkCurrentlyBeingParsed())+1);
				
				// this line superfluous, as all relevant links now added outside this loop??
				// this.htmlLinksFound.add(fullURL);
			}
		}
		
		if (!(fullURL.equals(urlToIgnore))){
			// add url to list of links on this page
			// Do nothing to list of urls to be parsed, as that is dealt with above
			this.htmlLinksFound.add(fullURL);
		}

		// If URL is already in urlsToParse, update number of times found
		//if (urlsToParseOLD.containsKey(fullURL)) {
		//	urlsToParseOLD.put(fullURL, urlsToParseOLD.get(fullURL) + 1);
		//}
		// If URL is not in urlsToParse, then add with one link
		//else {
		//	this.htmlLinksFound.add(fullURL);
		//	urlsToParseOLD.put(fullURL, 1);
		//}

	}

	// Lists the links in the webpage.
	public void listHtmlLinksFound() {
		for (int i = 0; i < this.getHtmlLinksFound().size(); i++) {
			System.out.println((i + 1) + ": " + this.getHtmlLinksFound().get(i));
		}
		System.out.println();
	}

	// Lists the visited pages and number of links to them within all parsed content
	private void listVisitedPagesPerVisitedURL() {
		for (int loopCounter = 0; loopCounter < this.visitedURLs.size(); loopCounter++) {
			System.out.println("<" + this.visitedURLs.get(loopCounter) + ">");
			System.out.println("    <No of links to Visited pages: " + this.visitedURLsVisitedPageCount.get(loopCounter) + ">");
		}
		System.out.println();
	}
	
	// Parses HTML links in the webpage.
	public void parseHtmlLinks(String nextURL) {
		try {
			URL url = new URI(nextURL).toURL();
			URLConnection conn = url.openConnection();
			Reader rd = new InputStreamReader(conn.getInputStream());
			EditorKit kit = new HTMLEditorKit();
			HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
			doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
			kit.read(rd, doc, 0);
			HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);
			while (it.isValid()) {
				SimpleAttributeSet s = (SimpleAttributeSet) it.getAttributes();
				String link = (String) s.getAttribute(HTML.Attribute.HREF);
				if (link != null) {
					this.addHtmlLinkFound(link);
				}
				it.next();
			}
		}
		catch(MalformedURLException e) {
			System.out.println(e);
			System.exit(1);
		}
		catch(URISyntaxException e) {
			System.out.println(e);
			System.exit(1);
		}
		catch(BadLocationException e) {
			System.out.println(e);
			System.exit(1);
		}
		// NB - currently will just fail if URL is not reachable
		catch(IOException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
}