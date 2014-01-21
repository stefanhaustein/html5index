package org.html5index.docscan;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.html5index.idl.IdlParser;
import org.html5index.model.Artifact;
import org.html5index.model.Library;
import org.html5index.util.HtmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Used for the ECMAScript spec and Khronos specs. Khronos specs dynamically create
 * the TOC (and the corresponding ids) at runtime.
 */
public class ExplicitIdlSpecScan extends AbstractSpecScan {
  String title;
  String specUrl;
  String idlUrl;
  String specTitle;
  int [] sectionNumber = new int[4];

  HashMap<String,String> index = new HashMap<String, String>();
  HashMap<String,String> summaries = new HashMap<String, String>();
  boolean ecma;
  
  public ExplicitIdlSpecScan(String title, String docUrl, String idlUrl) {
    this.ecma = title.startsWith("ECMA");
    this.title = title;
    this.specUrl = docUrl;
    this.idlUrl = idlUrl;
    System.out.print("Fetching " + docUrl + "...");
    Document doc = DomLoader.loadDom(docUrl);
    System.out.println("Done.");
    
    specTitle = docUrl;
    NodeList list = doc.getElementsByTagName("title");
    if (list.getLength() > 0) {
      specTitle = list.item(0).getTextContent();
    }
    
    scanHeadings(doc.getElementsByTagName(ecma ? "h1" : "*"));
  }
  
  void scanHeadings(NodeList list) {
    String[] currentType = {""};
    for (int i = 0; i < list.getLength(); i++) {
      Element element = (Element) list.item(i);
      String name = element.getNodeName();
      if (name.equals("h1") || name.equals("h2") || name.equals("h3") || name.equals("h4")) {
        scanHeading(element, name.charAt(1) - 48, currentType);
      }
    }
    
    System.out.println("index: " + index);
  }
      
  void scanHeading(Element h, int level, String[] currentType) {
    if ("no-toc".equals(h.getAttribute("class"))) {
      return;
    }
    String text = h.getTextContent();
    if (text.length() == 0) {
      return;
    }

    String id;
    String[] words = text.split("\\s+");
    if (ecma) {
      int cut = text.indexOf(' ');
      if (cut == -1) {
        return;
      }
      String section = words[0];
      if (!section.startsWith("15.")) {
        return;
      } 
      String[] tmp = new String[words.length - 1];
      System.arraycopy(words, 1, tmp, 0, tmp.length);
      words = tmp;
      text = text.substring(cut + 1);
      id = "sec-" + section;
    } else {
      sectionNumber[level - 1]++;
      for (int i = level; i < sectionNumber.length; i++) {
        sectionNumber[i] = 0;
      }
      StringBuilder sb = new StringBuilder();
      for (int i = 1; i < level; i++) {
        if (i > 1) {
          sb.append('.');
        }
        sb.append(sectionNumber[i]);
      }
      id = sb.toString();
    }
    if (words.length == 0) {
      return;
    }
    
    String key = null;
    if (words.length == 3 && words[0].equals("The") && (words[2].equals("Object") ||
        words[2].equals("Type"))) {
      currentType[0] = key = words[1];
    } else if (words.length == 2 && words[1].equals("Objects")) {
      currentType[0] = key = words[0];
    } else if (words.length == 1 || words[1].startsWith("(")) {
      key = words[0].replace(".prototype", "");
      int cut = key.indexOf("(");
      if (cut != -1) {
        key = key.substring(0, cut);
      }
      if (key.equals(currentType[0])) {
        key = null;
      } else if (key.indexOf(".") == -1 && (Character.isLowerCase(key.charAt(0))) ||
          key.toUpperCase().equals(key) || key.equals("NaN") || 
          key.equals("Infinity")) {
        if (id.startsWith("sec-15.10.7.")) {
          key = "RexExp." + key;
        } else {
          key = currentType[0] + "." + key;
        }
      }
    }
    if (key != null) {
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
    return specUrl + "#" + id;
  }

  @Override
  public Iterable<String[]> getUrls() {
    return Collections.singleton(new String[]{specUrl, specTitle});
  }

  @Override
  public void readDocumentation(Library lib) {
    try {
      lib.setDocumentationProvider(this);
      String idl =  DomLoader.loadText(idlUrl);
      new IdlParser(lib, idl).parse();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
