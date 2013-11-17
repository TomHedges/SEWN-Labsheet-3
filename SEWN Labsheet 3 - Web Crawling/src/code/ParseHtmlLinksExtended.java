package code;
// MODIFIED BY: Tom Hedges

// ORIGINALLY WRITTEN BY: Martin O'Shea - original available at "http://www.dcs.bbk.ac.uk/~martin/sewn/ls3/ParseHtmlLinks.java" (accessed 13/11/2013)
// With thanks to: http://www.javaalmanac.com/egs/javax.swing.text.html/GetLinks.html.

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

public class ParseHtmlLinksExtended {

	// ******************************************************************************************
	// ** INSTANCE VARIABLES.                                                                  **
	// ******************************************************************************************

	private String urlToParse;
	private String baseRelativeURL;
	private String currentRelativeURL;
	private String textForCrawlFile;
	private String textForResultsFile;
	private ArrayList<String> disallowedURLs;
	private ArrayList<String> htmlLinksFound;
	private ArrayList<String> urlsToVisit;
	private ArrayList<Integer> visitedURLsVisitedPageCount;
	private int arrayPointerToLinkCurrentlyBeingParsed;
	// indicator for 'links' which should not be listed anywhere! (eg. 'mailto' links)
	final static String urlToIgnore = "DO NOT USE THIS LINK FOR ANYTHING";

	// ******************************************************************************************
	// ** PROGRAM.                                                                             **
	// ******************************************************************************************

	public static void main(String args[]) {

		/// *** HARDCODED SEED URL - please change this to direct crawler at different site
		String urlToParse = ("http://www.dcs.bbk.ac.uk/~martin/sewn/ls3");
		// Feed constructor seed URL
		ParseHtmlLinksExtended phl = new ParseHtmlLinksExtended(urlToParse);

		// Iterate through each link in list to visit, and output results to screen/variable to output text file
		int loopCounter = 1;
		do
		{
			String nextURL = phl.getUrlToParse(loopCounter);
			System.out.println(loopCounter + ".) Parsing links from: " + nextURL);
			phl.setTextForCrawlFile(phl.getTextForCrawlFile() + "<" + nextURL + ">" + System.getProperty("line.separator"));
			phl.setCurrentRelativeURL(nextURL);
			phl.parseHtmlLinks(nextURL);
			System.out.println("Links found: ");
			phl.listHtmlLinksFound();
			phl.htmlLinksFound.clear(); //- NOT NECESSARY?
			loopCounter++;
		} while (phl.getUrlsToParseSize()>=loopCounter);
		System.out.println();

		// output results of crawl to file in local directory
		phl.outputTextFile("crawl.txt", phl.getTextForCrawlFile());
		// list results (links visited and number of links on each to another visited page) 
		phl.listVisitedPagesPerVisitedURL();
	}

	// ******************************************************************************************
	// ** CONSTRUCTOR.                                                                         **
	// ******************************************************************************************

	public ParseHtmlLinksExtended(String urlToParse) {
		this.urlToParse = urlToParse;
		this.baseRelativeURL = parseURLToRelativeForm(urlToParse);
		this.htmlLinksFound = new ArrayList<String>();
		this.disallowedURLs = new ArrayList<String>();

		// Use arraylists to store list of links to be visited and number of links to each
		this.urlsToVisit = new ArrayList<String>();
		this.visitedURLsVisitedPageCount = new ArrayList<Integer>();

		// add initial link
		this.urlsToVisit.add(urlToParse);
		this.visitedURLsVisitedPageCount.add(0);
		// Set pointer for initial page (first)
		this.setArrayPointerToLinkCurrentlyBeingParsed(0);

		// Build disallowed list from "robots.txt"
		this.buildDisallowedList();

		// Set initial blank variables to be output as text files at the end of execution
		this.textForCrawlFile = "";
		this.textForResultsFile = "";
	}

	// ******************************************************************************************
	// ** ACCESSOR AND MUTATOR METHODS.                                                        **
	// ******************************************************************************************

