package org.html5index.docscan;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
  
  public static InputStream openStream(String url) throws IOException {
    InputStream is;
    if (url.startsWith("/")) {
      is = DomLoader.class.getResourceAsStream(url);
    } else {
      URLConnection con = new URL(url).openConnection();
      con.setUseCaches(true);
      is = con.getInputStream();
    }
    return new BufferedInputStream(is);
  }

  static String loadText(String url) throws IOException {
    InputStream is = openStream(url);
    byte[] buf = new byte [8192];
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while (true) {
      int count = is.read(buf);
      if (count <= 0) {
        break;
      }
      baos.write(buf,0, count);
    }
    return new String(baos.toByteArray(), "UTF-8");
  }
  
  public static Document loadDom(String url) {
    Parser parser = new Parser();

    try {
      parser.setFeature(Parser.namespacesFeature, false);
      parser.setFeature(Parser.namespacePrefixesFeature, false);
      InputStream is = openStream(url);
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
