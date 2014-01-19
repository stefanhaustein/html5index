package org.html5index.model;

import org.html5index.util.HtmlWriter;

public abstract class Artifact implements Comparable<Artifact> {
  protected String name;
  protected String documentation;
  protected String documentationSummary;
  protected String documentationUrl;

  protected Artifact(String name) {
    this.name = name;
  }
  
  public String getName() {
    return name;
  }
  
  public String getDocumentationSummary() {
    return documentationSummary;
  }
  
  public String getDocumentationLink() {
    return documentationUrl;
  }

  
  public String getDocumentation() {
    return documentation;
  }
  
  public void setDocumentation(String s) {
    this.documentation = s;
  }
  
  public void setDocumentationSummary(String s) {
    this.documentationSummary = s;
  }
  
  public void setDocumentationUrl(String url) {
    this.documentationUrl = url;
  }
  
  public String toString() {
    return name;
  }
  
  public String getTitle() {
    return "<b>" + HtmlWriter.htmlEscape(toString()) + "</b>";
  }
  
  public abstract Library getLibrary();

  public String getQualifiedName() {
    if (getLibrary() == this) {
      return name.replace(' ', '+');
    }
    return getLibrary().getQualifiedName() + "/" + name;
  }
  
  public String getLink() {
    if (getLibrary().getName().equals("hidden") || getLibrary().getName().equals("primitives")) {
      return HtmlWriter.htmlEscape(getName());
    }
    return "<a href='#" + (getLibrary().isReadOnly() ? "Library/" : "Project/") + HtmlWriter.htmlEscape(getQualifiedName()) + "'>" + HtmlWriter.htmlEscape(getName()) + "</a>";
  }

  @Override
  public int compareTo(Artifact other) {
    return getName().compareToIgnoreCase(other.getName());
  }
}
