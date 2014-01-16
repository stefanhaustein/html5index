package org.html5index.docscan;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.html5index.model.Artifact;
import org.html5index.model.DocumentationProvider;
import org.html5index.model.Member;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Type;
import org.html5index.util.HtmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Html5SpecScan implements DocumentationProvider {
  
  enum IdStyle {
    FILE_API, FILE_SYSTEM_API, WHATWG
  }
  
  final String title;
  final List<String> urls = new ArrayList<String>();
  final Map<String, String[]> definitions = new HashMap<String, String[]>();
  final IdStyle idStyle;
  StringBuilder idl = new StringBuilder();
  Map<String, String> tutorials = new TreeMap<String,String>();
  
  static final String[] ID_PREFIX = {"", "dom-", "handler-", "api-"};
  
  Html5SpecScan(String title, String... urls) {
    this(title, IdStyle.WHATWG, urls);
  }

  Html5SpecScan(String title, IdStyle idStyle, String... urls) {
    this.title = title;
    this.idStyle = idStyle;
    for (String url: urls) {
      fetch(url);
    }
  }


  public String getIdl() {
    return idl.toString();
  }

  // TODO(haustein) Just move statics on the main type and annotate them.
  private static String typeName(Type t) {
    if (t == null) {
      return "void";
    }
    String s = t.getName();
    if (s.startsWith("Meta<")) {
      s = s.substring(5).replace(">", "");
    }
    s = s.replace("?", "").replace("[]", "");
    return s;
  }
  
  private boolean isValidKey(String key) {
    return definitions.containsKey(key);
  }
  
  /** Guess the matching id for the artifact based on the keys available */
  private String getKey(Artifact artifact) {
    if (idStyle == IdStyle.FILE_API) {
      // File Spec style
      String key = "dfn-" + (artifact.getName().equals(artifact.getName().toUpperCase()) ?
          artifact.getName().toLowerCase() : artifact.getName());
      if (definitions.containsKey(key)) {
        return key;
      }
      if (artifact instanceof Type) {
        key = artifact.getName().toLowerCase();
        if (definitions.containsKey(key)) {
          return key;
        }
        key += "-section";
        if (definitions.containsKey(key)) {
          return key;
        }
        key = "idl-def-" + artifact.getName().toLowerCase();
        if (definitions.containsKey(key)) {
          return key;
        }
      }
      return null;
    }

    if (idStyle == IdStyle.FILE_SYSTEM_API) {
      if (artifact instanceof Member) {
        StringBuilder sb = new StringBuilder("widl-");
        Member m = (Member) artifact;
        sb.append(typeName(m.getOwner()));
        sb.append('-');
        sb.append(m.getName());
        if (m instanceof Operation) {
          sb.append('-');
          sb.append(typeName(m.getType()));
          Operation op = (Operation) m;
          for (Parameter p: op.getParameters()) {
            sb.append('-');
            sb.append(typeName(p.getType()));
            sb.append('-');
            sb.append(p.getName());
          }
        }
        String key = sb.toString();
        if (isValidKey(key)) {
          return key;
        }
      } else {
        String key = "the-" + artifact.getName().toLowerCase() + "-interface";
        if (isValidKey(key)) {
          return key;
        }
      }
      return null;
    }

    // WHATWG Style identifiers
    String key = artifact.getQualifiedName();
    int cut = key.indexOf("/");
    key = key.substring(cut + 1).replace(".", "-");
    key = key.replace("Meta<", "").replace(">", "");

    int dashIndex = key.indexOf('-');
    if (key.startsWith("CanvasRenderingContext2d-") ||
        key.startsWith("CanvasDrawingStyles-") ||
        key.startsWith("CanvasPathMethods-")) {
      key = "context-2d" + key.substring(dashIndex);
    } else if (key.startsWith("HTMLCanvasElement-")) {
      key = "canvas" + key.substring(dashIndex);
    } else if (key.startsWith("GlobalEventHandlers-")) {
      key = "handler" + key.substring(dashIndex);
    } else if (key.startsWith("WindowEventHandlers-")) {
      key = "window" + key.substring(dashIndex);
    }

    for (int i = 0; i < 2; i++) {
      for (String prefix: ID_PREFIX) {
        if (isValidKey(prefix + key)) {
          return prefix + key;
        }
      }
      key = key.toLowerCase();
    }

    cut = key.indexOf('-');
    if (cut != -1 && isValidKey("dom" + key.substring(cut))) {
      return "dom" + key.substring(cut);
    }
    cut = key.indexOf("element-");
    if (key.startsWith("html") && cut != -1 && cut != 4) {
      String k = "dom-" + key.substring(4, cut) + key.substring(cut + 7);
      if (isValidKey(k)) {
        return k;
      }
    }
    
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
  
  static boolean isInside(Node node, String name) {
    while (node != null) {
      if (node.getNodeName().equals(name)) {
        return true;
      }
      node = node.getParentNode();
    }
    return false;
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
        String tc = pre.getTextContent();
        idl.append(tc);
        idl.append("\n\n");
      }
    }
    
    // The file spec uses this idl code annotation
    list = doc.getElementsByTagName("code");
    for (int i = 0; i < list.getLength(); i++) {
      Element code = (Element) list.item(i);
      if (code.getAttribute("class").equals("idl-code")) {
        String tc = code.getTextContent();
        // Fix known issues
        tc = tc.replace("static DOMString? createFor()Blob blob);", 
            "static DOMString? createFor(Blob blob);");
        idl.append(tc);
        idl.append("\n\n");
      }
    }

    // Read summaries
    list = doc.getElementsByTagName("*");
    for (int i = 0; i < list.getLength(); i++) {
      Element element = (Element) list.item(i);
      String id = element.getAttribute("id");
      if (id != null) {
        if (element.getNodeName().equals("dfn")) {
          element = (Element) element.getParentNode();
        }
        String name = element.getNodeName();
        String text = element.getTextContent();
        if ("div".equals(name) && "section".equals(element.getAttribute("class"))) {
          NodeList sub = element.getElementsByTagName("p");
          if (sub.getLength() > 0) {
            text = sub.item(0).getTextContent();
          }
        } else if (isInside(element, "pre") || name.startsWith("t")) {
          text = "";
        } else if (name.equals("dt")) {
          Node dd = element.getNextSibling();
          while (dd != null && !dd.getNodeName().equals("dd")) {
            dd = dd.getNextSibling();
          }
          if (dd != null) {
            text = dd.getTextContent();
          } else {
            text = "";
          }
        }
        definitions.put(id, new String[]{url, HtmlWriter.summary(text)});
      }
    }
    System.out.println(definitions.keySet());
  }
  
  
  Html5SpecScan addTutorial(String title, String url) {
    tutorials.put(title, url);
    return this;
  }
}
