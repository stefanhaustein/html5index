package org.html5index.docscan;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

public class DomLoader {

  static {
    ignoreBadSSLCerts();
  }
  
  public static BufferedReader openReader(String url) throws IOException {
    if (url.startsWith("/")) {
      InputStream inputStream = DomLoader.class.getResourceAsStream(url);
      return new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
    } 
    String cacheName = url.replace(":", "_").replace("/", "_2");
    File cacheFile = new File("cache", cacheName);
    if (cacheFile.exists()) {
      return new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile), "utf-8"));
    }
      
    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
    // normally, 3xx is redirect
    int status = con.getResponseCode();
    if (status != HttpURLConnection.HTTP_OK) {
      if (status == HttpURLConnection.HTTP_MOVED_TEMP
          || status == HttpURLConnection.HTTP_MOVED_PERM
          || status == HttpURLConnection.HTTP_SEE_OTHER) {
        String newUrl = con.getHeaderField("Location");
        return openReader(newUrl);
      }
    }

    if (status >= 400) {
      return new BufferedReader(new StringReader(""));
    }

    System.out.println("Response Code ... " + status);

    // get redirect url from "location" header field
    String contentType = con.getContentType();
    String charSet = "ISO-8859-1";
    if (contentType != null) {
      for (String part: contentType.split(";")) {
        part = part.trim();
        if (part.startsWith("charset=")) {
          charSet = part.substring(8);
          break;
        }
      }
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), charSet));
    String text = loadText(reader);
    reader.close();
    
    new File("cache").mkdir();
    Writer writer = new OutputStreamWriter(new FileOutputStream(cacheFile), "utf-8");
    writer.write(text);
    writer.close();
    
    return new BufferedReader(new StringReader(text));
  }

  static String loadText(String url) throws IOException {
    BufferedReader reader = openReader(url);
    String result = loadText(reader);
    reader.close();
    return result;
  }
  
  static String loadText(BufferedReader reader) throws IOException {
    StringBuilder sb = new StringBuilder();
    String line = reader.readLine();
    if (line != null) {
      sb.append(line);
      while (true) {
        line = reader.readLine();
        if (line == null) {
          break;
        }
        sb.append('\n');
        sb.append(line);
      }
    }
    return sb.toString();
  }

  private static void ignoreBadSSLCerts() {
    try {
      TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

          }
      };

      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      // Create all-trusting host name verifier
      HostnameVerifier allHostsValid = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };
      // Install the all-trusting host verifier
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
  }
  public static Document loadDom(String url) {
    Parser parser = new Parser();

    try {
      parser.setFeature(Parser.namespacesFeature, false);
      parser.setFeature(Parser.namespacePrefixesFeature, false);
      Reader reader = openReader(url);
      DOMResult result = new DOMResult();
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new SAXSource(parser, new InputSource(reader)), result);
      reader.close();
      return (Document) result.getNode();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
   }
}
