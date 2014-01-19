package org.html5index.docscan;

import java.util.Map;
import java.util.TreeMap;

import org.html5index.model.Artifact;
import org.html5index.model.DocumentationProvider;
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

}
