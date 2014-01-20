package org.html5index.model;

public abstract class Member extends Artifact {
  protected Type type;
  protected Type owner;

  protected Member(int modifiers, Type type, String name) {
    super(modifiers, name);
    this.type = type;
    if (type != null) {
      type.addReference(this);
    }
  }

  public Type getOwner() {
    return owner;
  }

  public Type getType() {
    return type;
  }
  
  public boolean isStatic() {
    return owner.getName().startsWith("Meta<");
  }

  public String getQualifiedName() {
    return owner.getQualifiedName() + "." + name;
  }

  public void setName(String name) {
    if (owner != null) {
      throw new RuntimeException("Can't change name -- owner is set");
    }
    this.name = name;
  }

  @Override
  public Library getLibrary() {
    return owner == null ? null : owner.getLibrary();
  }
  

  public void setType(Type type) {
    this.type = type;
  }
  
  @Override 
  public String getNameForCompare() {
    return owner == null ? name : (name + " (" + owner.getNameForCompare() + ")");
  }
}
