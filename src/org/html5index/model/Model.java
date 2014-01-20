package org.html5index.model;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Model {
  TreeMap<String, Library> libraries = new TreeMap<String, Library>();
  Operation main;
  Library primitives = new Library("primitives", true, null);
  // Cache for sequence and array types
  Library hidden = new Library("hidden", true, null);

  
  public Model() {
    primitives.addType(new Type("number", Type.Kind.PRIMITIVE));
    primitives.addType(new Type("boolean", Type.Kind.PRIMITIVE));
    primitives.addType(new Type("string", Type.Kind.PRIMITIVE));
    primitives.addType(new Type("object", Type.Kind.PRIMITIVE));
    primitives.addType(new Type("function", Type.Kind.PRIMITIVE));
    // webidl
    primitives.addType(new Type("any", Type.Kind.PRIMITIVE)); 
    
    primitives.addType(new Type("octet", Type.Kind.PRIMITIVE)); 
    
    primitives.addType(new Type("byte", Type.Kind.PRIMITIVE)); 
    primitives.addType(new Type("short", Type.Kind.PRIMITIVE)); 
    primitives.addType(new Type("int", Type.Kind.PRIMITIVE)); 
    primitives.addType(new Type("long", Type.Kind.PRIMITIVE)); 
    primitives.addType(new Type("long long", Type.Kind.PRIMITIVE)); 
    
    primitives.addType(new Type("unsigned byte", Type.Kind.PRIMITIVE)); 
    primitives.addType(new Type("unsigned short", Type.Kind.PRIMITIVE)); 
    primitives.addType(new Type("unsigned int", Type.Kind.PRIMITIVE)); 
    primitives.addType(new Type("unsigned long", Type.Kind.PRIMITIVE)); 
    primitives.addType(new Type("unsigned long long", Type.Kind.PRIMITIVE)); 
    
    primitives.addType(new Type("unrestricted double", Type.Kind.PRIMITIVE));
  }

  
  public Set<String> getAllTypeNames() {
    TreeSet<String> result = new TreeSet<String>();
    for (Library l: libraries.values()) {
      result.addAll(l.getTypeNames());
    }
    result.addAll(primitives.getTypeNames());
    result.addAll(hidden.getTypeNames());
    return result;
  }


  public void addLibrary(Library lib) {
    libraries.put(lib.getName(), lib);
  }
 
  
  public Type getType(String name) {
    Type result = primitives.getType(name);
    if (result == null) {
      result = hidden.getType(name);
      if (result == null || result.getKind() == Type.Kind.PARTIAL) {
        for (Library lib: libraries.values()) {
          result = lib.getType(name);
          if (result != null && result.getKind() != Type.Kind.PARTIAL) {
            break;
          }
          result = null;
        }
      }
    }
    return result;
  }

  public Collection<Library> getLibraries() {
    return libraries.values();
  }


  public void addHiddenType(Type type) {
    hidden.addType(type);
  }


  public Library getLibrary(String libName) {
    return libraries.get(libName);
  }


  public void removeLibrary(Library lib) {
    libraries.remove(lib.getName());
  }


}
