package org.html5index.docscan;

import java.util.Map;
import java.util.TreeMap;

import org.html5index.model.Artifact;
import org.html5index.model.DocumentationProvider;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class AbstractSpecScan implements DocumentationProvider {

  Map<String, String> tutorials = new TreeMap<String,String>();
  
  AbstractSpecScan addTutorial(String title, String url) {
    tutorials.put(title, url);
    return this;
  }

  public Map<String,String> getTutorials() {
    return tutorials;
  }
  
  
  public void addDocumentation(Artifact artifact) {
    artifact.setDocumentationSummary(getSummary(artifact));
    artifact.setDocumentationUrl(getLink(artifact));
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

  static Element getNextElementSibling(Node n) {
    do {
      n = n.getNextSibling();
    } while (n != null && !(n instanceof Element));
    return (Element) n;
  }
  
  static Element getFirstElementChild(Node n) {
    n = n.getFirstChild();
    if (n == null || (n instanceof Element)) {
      return (Element) n;
    }
    return getNextElementSibling(n);
  }
  
}
