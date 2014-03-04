package org.html5index.docscan;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.html5index.idl.IdlParser;
import org.html5index.model.Artifact;
import org.html5index.model.Library;
import org.html5index.model.Type;
import org.html5index.util.HtmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Supports multiple sources; IDL is embedded.
 */
public class Html5SpecScan extends AbstractSpecScan {
  final List<String[]> urls = new ArrayList<String[]>();
  final List<Document> docs = new ArrayList<Document>();
  final Map<String, String[]> definitions = new HashMap<String, String[]>();
  private NodeList currentIdlLinks;
  private int currentIdlLinkIndex;
  final HashMap<String,String> typeIdMap = new HashMap<String, String>();
  boolean fetched = false;

  Html5SpecScan(String title, Category category, String... urls) {
    super(title, category);
    
    for (String url: urls) {
      this.urls.add(new String[]{url, url});
    }
  }

  public Html5SpecScan addTypeIdMap(String... s) {
    for (int i = 0; i < s.length; i += 2) {
      typeIdMap.put(s[i], s[i+1]);
    }
    return this;
  }

  private String typeToId(String name) {
    String id = typeIdMap.get(name);
    if (id != null) {
      return id;
    }
    int cut = name.indexOf("Element");
    if (name.startsWith("HTML") && cut != -1 && cut != 4) {
      id = name.substring(4, cut).toLowerCase();
      if (id.equals("tablerow")) {
        id = "tr";
      } else if (id.equals("tablecell") || id.equals("tabledatacell")) {
        id = "td";
      } else if (id.equals("tableheadercell")) {
        id = "th";
      }
    }
    return id;
  }
  
  private boolean isValidKey(String key) {
    return definitions.containsKey(key);
  }
  
  private final String[] TYPE_ID_PREFIX = {"the-", "widl-", "idl-def-", "interface-", ""};
  private final String[] TYPE_ID_SUFFIX = {"-interface", "-interfaces", "-section", "-element", ""};

  private String getTypeKey(Type type) {
    String name = type.getName();
    if (name.toLowerCase().indexOf("canvas") != -1) {
      // doofl;
      System.out.println("ddd");
    }
    String id = typeToId(name);
    for (String prefix: TYPE_ID_PREFIX) {
      for (String suffix: TYPE_ID_SUFFIX) {
        String key = prefix + name + suffix;
        if (isValidKey(key)) {
          return key;
        }
        key = key.toLowerCase();
        if (isValidKey(key)) {
          return key;
        }
        if (id != null) {
          key = prefix + id + suffix;
          if (isValidKey(key)) {
            return key;
          }
          key = key.toLowerCase();
          if (isValidKey(key)) {
            return key;
          }
        }
      }
    }
    return null;
  }
  
  private String getKey(Artifact artifact) {
    if (artifact instanceof Type) {
      String key = getTypeKey((Type) artifact);
      if (key != null) {
        return key;
      }
    } 
    
    String name = artifact.getName();
    int count = currentIdlLinks.getLength();
    for (int i = 0; i < count; i++) {
      Element a = (Element) currentIdlLinks.item(currentIdlLinkIndex);
      if (a.getTextContent().equals(name)) {
        currentIdlLinkIndex = i;
        String key = a.getAttribute("href");
        int cut = key.indexOf('#');
        if (cut != -1) {
          key = key.substring(cut + 1);
          if (isValidKey(key)) {
            return key;
          }
        }
      }
      currentIdlLinkIndex = (currentIdlLinkIndex + 1) % count;
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
  
  void fetchAll() {
    if (!fetched) {
      fetched = true;
      for (String[] urlAndTitle : urls) {
        urlAndTitle[1] = fetch(urlAndTitle[0]);
      }
    }
  }

  String fetch(String url) {
    System.out.println(title + ": " + url);
    Document doc = DomLoader.loadDom(url);
    String title = url;
    NodeList list = doc.getElementsByTagName("title");
    if (list.getLength() > 0) {
      title = list.item(0).getTextContent();
    }
    docs.add(doc);

    // Read summaries
    list = doc.getElementsByTagName("*");
    for (int i = 0; i < list.getLength(); i++) {
      Element element = (Element) list.item(i);
      String id = element.getAttribute("id");
      if (id != null) {
        while (element.getNodeName().equals("dfn") || element.getNodeName().equals("code")) {
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
          Element dd = getNextElementSibling(element);
          if (dd != null && dd.getNodeName().equals("dd")) {
            Element fc = getFirstElementChild(dd);
            if (fc != null && fc.getNodeName().equals("p")) {
              text = fc.getTextContent();
            } else {
              text = dd.getTextContent();
            }
          } else {
            text = "";
          }
        } else if (name.startsWith("h") && name.length() == 2) {
          int j = 0;
          do {
            element = getNextElementSibling(element);
            if (element == null || element.getNodeName().startsWith("h")) {
              text = "";
              break;
            } else if (element.getNodeName().equals("p")) {
              text = element.getTextContent();
              break;
            } 
          } while(++j < 3);
          if (j == 3) {
            text = "";
          }
        }
        definitions.put(id, new String[]{url, HtmlWriter.summary(text)});
      }
    }
    return title;
  }

  void addIdl(Library lib, String idl, NodeList links) {
    try {
      // Hack... :-/
      currentIdlLinks = links;
      currentIdlLinkIndex = 0;
      new IdlParser(lib, idl).parse();
    } catch(Exception e) {
      System.out.println(idl);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void readDocumentation(Library lib) {
    fetchAll();
    lib.setDocumentationProvider(this);
    
    for (Document doc: docs) {
      // Read idl
      NodeList list = doc.getElementsByTagName("pre");
      for (int i = 0; i < list.getLength(); i++) {
        Element pre = (Element) list.item(i);
        String tc = pre.getTextContent().trim();
        if (pre.getAttribute("class").equals("idl") || 
            tc.startsWith("interface ") || tc.startsWith("partial interface")) {

          tc = tc.replace("createFor()Blob", "createFor(Blob");
          tc = tc.replace("attribute DOMString _camel-cased attribute", "attribute DOMString _camel_cased_attribute");
          addIdl(lib, tc, pre.getElementsByTagName("a"));
        }
      }
    
      if (title.indexOf("File API") != -1) {
        // The file spec uses this idl code annotation. We permit both for the case that this gets fixed...
        list = doc.getElementsByTagName("code");
        for (int i = 0; i < list.getLength(); i++) {
          Element code = (Element) list.item(i);
          if (code.getAttribute("class").equals("idl-code")) {
            String tc = code.getTextContent();
            // Fix known issues
            tc = tc.replace("static DOMString? createFor()Blob blob);", 
                "static DOMString? createFor(Blob blob);");
            addIdl(lib, tc, code.getElementsByTagName("a"));
          }
        }
      }
    }
  }
}
