package code;
// WRITTEN BY: Martin O'Shea.

// With thanks to: http://www.javaalmanac.com/egs/javax.swing.text.html/GetLinks.html.

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
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
    private ArrayList<String> htmlLinks;

    // ******************************************************************************************
    // ** PROGRAM.                                                                             **
    // ******************************************************************************************

    public static void main(String args[]) {
        String urlToParse = ("http://www.dcs.bbk.ac.uk/~martin/sewn/ls3/index.html");
        ParseHtmlLinks phl = new ParseHtmlLinks(urlToParse);
        System.out.println("Parsing links from: " + phl.getUrlToParse());
        phl.parseHtmlLinks();
        System.out.println("Links found: ");
        phl.listHtmlLinks();
    }

    // ******************************************************************************************
    // ** CONSTRUCTOR.                                                                         **
    // ******************************************************************************************

    public ParseHtmlLinks(String urlToParse) {
        this.urlToParse = urlToParse;
        this.baseRelativeURL = baseRelativeURLToParse(urlToParse);
        this.htmlLinks = new ArrayList<String>();
    }

	// ******************************************************************************************
    // ** ACCESSOR AND MUTATOR METHODS.                                                        **
    // ******************************************************************************************

    public String getUrlToParse() {
        return urlToParse;
    }

	public String getBaseRelativeURL() {
		return baseRelativeURL;
	}

    public ArrayList<String> getHtmlLinks() {
        return htmlLinks;
    }

    public void setHtmlLinks(ArrayList<String> htmlLinks) {
        this.htmlLinks = htmlLinks;
    }

    // ******************************************************************************************
    // ** HELPER METHODS.                                                                      **
    // ******************************************************************************************

    // Checks format of 'urlToParse' and returns version suitable for use as a base relative URL 
    private String baseRelativeURLToParse(String originalURL) {
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
    			else
    			{
    				// if dot not yet encountered, then 'originalURL' was base relative path, just needing '/'
    				if (!dotEncountered)
    				{
        				baseRelativeURL = originalURL + "/";
        				finishedChecks = true;
    				}
    				else
    				{
    					//'originalURL' was a full filepath - so need to perform sum to get correct base URL
        				baseRelativeURL = originalURL.substring(0, charactersInURL.length-loopCounter);
    				}
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
    	if (!htmlLink.toLowerCase().contains("://"))
    	{
    		htmlLink = this.getBaseRelativeURL() + htmlLink;
    	}
		return htmlLink;
	}
    
    // Adds a HTML link found in the webpage to collection htmlLinks.
    public void addHtmlLink(String htmlLink) {
        this.htmlLinks.add(returnFullURL(htmlLink));
    }

	// Lists the links in the webpage.
    public void listHtmlLinks() {
        for (int i = 0; i < this.getHtmlLinks().size(); i++) {
            System.out.println((i + 1) + ": " + this.getHtmlLinks().get(i));
        }
    }

    // Parses HTML links in the webpage.
    public void parseHtmlLinks() {
        try {
            URL url = new URI(this.getUrlToParse()).toURL();
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
                    this.addHtmlLink(link);
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
        catch(IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }
}