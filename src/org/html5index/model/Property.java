package org.html5index.model;

public class Property extends Member {
  String initialValue;
  boolean constant;
  
  public Property(String name, Type type, boolean constant, String initialValue) {
    super(name, type);
    this.initialValue = initialValue;
    this.constant = constant;
  }
  
  public String getInitialValue() {
    return initialValue;
  }

  @Override
  public String getTitle() {
    return type.getLink() + " <b>" + name + "</b>";
  }

  public void setInitialValue(String newInitialValue) {
    this.initialValue = newInitialValue;
  }

  public boolean isConstant() {
    return constant;
  }
  
}
