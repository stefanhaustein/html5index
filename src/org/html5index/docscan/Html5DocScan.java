package org.html5index.docscan;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.html5index.model.Artifact;
import org.html5index.model.DocumentationProvider;
import org.html5index.util.HtmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Html5DocScan implements DocumentationProvider {
  final String title;
  final List<String> urls = new ArrayList<String>();
  final Map<String, String[]> definitions = new HashMap<String, String[]>();
  StringBuilder idl = new StringBuilder();
  Map<String, String> tutorials = new TreeMap<String,String>();
  
  Html5DocScan(String title, String... urls) {
    this.title = title;
    for (String url: urls) {
      fetch(url);
    }
  }


  public String getIdl() {
    return idl.toString();
  }

  /** Guess the matching id for the artifact based on the keys available */
  private String getKey(Artifact artifact) {
    String key = artifact.getQualifiedName();
    int cut = key.indexOf("/");
    key = key.substring(cut + 1).replace(".", "-");
    key = key.replace("Meta<", "").replace(">", "").toLowerCase();

    int dashIndex = key.indexOf('-');
    if (key.startsWith("canvasrenderingcontext2d-") ||
        key.startsWith("canvasdrawingstyles-") ||
        key.startsWith("canvaspathmethods-")) {
      key = "context-2d" + key.substring(dashIndex);
    } else if (key.startsWith("htmlcanvaselement-")) {
      key = "canvas" + key.substring(dashIndex);
    } else if (key.startsWith("globaleventhandler-")) {
      key = "handler" + key.substring(dashIndex);
    } else if (key.startsWith("windoweventhandlers-")) {
      key = "window" + key.substring(dashIndex);
    }

    if (definitions.containsKey(key)) {
      return key;
    }
    if (definitions.containsKey("dom-" + key)) {
      return "dom-" + key;
    }
    if (definitions.containsKey("handler-" + key)) {
      return "handler-" + key;
    }
    if (definitions.containsKey("api-" + key)) {
      return "api-" + key;
    }
    cut = key.indexOf('-');
    if (cut != -1 && definitions.containsKey("dom" + key.substring(cut))) {
      return "dom" + key.substring(cut);
    }
    cut = key.indexOf("element-");
    if (key.startsWith("html") && cut != -1 && cut != 4) {
      String k = "dom-" + key.substring(4, cut) + key.substring(cut + 7);
      if (definitions.containsKey(k)) {
        return k;
      } else {
        System.out.println(key + "->" + k + " Not found");
      }
    }
    
    //System.err.println("Key '" + key + "' not found in " + definitions.keySet());
    return null;
  }


  @Override
  public String getSummary(Artifact artifact) {
    String key = getKey(artifact);
    if (key == null) {
      return null;
    }
    String[] urlAndSummary = definitions.get(key);
    return urlAndSummary == null || urlAndSummary[1].length() == 0 ? null : urlAndSummary[1];
  }

  
  @Override
  public String getLink(Artifact artifact) {
    String key = getKey(artifact);
    if (key == null) {
      return null;
    } 
    return key == null ? null : definitions.get(key)[0] + "#" + key;
  }


  @Override
  public String getTitle() {
    return title;
  }


  @Override
  public Iterable<String> getUrls() {
    return urls;
  }
  
  public Map<String,String> getTutorials() {
    return tutorials;
  }
  
  void fetch(String url) {
    System.out.println(title + ": " + url);
    urls.add(url);
    Document doc = DomLoader.loadDom(url);
    
    // Read idl
    NodeList list = doc.getElementsByTagName("pre");
    for (int i = 0; i < list.getLength(); i++) {
      Element pre = (Element) list.item(i);
      if (pre.getAttribute("class").equals("idl")) {
        idl.append(pre.getTextContent());
        idl.append("\n\n");
      }
    }
    
    // Read summaries
    list = doc.getElementsByTagName("dfn");
    for (int i = 0; i < list.getLength(); i++) {
      Element dfn = (Element) list.item(i);
      String id = dfn.getAttribute("id");
      if (id != null) {
        Node parent = dfn.getParentNode();
        String text = parent.getTextContent();
        String parentName = parent.getNodeName();
        if (parentName.equals("pre") || parentName.startsWith("t")) {
          text = "";
        } else if (parentName.equals("dt")) {
          Node dd = parent.getNextSibling();
          while (dd != null && !dd.getNodeName().equals("dd")) {
            dd = dd.getNextSibling();
          }
          if (dd != null) {
            text = dd.getTextContent();
          } else {
            text = "";
          }
        }
        definitions.put(id.toLowerCase(), new String[]{url, HtmlWriter.summary(text)});
      }
    }
  }
  
  
  Html5DocScan addTutorial(String title, String url) {
    tutorials.put(title, url);
    return this;
  }
}
