package org.html5index.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class Type extends Artifact {
  
  public enum Kind {
    ARRAY_OBJECT, INTERFACE, GLOBAL, NO_INTERFACE_OBJECT, PRIMITIVE, EXCEPTION, DICTIONARY, ALIAS, UNION, 
    ENUM, SEQUENCE, ARRAY, NULLABLE, PARTIAL, CALLBACK_FUNCTION, CALLBACK_INTERFACE
  }

  public static final int ARRAY_CLASS = 256;
  public static final int OVERRIDE_BUILTINS = 512;
  public static final int PRIMARY_GLOBAL = 1024;
  
  private Kind kind = Kind.NO_INTERFACE_OBJECT;
  // TODO: null the objects and create on demand only?
  private TreeMap<String,Property> properties = new TreeMap<String,Property>();
  private TreeMap<String,Operation> operations = new TreeMap<String,Operation>();
  private TreeSet<Member> referencedBy = new TreeSet<Member>();
  private List<Operation> constructors = new ArrayList<Operation>();
  Library owner;
  Type superType;
  private List<Type> types = new ArrayList<Type>();
  private List<Type> implementedBy = new ArrayList<Type>();
  private List<String> enumLiterals = new ArrayList<String>();
  
  public Type(String name, Kind kind) {
    this(name, kind, null);
  }
  
  public Type(String name, Kind kind, Type baseType) {
    super(0, name);
    this.kind = kind;
    this.superType = baseType;
  }

  public void addEnumLiteral(String value) {
    enumLiterals.add(value);
  }
  
  public Collection<String> getEnumLiterals() {
    return enumLiterals;
  }
  
  public void addImplemenetedBy(Type type) {
    implementedBy.add(type);
  }
  
  public Type(String name) {
    this(name, Kind.NO_INTERFACE_OBJECT);
  }

  public void setKind(Kind kind) {
    this.kind = kind;
  }
  
  public void setSuperType(Type superType) {
    this.superType = superType;
    if (this.kind != Kind.UNION && this.kind != Kind.SEQUENCE && this.kind != Kind.NULLABLE) {
      superType.addImplemenetedBy(this);
    }
  }

  public void addOperation(Operation op) {
    operations.put(op.getName(), op);
    if (kind == Kind.PARTIAL) {
      superType.addOperation(op);
    } else {
      op.owner = this;
    }
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
    if (kind == Kind.PARTIAL) {
      superType.addProperty(property);
    } else {
      property.owner = this;
    }
  }

  public Collection<Property> getOwnProperties() {
    return properties.values();
  }

  public Collection<Member> getReferences() {
    return referencedBy;
  }
  
  public void addConstructor(Operation constructor) {
    this.constructors.add(constructor);
    this.kind = Kind.INTERFACE;
    constructor.owner = this;
    
  }

  public Collection<Operation> getConstructors() {
    return constructors;
  }

  @Override
  public Library getLibrary() {
    return owner;
  }

  public Operation getOperation(String name) {
    return operations.get(name);
  }
 
  public Type getSuperType() {
    return superType;
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

  public void addReference(Member member) {
    if (kind == Kind.ARRAY || kind == Kind.SEQUENCE || kind == Kind.NULLABLE) {
      getSuperType().addReference(member);
    } else if (kind == Kind.UNION) {
      for (Type t: types) {
        t.addReference(member);
      }
    } else {
      this.referencedBy.add(member);
      if (kind == Kind.ALIAS) {
        getSuperType().addReference(member);
      }
    }
  }

  public Collection<Library> getExtendedBy() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public String getNameForCompare() {
    return getLibrary() == null ? name : (name + " (" + getLibrary().getName() + ")");
  }

}
