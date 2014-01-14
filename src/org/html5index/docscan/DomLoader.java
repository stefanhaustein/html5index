package org.html5index.docscan;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class DomLoader {
  public static Document loadDom(String url) {
    Parser parser = new Parser();

    try {
      parser.setFeature(Parser.namespacesFeature, false);
      parser.setFeature(Parser.namespacePrefixesFeature, false);
      
      URLConnection con = new URL(url).openConnection();
      con.setUseCaches(true);
      InputStream is = new BufferedInputStream(con.getInputStream());
      DOMResult result = new DOMResult();
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new SAXSource(parser, new InputSource(is)), result);
      is.close();
      return (Document) result.getNode();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
   }
}
