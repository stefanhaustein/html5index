package org.html5index.docscan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.html5index.model.Artifact;
import org.html5index.model.DocumentationProvider;
import org.html5index.util.HtmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EcmascriptDomScan implements DocumentationProvider{

  static final String DOC_URL = "http://www.ecma-international.org/ecma-262/5.1/";
  
  HashMap<String,String> index = new HashMap<String, String>();
  HashMap<String,String> summaries = new HashMap<String, String>();
  
  public EcmascriptDomScan() {
    System.out.print("Fetching " + DOC_URL + "...");
    Document doc = DomLoader.loadDom(DOC_URL);
    System.out.println("Done.");
    String currentType = "";
    NodeList list = doc.getElementsByTagName("h1");
    for (int i = 0; i < list.getLength(); i++) {
      Element h1 = (Element) list.item(i);
      String text = h1.getTextContent();
      String[] parts = text.split("\\s+");
      if (parts.length >= 2 && parts[0].startsWith("15.")) {
        String section = parts[0];
        String key = null;
        if (parts.length == 4 && parts[1].equals("The") && parts[3].equals("Object")) {
          currentType = key = parts[2];
        } else if (parts.length == 3 && parts[2].equals("Objects")) {
          currentType = key = parts[1];
        } else if (parts.length == 2 || parts[2].startsWith("(")) {
          key = parts[1].replace(".prototype", "");
          int cut = key.indexOf("(");
          if (cut != -1) {
            key = key.substring(0, cut);
          }
          if (key.equals(currentType)) {
            key = null;
          } else if (key.indexOf(".") == -1 && (Character.isLowerCase(key.charAt(0))) ||
              key.toUpperCase().equals(key) || key.equals("NaN") || 
              key.equals("Infinity")) {
            if (section.startsWith("15.10.7.")) {
              key = "RexExp." + key;
            } else {
              key = currentType + "." + key;
            }
          }
        }
        if (key != null) {
          index.put(key, section);
          Node next = h1.getNextSibling();
          while (next != null && !(next instanceof Element)) {
            next = next.getNextSibling();
          }
          if (next != null && next.getNodeName().equals("p")) {
            String summary = HtmlWriter.summary(next.getTextContent());
            if (summary != null) {
              summaries.put(key, summary);
            }
          }
        }
      }
    }
  }

  @Override
  public String getTitle() {
    return "ECMAScript";
  }

  String getKey(Artifact artifact) {
    String key = artifact.getQualifiedName();
    int cut = key.indexOf("/");
    if (cut != -1) {
      key = key.substring(cut + 1);
    }
    return key.replace("Meta<", "").replace(">", "");
  }
  
  @Override
  public String getSummary(Artifact artifact) {
    return summaries.get(getKey(artifact));
  }

  @Override
  public String getLink(Artifact artifact) {
    String key = getKey(artifact);
    String section = index.get(key);
    if (section == null) {
      System.out.println("not found: " + key);
      return null;
    }
    return DOC_URL + "#sec-" + section;
  }

  @Override
  public String getIdl() {
    try {
      InputStream is = getClass().getResourceAsStream("/idl/ecmascript.idl");
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
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Iterable<String> getUrls() {
    return Collections.singleton(DOC_URL);
  }

  @Override
  public Map<String, String> getTutorials() {
    return null;
  }
}
