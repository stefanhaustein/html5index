package org.html5index.model;

import java.util.Collection;
import java.util.TreeMap;

public class Library extends Artifact {
  
  public static final String GLOBAL_TYPE_NAME = "(Global)";
  
  private TreeMap<String, Type> classes = new TreeMap<String, Type>();
  Model model;
  private final Type globals = new Type(GLOBAL_TYPE_NAME);
  boolean readOnly;
  private DocumentationProvider documentationProvider;
  
  public Library(String name, boolean readOnly, DocumentationProvider documentationProvider) {
    super(name);
    globals.owner = this;
    this.readOnly = readOnly;
    this.documentationProvider = documentationProvider;
  }
  
  public void addType(Type type) {
    Type existing = classes.get(type.getName());
    if (existing != null && existing != type) {
      throw new RuntimeException("Overwriting existing type: " + type);
    }
    classes.put(type.getName(), type);
    type.owner = this;
   }

  public DocumentationProvider getDocumentationProvider() {
    return documentationProvider;
  }
  
  public Collection<String> getTypeNames() {
    return classes.keySet();
  }
  
  public Collection<Type> getTypes() {
    return classes.values();
  }

  public Type getType(String name) {
    return name.equals(GLOBAL_TYPE_NAME) ? globals : classes.get(name);
  }

  @Override
  public Library getLibrary() {
    return this;
  }

  public Model getModel() {
    return model;
  }

  public void deleteType(Type type) {
    assert type.owner == this;
    classes.remove(type.getName());
    type.owner = null;
  }

  public Type getGlobals() {
    return globals;
  }

  public boolean isReadOnly() {
    return readOnly;
  }
}
