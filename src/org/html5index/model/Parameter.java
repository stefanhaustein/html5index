package org.html5index.model;

public class Parameter extends Artifact {
  Type type;
  Operation owner;
  
  public Parameter(int modifiers, Type type, String name) {
    super(modifiers, name);
    this.type = type;
  }
  
  
  public Type getType() {
    return type;
  }


  @Override
  public Library getLibrary() {
    return owner.getLibrary();
  }
  
  public String getTitle() {
    return (type == null ? "?" : type.getLink()) + " " + name;
  }


  public int getModifiers() {
    return modifiers;
  }


  public void setType(Type newType) {
    this.type = newType;
  }

}
