package org.html5index.model;

import java.util.Map;

public interface DocumentationProvider {
  enum Category {
    CONNECTIVITY("Connectivity"), 
    DEVICE_ACCESS("Device Access"), 
    GRAPHICS("Graphics and 3D"), 
    MULTIMEDIA("Multimedia"),
    OFFLINE_STORAGE("Offline and Storage"), 
    PERFORMANCE("Performance"), 
    SEMANTICS("Semantics"), 
    STYLING("Styling");
    private String name;
    Category(String name) {
      this.name = name;
    }
    public String toString() {
      return name;
    }
  }
  
  public String getTitle();
  public Category getCategory();
  public String getSummary(Artifact artifact);
  public String getLink(Artifact artifact);
  public Iterable<String[]> getUrls();
  public Map<String, String> getTutorials();
  public void addDocumentation(Artifact artifact);
  public void readDocumentation(Library lib);
  
}
