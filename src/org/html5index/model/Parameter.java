package org.html5index.model;

public class Parameter extends Artifact {
  public static final int OPTIONAL = 1;
  public static final int VARIADIC = 2;
  Type type;
  Operation owner;
  int modifiers;
  
  public Parameter(String name, Type type, int modifiers) {
    super(name);
    this.type = type;
    this.modifiers = modifiers;
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