	public String getUrlToParse(int loopCounter) {
		// Set pointer for which link in the "to parse" array is currently being looked at, so that links to be visited can be added to correct total
		this.setArrayPointerToLinkCurrentlyBeingParsed(loopCounter-1);
		// Return URL for parsing
		return this.urlsToVisit.get(loopCounter-1);
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
		return urlsToVisit.size();
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

	private void setTextForCrawlFile(String textForCrawlFile) {
		this.textForCrawlFile = textForCrawlFile;
	}

	private String getTextForCrawlFile() {
		return textForCrawlFile;
	}
	private void setTextForResultsFile(String textForResultsFile) {
		this.textForResultsFile = textForResultsFile;
	}

	private String getTextForResultsFile() {
		return textForResultsFile;
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
		boolean dotEncountered = false;

		do
		{
			if (charactersInURL[loopCounter] == '/')
			{
				// checks if last character in array is a slash
				if (loopCounter == charactersInURL.length-1)
				{
					// if so, then 'originalURL' is already base relative path - so we are finished!
					baseRelativeURL = originalURL;
					return baseRelativeURL;
				}

				// if dot not yet encountered, then 'originalURL' was base relative path, just needing '/' adding to the end
				if (!dotEncountered)
				{
					baseRelativeURL = originalURL + "/";
					return baseRelativeURL;
				}
				else
				{
					// as '.' was encountered before '/' in 'originalURL', it must have been a full filepath - so need to perform sum on character position to get correct base URL
					baseRelativeURL = originalURL.substring(0, loopCounter+1);
					return baseRelativeURL;
				}
			}

			if (charactersInURL[loopCounter] == '.')
			{
				dotEncountered=true;
			}

			loopCounter--;
		} while (loopCounter>0);

		// if URL is well formed, should never reach this return statement
		return baseRelativeURL;
	}

	// Checks the format of a URL, and returns full version if it is relative
	private String returnAbsoluteURL(String htmlLink) {
		// Only return links beginning with 'http://' or 'ftp://'- so ignore anything with other forms of link (eg. 'mailto:')
		if (!htmlLink.toLowerCase().substring(0, 7).equals("http://")  && !htmlLink.toLowerCase().substring(0, 6).equals("ftp://") && htmlLink.toLowerCase().contains(":")) {
			htmlLink = urlToIgnore;
		} else {

			// if link is not already absolute, then make it so
			if (!htmlLink.toLowerCase().substring(0, 7).equals("http://") && !htmlLink.toLowerCase().substring(0, 6).equals("ftp://")) {
				htmlLink = this.getCurrentRelativeURL() + htmlLink;
			}
			// where link contains an "up directory" link, parse through this to achieve correct absolute URL
			while (htmlLink.contains("../")) {
				int pointer = htmlLink.indexOf("../")-1;
				String linkManipulator = htmlLink.substring(0, pointer);
				linkManipulator = linkManipulator.substring(0, linkManipulator.lastIndexOf("/"));
				htmlLink = linkManipulator + htmlLink.substring(pointer+3);
			}
			// remove this reference to the current parent directory for neatness as its use has no relevance to this exercise
			htmlLink = htmlLink.replace("./", "");
		}

		return htmlLink;
	}

	// Adds a HTML link found in the webpage to collection htmlLinks. also adds/updates link in ArrayList urlsToParse
	public void addHtmlLinkFound(String htmlLink) {
		// Get absolute URL
		String fullURL = returnAbsoluteURL(htmlLink);
		// Check that found URL is extension of seed URL, and not to be ignored
		if (!(fullURL.equals(urlToIgnore)) && fullURL.length()>=baseRelativeURL.length() && fullURL.substring(0, baseRelativeURL.length()).equals(baseRelativeURL)) {
			// Because this link is an extension of seed, check whether it is disallowed by 'robots.txt' file
			int disallowLoopCounter = 0;
			boolean bDisallowed = false;
			// iterate through each line of array holding the 'robots.txt' entries
			do {
				// ascertain shortest link, and compare that much of each string
				int smallestLength = 0;
				if (fullURL.length()<disallowedURLs.get(disallowLoopCounter).length())
				{
					smallestLength = fullURL.length();
				}
				else
				{
					smallestLength = disallowedURLs.get(disallowLoopCounter).length();
				}
				// if there is a match, then this link is disallowed by 'robots.txt', so disallow it!
				if (fullURL.substring(0, smallestLength).equals(disallowedURLs.get(disallowLoopCounter).substring(0, smallestLength))) {
					bDisallowed = true;
				}
				disallowLoopCounter++;
			} while ((disallowLoopCounter<disallowedURLs.size()) && (bDisallowed==false));

			if (!bDisallowed) {
				// Because this link is not disallowed, check it its already listed for visiting
				int loopCounter = 0;
				boolean bLinkFound = false;
				do {
					if (fullURL.equals(urlsToVisit.get(loopCounter))) {
						bLinkFound = true;
					}
					loopCounter++;
				} while ((loopCounter<urlsToVisit.size()) && (bLinkFound==false));

				// if link was not found, then add it to the list for visiting, with 0 links from it
				if (!bLinkFound) {
					urlsToVisit.add(fullURL);
					visitedURLsVisitedPageCount.add(0);
				}

				// as we have identified a link to new or known page to be visited, increase the counter for links from the current page
				visitedURLsVisitedPageCount.set(getArrayPointerToLinkCurrentlyBeingParsed(),visitedURLsVisitedPageCount.get(getArrayPointerToLinkCurrentlyBeingParsed())+1);
			}
		}
		// if the fullURL is not to be ignored, then add it to the list of links for this page
		if (!(fullURL.equals(urlToIgnore))){
			// add url to list of links on this page
			// Do nothing to list of urls to be parsed, as that is dealt with above
			this.htmlLinksFound.add(fullURL);
		}
	}

	public void buildDisallowedList() {
		// Read "robots.txt" to fill arraylist of prohibited paths
		try {
			URL url = new URI(this.getBaseRelativeURL() + "robots.txt").toURL();
			InputStream isRobots = url.openStream();
			Scanner scRobots = new Scanner(isRobots);

			while (scRobots.hasNextLine()) {
				String robotsLine = scRobots.nextLine();
				// if this line of robots file contains a slash, then it is a disallowed path, so add to arraylist as absolute path (or beginning of)
				if (robotsLine.contains("/")) {
					robotsLine = robotsLine.substring(robotsLine.indexOf("/")+1);
					this.disallowedURLs.add(this.getBaseRelativeURL() + robotsLine);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.out.println(e);
			System.exit(1);
		} catch (IOException e) {
			System.out.println(e);
			System.exit(1);
		} catch (URISyntaxException e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	// Print the list of pages found on page to screen, and continue building variable which will become text file
	public void listHtmlLinksFound() {
		for (int i = 0; i < this.getHtmlLinksFound().size(); i++) {
			System.out.println((i + 1) + ": " + this.getHtmlLinksFound().get(i));
			this.setTextForCrawlFile(this.getTextForCrawlFile() + "    <" + this.getHtmlLinksFound().get(i) + ">" + System.getProperty("line.separator"));
		}
		System.out.println();
	}

	// builds variable containing the list of visited pages and number of links from them to other visited pages. output to text file
	private void listVisitedPagesPerVisitedURL() {
		for (int loopCounter = 0; loopCounter < this.urlsToVisit.size(); loopCounter++) {
			this.setTextForResultsFile(this.getTextForResultsFile() + "<" + (this.urlsToVisit.get(loopCounter) + ">") + System.getProperty("line.separator"));
			this.setTextForResultsFile(this.getTextForResultsFile() + "    <No of links to Visited pages: " + this.visitedURLsVisitedPageCount.get(loopCounter) + ">" + System.getProperty("line.separator"));
		}
		this.outputTextFile("results.txt", this.getTextForResultsFile());
	}
	// output text file using given variables
	private void outputTextFile(String fileName, String textToOutput) {
		try {
			PrintWriter pwOutput = new PrintWriter(fileName);
			pwOutput.println(textToOutput);
			pwOutput.close();
		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		}
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
		// Will just fail and exit if URL is not reachable
		catch(IOException e) {
			System.out.println(e);
			System.exit(1);
		}
	}
}