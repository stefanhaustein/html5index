package org.html5index.docscan;

import java.util.Map;
import java.util.TreeMap;

import org.html5index.model.DocumentationProvider;

public abstract class AbstractSpecScan implements DocumentationProvider {

  Map<String, String> tutorials = new TreeMap<String,String>();
  
  AbstractSpecScan addTutorial(String title, String url) {
    tutorials.put(title, url);
    return this;
  }

  public Map<String,String> getTutorials() {
    return tutorials;
  }
  

}
