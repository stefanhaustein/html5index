package org.html5index.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class Type extends Artifact {
  
  public enum Kind {
    CLASS, GLOBAL, INTERFACE, PRIMITIVE, EXCEPTION, DICTIONARY, ALIAS, UNION, ENUM,
    SEQUENCE, ARRAY, NULLABLE
  }
  
  private Kind kind = Kind.INTERFACE;
  private TreeMap<String,Property> properties = new TreeMap<String,Property>();
  private TreeMap<String,Operation> operations = new TreeMap<String,Operation>();
  private TreeSet<Artifact> referencedBy = new TreeSet<Artifact>();
  private Operation constructor;
  Library owner;
  Type superType;
  Type metaType;
  Type metaTypeOf;
  private List<Type> types = new ArrayList<Type>();
  private List<Type> implementedBy = new ArrayList<Type>();
  
  public Type(String name, Kind kind) {
    this(name, kind, null);
  }
  
  public Type(String name, Kind kind, Type baseType) {
    super(name);
    this.kind = kind;
    this.superType = baseType;
  }

  public void addImplemenetedBy(Type type) {
    implementedBy.add(type);
  }
  
  public Type(String name) {
    this(name, Kind.INTERFACE);
  }

  public void setKind(Kind kind) {
    this.kind = kind;
  }
  
  public void setSuperType(Type superType) {
    this.superType = superType;
  }

  public void addOperation(Operation op) {
    operations.put(op.getName(), op);
    op.owner = this;
  }

  public void addType(Type type) {
    types.add(type);
  }
  
  public Collection<Operation> getOwnOperations() {
    return operations.values();
  }

  public Collection<Operation> getOwnAndInterfaceOperations() {
    TreeSet<Operation> set = new TreeSet<Operation>();
    set.addAll(operations.values());
    for (Type t: types) {
      set.addAll(t.getOwnAndInterfaceOperations());
    }
    return set;
  }

  public Collection<Property> getOwnAndInterfaceProperties() {
    TreeSet<Property> set = new TreeSet<Property>();
    set.addAll(properties.values());
    for (Type t: types) {
      set.addAll(t.getOwnAndInterfaceProperties());
    }
    return set;
  }

  
  public List<Type> getImplementedBy() {
    return implementedBy;
  }
  
  public Collection<Type> getTypes() {
    return types;
  }
  
  public void addProperty(Property property) {
    properties.put(property.getName(), property);
    property.owner = this;
  }

  public Collection<Property> getOwnProperties() {
    return properties.values();
  }

  public void setConstructor(Operation constructor) {
    this.constructor = constructor;
    this.kind = Kind.CLASS;
    constructor.owner = this;
  }

  public Operation getConstructor() {
    return constructor;
  }

  @Override
  public Library getLibrary() {
    return owner;
  }

  public Operation getOperation(String name) {
    if (constructor != null && name.equals(constructor.getName())) {
      return constructor;
    }
    return operations.get(name);
  }

  public Type getMetaType() {
    if (metaType == null) {
      metaType = new Type("Meta<" + name + ">");
      metaType.metaTypeOf = this;
      metaType.owner = this.owner;
    }
    return metaType;
  }
  
  public Type getSuperType() {
    return superType;
  }
  
  public String getTitle() {
    if (superType != null) {
      return super.getTitle() + " : " + superType.getLink();
    } 
    return super.getTitle();
  }

  public void setName(String name) {
    if (owner != null) {
      throw new RuntimeException("Can't change name -- owner is set.");
    }
    this.name = name;
  }

  public Property getProperty(String string) {
    return properties.get(name);
  }

  public void removeOperation(Operation operation) {
    assert operation.owner == this;
    operations.remove(operation.getName());
    operation.owner = null;
  }

  public void removeProperty(Property property) {
    assert property.owner == this;
    properties.remove(property.getName());
    property.owner = null;

  }

  public Kind getKind() {
    return kind;
  }

  public void addReference(Artifact artifact) {
    this.referencedBy.add(artifact);
  }

}
