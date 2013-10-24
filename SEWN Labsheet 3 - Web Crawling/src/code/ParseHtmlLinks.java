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
    private ArrayList<String> htmlLinks;

    // ******************************************************************************************
    // ** PROGRAM.                                                                             **
    // ******************************************************************************************

    public static void main(String args[]) {
        String urlToParse = ("http://www.dcs.bbk.ac.uk/~martin/sewn/ls3/testpage.html");
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
        this.htmlLinks = new ArrayList<String>();
    }

    // ******************************************************************************************
    // ** ACCESSOR AND MUTATOR METHODS.                                                        **
    // ******************************************************************************************

    public String getUrlToParse() {
        return urlToParse;
    }

    public void setUtlToParse(String urlToParse) {
        this.urlToParse = urlToParse;
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

    // Adds a HTML link found in the webpage to collection htmlLinks.
    public void addHtmlLink(String htmlLink) {
        this.htmlLinks.add(htmlLink);
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