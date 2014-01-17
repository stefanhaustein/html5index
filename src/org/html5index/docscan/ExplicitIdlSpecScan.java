package org.html5index.docscan;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.html5index.model.Artifact;
import org.html5index.util.HtmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ExplicitIdlSpecScan extends AbstractSpecScan {
  String title;
  String docUrl;
  String idlUrl;

  HashMap<String,String> index = new HashMap<String, String>();
  HashMap<String,String> summaries = new HashMap<String, String>();
  boolean ecma;
  
  public ExplicitIdlSpecScan(String title, String docUrl, String idlUrl) {
    this.ecma = title.startsWith("ECMA");
    this.title = title;
    this.docUrl = docUrl;
    this.idlUrl = idlUrl;
    System.out.print("Fetching " + docUrl + "...");
    Document doc = DomLoader.loadDom(docUrl);
    System.out.println("Done.");
    scanHeadings(doc.getElementsByTagName(ecma ? "h1" : "*"));
  }
  
  void scanHeadings(NodeList list) {
    String[] currentType = {""};
    for (int i = 0; i < list.getLength(); i++) {
      Element element = (Element) list.item(i);
      String name = element.getNodeName();
      if (name.startsWith("h") && name.length() == 2 && !name.equals("hr")) {
        scanHeading(element, currentType);
      }
    }
    
    System.out.println("index: " + index);
  }
      
  void scanHeading(Element h, String[] currentType) {
    String text = h.getTextContent();
    if (text.length() == 0) {
      return;
    }
    if (text.charAt(0) < '0' || text.charAt(0) > '9') {
      text = "0 " + text;
    }
    String[] parts = text.split("\\s+");
    if (parts.length < 2) {
      return;
    }
    
    System.out.println("heading parts: " + Arrays.toString(parts));
    
    String section = parts[0];
    String id;
    if (ecma) {
      if (!section.startsWith("15.")) {
        return;
      } 
      id = "sec-" + section;
    } else {
      id = h.getAttribute("id");
      if (id == null) {
        return;
      }
    }
    String key = null;
    if (parts.length == 4 && parts[1].equals("The") && (parts[3].equals("Object") ||
        parts[3].equals("Type"))) {
      currentType[0] = key = parts[2];
    } else if (parts.length == 3 && parts[2].equals("Objects")) {
      currentType[0] = key = parts[1];
    } else if (parts.length == 2 || parts[2].startsWith("(")) {
      key = parts[1].replace(".prototype", "");
      int cut = key.indexOf("(");
      if (cut != -1) {
        key = key.substring(0, cut);
      }
      if (key.equals(currentType[0])) {
        key = null;
      } else if (key.indexOf(".") == -1 && (Character.isLowerCase(key.charAt(0))) ||
          key.toUpperCase().equals(key) || key.equals("NaN") || 
          key.equals("Infinity")) {
        if (section.startsWith("15.10.7.")) {
          key = "RexExp." + key;
        } else {
          key = currentType[0] + "." + key;
        }
      }
    }
    if (key != null) {
      System.out.println(key + ": " + index);
      index.put(key, id);
      Node next = h.getNextSibling();
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

  @Override
  public String getTitle() {
    return title;
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
    String id = index.get(key);
    if (id == null) {
      System.out.println("not found: " + key);
      return null;
    }
    return docUrl + "#" + id;
  }

  @Override
  public String getIdl() {
    try {
      return DomLoader.loadText(idlUrl);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Iterable<String> getUrls() {
    return Collections.singleton(docUrl);
  }
}
