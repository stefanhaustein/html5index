package org.html5index.model;

import java.util.Map;


public interface DocumentationProvider {
  public String getTitle();
  public String getSummary(Artifact artifact);
  public String getLink(Artifact artifact);
  public String getIdl();
  public Iterable<String[]> getUrls();
  public Map<String, String> getTutorials();
  public void addDocumentation(Artifact artifact);
  
}
