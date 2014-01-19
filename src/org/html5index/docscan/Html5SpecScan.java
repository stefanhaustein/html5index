package org.html5index.docscan;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.html5index.model.Artifact;
import org.html5index.model.Member;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Type;
import org.html5index.util.HtmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Supports multiple sources; IDL is embedded.
 */
public class Html5SpecScan extends AbstractSpecScan {
  final String title;
  final List<String[]> urls = new ArrayList<String[]>();
  final Map<String, String[]> definitions = new HashMap<String, String[]>();
  StringBuilder idl = new StringBuilder();
  
  static final String[] ID_PREFIX = {"", "dom-", "handler-", "api-"};

  Html5SpecScan(String title, String... urls) {
    this.title = title;
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
  
  private final String[] TYPE_ID_PREFIX = {"the-", "widl-", "idl-def-", ""};
  private final String[] TYPE_ID_SUFFIX = {"-interface", "-section", ""};

  private String getTypeKey(Type type) {
    for (String prefix: TYPE_ID_PREFIX) {
      for (String suffix: TYPE_ID_SUFFIX) {
        String key = prefix + type.getName() + suffix;
        if (isValidKey(key)) {
          return key;
        }
        key = key.toLowerCase();
        if (isValidKey(key)) {
          return key;
        }
      }
    }
    return null;
  }
  
  private String getMemberKey(Member member) {
    // File API
    String key = "dfn-" + member.getName();
    if (isValidKey(key)) {
      return key;
    }
    // File Writer, File System
    key = key.toLowerCase();
    if (isValidKey(key)) {
      return key;
    }
    StringBuilder sb = new StringBuilder("widl-");
    sb.append(typeName(member.getOwner()));
    sb.append('-');
    sb.append(member.getName());
    if (member instanceof Operation) {
      sb.append('-');
      sb.append(typeName(member.getType()).replace(' ', '-'));
      Operation op = (Operation) member;
      for (Parameter p: op.getParameters()) {
        sb.append('-');
        sb.append(typeName(p.getType()));
        sb.append('-');
        sb.append(p.getName());
      }
    }
    key = sb.toString();
    if (isValidKey(key)) {
      return key;
    }

    // WHATWG
    key = member.getQualifiedName();
    int cut = key.indexOf("/");
    key = key.substring(cut + 1).replace(".", "-");
    key = key.replace("Meta<", "").replace(">", "");

    // Type name map
    int dashIndex = key.indexOf('-');
    if (key.startsWith("CanvasRenderingContext2D-") ||
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

    // Note: Key is lowercase from here on (set in the loop above).
    cut = key.indexOf('-');
    if (cut != -1 && isValidKey("dom" + key.substring(cut))) {
      return "dom" + key.substring(cut);
    }
    cut = key.indexOf("element-");
    if (key.startsWith("html") && cut != -1 && cut != 4) {
      String elementName = key.substring(4, cut);
      if (elementName.equals("tablerow")) {
        elementName = "tr";
      } else if (elementName.equals("tablecell") || elementName.equals("tabledatacell")) {
        elementName = "td";
      } else if (elementName.equals("tableheadercell")) {
        elementName = "th";
      }
      
      if (elementName.startsWith("table") && elementName.length() > 5) {
        elementName = elementName.substring(5);
      }
      
      String memberName = key.substring(cut + 8);
      String k = "dom-" + elementName + "-" + memberName; 
      if (isValidKey(k)) {
        return k;
      }
      // Example: dom-textarea/input-selectiondirection
      k = "dom-" + elementName + "/input-" + memberName;
      if (isValidKey(k)) {
        return k;
      }
      k = "dom-fe-" + memberName;
      if (isValidKey(k)) {
        return k;
      }
      k = "dom-fae-" + memberName;
      if (isValidKey(k)) {
        return k;
      }
    }
    
    return null;
  }
  
  /** Guess the matching id for the artifact based on the keys available */
  private String getKey(Artifact artifact) {
    if (artifact instanceof Type) {
     return getTypeKey((Type) artifact);
    } 
    if (artifact instanceof Member) {
      return getMemberKey((Member) artifact);
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
  public Iterable<String[]> getUrls() {
    return urls;
  }
  
  void fetch(String url) {
    System.out.println(title + ": " + url);
    Document doc = DomLoader.loadDom(url);
    
    String title = url;
    NodeList list = doc.getElementsByTagName("title");
    if (list.getLength() > 0) {
      title = list.item(0).getTextContent();
    }
    
    urls.add(new String[] {url, title});
    
    // Read idl
    list = doc.getElementsByTagName("pre");
    for (int i = 0; i < list.getLength(); i++) {
      Element pre = (Element) list.item(i);
      String tc = pre.getTextContent().trim();
      if (pre.getAttribute("class").equals("idl") || 
          tc.startsWith("interface ") || tc.startsWith("partial interface")) {
        tc = tc.replace("createFor()Blob", "createFor(Blob");
        tc = tc.replace("attribute DOMString _camel-cased attribute", "attribute DOMString _camel_cased_attribute");
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
        } else if (AbstractSpecScan.isInside(element, "pre") || name.startsWith("t")) {
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
        } else if (name.startsWith("h") && name.length() == 2) {
          Node next = element.getNextSibling();
          while (next != null && !(next instanceof Element)) {
            next = next.getNextSibling();
          }
          if (next != null && next.getNodeName().equals("p")) {
            text = next.getTextContent();
          } else {
            text = "";
          }
        }
        definitions.put(id, new String[]{url, HtmlWriter.summary(text)});
      }
    }
  }
  
}
