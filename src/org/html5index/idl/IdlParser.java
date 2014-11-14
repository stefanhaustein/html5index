package org.html5index.idl;

import org.html5index.model.Artifact;
import org.html5index.model.DocumentationProvider;
import org.html5index.model.Library;
import org.html5index.model.Model;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Property;
import org.html5index.model.Type;
import org.html5index.model.Type.Kind;
import org.html5index.util.Tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class IdlParser {
  public static final int IGNORE = -1;
  public static final int IGNORE_EQUALS = -2;
  public static final int IGNORE_PAREN = -4;
  
  /**
   * Extended attributes that are stored as simple modifiers in Type or can
   * be ignored. Extended attributes for which we store the value are handled
   * separately in code.
   */
  @SuppressWarnings("serial")
  private static final Map<String, Integer> EXTENDED_ATTRIBUTES = new HashMap<String, Integer>() {
    {
      put("ArrayClass", Type.ARRAY_CLASS);
      put("OverrideBuiltins", Type.OVERRIDE_BUILTINS);
      put("Unforgeable", Artifact.UNFORGEABLE);
 
      // From old spec versions?
      put("Supplemental", IGNORE);
      put("TreatNonCallableAsNull", IGNORE);
      
      // Ignored for now because the spec is wrong
      put("SharedWorker", IGNORE);
      put("DedicatedWorker", IGNORE);
      put("Worker", IGNORE);
      
      // Blink
      put("ActiveDOMObject", IGNORE);
      put("CheckSecurity", IGNORE_EQUALS);
      put("Conditional", IGNORE_EQUALS);
      put("ConstructorCallWith", IGNORE_EQUALS);
      put("Custom", IGNORE_EQUALS);
      put("CustomConstructor", IGNORE);
      put("DependentLifetime", IGNORE);
      put("DoNotCheckConstants", IGNORE);
      put("EventConstructor", IGNORE);
      put("GlobalContext", IGNORE_EQUALS);
      put("NoHeader", IGNORE);
      put("ImplementedAs", IGNORE_EQUALS);
      put("PerContextEnabled", IGNORE);
      put("RaisesException", IGNORE_EQUALS);
      put("RuntimeEnabled", IGNORE_EQUALS);
      put("SetWrapperReferenceTo", IGNORE_PAREN);
      put("SetWrapperReferenceFrom", IGNORE_EQUALS);
      put("StrictTypeChecking", IGNORE); 
      put("SpecialWrapFor", IGNORE_EQUALS);
      put("WillBeGarbageCollected", IGNORE);
      put("CustomConstructor", IGNORE_PAREN);
      put("CheckSecurity", IGNORE_EQUALS);
    }
  };

  
  Tokenizer tokenizer;
  Model model;
  Library lib;
  DocumentationProvider documentationProvider;
  
  public IdlParser(Library lib, String idl) {
    this.model = lib.getModel();
    this.lib = lib;
    this.documentationProvider = lib.getDocumentationProvider();
    tokenizer = new Tokenizer(idl);
  }
 
  Type parseNewTypeName(Type.Kind kind) {
    String name = consumeIdentifier();
    Type type = lib.getType(name);
    if (type == null) {
      type = model.getType(name);
    }
    if (kind == Type.Kind.PARTIAL) {
      if (type != null) {
        if (type.getLibrary() == lib) {
          return type;
        }
        Type newType = new Type(name, kind);
        lib.addType(newType);
        documentationProvider.addDocumentation(newType);
        newType.setSuperType(type);
        return newType;
      } else {
        kind = Type.Kind.INTERFACE;
      }
    }
    if (type == null) {
      type = new Type(name, kind);
      lib.addType(type);
    } 
    type.setKind(kind);
    lib.addType(type);
    documentationProvider.addDocumentation(type);
    return type;
  }
  
  private Type parseInterface(Kind kind) {
    tokenizer.nextToken();
    Type type;
    type = parseNewTypeName(kind);
    if (tokenizer.ttype == ':' || tokenizer.sval.equals("extends")) { // DOM spec error?
      tokenizer.nextToken();
      Type superType = parseType();
      type.setSuperType(superType);
    } 
    if (tokenizer.sval.equals("implements") || tokenizer.sval.equals(",")) {
      do {
        tokenizer.nextToken();
        Type base = parseType();
        type.addType(base);
        base.addImplemenetedBy(type);
      } while (tokenizer.sval.equals(","));
    }
    
    if (tokenizer.ttype == ';') {
      tokenizer.nextToken();
      return type;
    }
    
    consume('{');
    while (tokenizer.ttype != '}') {
      parseOptions();

      int modifiers = 0;
      if ("static".equals(tokenizer.sval)) {
        consumeIdentifier();
        modifiers |= Artifact.STATIC;
      }
      if ("stringifier".equals(tokenizer.sval)) {
        consumeIdentifier();
        if (tokenizer.ttype == ';') {
          consume(';');
          continue;
        }
      } 
      String sval = tokenizer.sval;
      if ("readonly".equals(sval) || "attribute".equals(sval)) {
        Property property = parseProperty(modifiers);
        type.addProperty(property);
        documentationProvider.addDocumentation(property);
      } else if ("const".equals(sval)) {
        Property c = parseConst();
        type.addProperty(c);
        documentationProvider.addDocumentation(c);
      } else if ("serializer".equals(sval)) {
        while (tokenizer.nextToken() != '}');  // TODO: Support serializers
        consume('}');
        consume(';');
      } else if ("typedef".equals(sval)) {
        parseTypedef();
      } else {
        Operation operation = parseOperation(modifiers);
        if (operation != null) {
          Operation old = type.getOperation(operation.getName());
          if (old != null) {
            old.merge(model, operation);
          } else {
            type.addOperation(operation);
            documentationProvider.addDocumentation(operation);
          }
        }
      }
    }
    consume('}');
    if (tokenizer.ttype == ';') {
      consume(';');
    }
    return type;
  }
  
  private Property parseConst() {
    consume(Tokenizer.TT_WORD, "const");
    Type type = parseType();
    String name = consumeIdentifier();
    consume('=');
    StringBuilder sb = new StringBuilder();
    while(tokenizer.ttype != ';' && tokenizer.ttype != Tokenizer.TT_EOF) {
      sb.append(tokenizer.sval);
      sb.append(' ');
      tokenizer.nextToken();
    }
    consume(';');
    return new Property(Property.CONSTANT, type, name, sb.toString().trim());
  }

  private Property parseProperty(int modifiers) {
    if (tokenizer.sval.equals("readonly")) {
      modifiers |= Artifact.READ_ONLY;
      tokenizer.nextToken();
    }
    consume(Tokenizer.TT_WORD, "attribute");
    Type propertyType = parseType();
    
    String name = tokenizer.sval;
    consume(Tokenizer.TT_WORD);
    
    if (tokenizer.sval.equals("setraises") ||  // Used in SVG spec
        tokenizer.sval.equals("raises")) { 
      tokenizer.nextToken();
      consume('(');
      consumeIdentifier(); // exception(?);
      consume(')');
    }
    
    consume(';');
    return new Property(modifiers, propertyType, name, null);
  }
 
  private Operation parseOperation(int modifiers) {
    StringBuilder special = new StringBuilder();
    Operation.Special specialType = Operation.Special.NONE;

    while ("getter".equals(tokenizer.sval) || "setter".equals(tokenizer.sval) ||
        "deleter".equals(tokenizer.sval) || "creator".equals(tokenizer.sval) ||
        "legacycaller".equals(tokenizer.sval)) {
      if ("getter".equals(tokenizer.sval)) {
        specialType = Operation.Special.GETTER;
      } else if ("setter".equals(tokenizer.sval)) {
        specialType = Operation.Special.SETTER;
      }
      if (special.length() > 0) {
        special.append(' ');
      }
      special.append(consumeIdentifier());
    }
    Type type;
    if ("void".equals(tokenizer.sval)) {
      type = null;
      consumeIdentifier();
    } else {
      type = parseType();
    }

    // iterator declaration, not an op
    if (type != null && type.getIterableKeyType() != null) {
      consume(';');
      return null;
    }
    String name;
    if (tokenizer.ttype == '(' && special.length() != 0) {
      name = special.toString();
    } else {
      name = consumeIdentifier();
    }
    Operation op = new Operation(modifiers, type, name);
    op.setSpecial(specialType);
    parseParameterList(op);
    consume(';');
    return op;
  }
  
  private void fail(String msg) {
    tokenizer.fail("Error parsing " + lib.getName() + ": " + msg);
  }
  
  private Type parseUnionType() {
    List<Type> types = new ArrayList<Type>();
    do {
      tokenizer.nextToken();
      types.add(parseType());
    } while(tokenizer.sval.equals("or"));
    consume(')');
    if (tokenizer.ttype == '?') {
      tokenizer.nextToken();
    }
    StringBuilder sb = new StringBuilder("(");
    for (Type t: types) {
      if (sb.length() > 1) {
        sb.append(" or ");
      }
      sb.append(t.getName());
    }
    sb.append(')');
    Type union = new Type(sb.toString());
    union.setKind(Type.Kind.UNION);
    for (Type t: types) {
      union.addType(t);
    }
    return union;
  }
  
  private Type parseType() {
    if (tokenizer.ttype == '(') {
      return parseUnionType();
    }
    
    String name;
    while (true) {
      if (tokenizer.sval.equals("unrestricted")) {
        consumeIdentifier();
        name = "unrestricted " + consumeIdentifier();
      } else if (tokenizer.sval.equals("unsigned")) {
        consumeIdentifier();
        if (tokenizer.ttype == '?') { // This pain occurs in webrtc...
          name = "unsigned int"; // is this right?
        } else {
          name = "unsigned " + consumeIdentifier();
        }
      } else {
        name = consumeIdentifier();
      }
      if (tokenizer.ttype != Tokenizer.TT_SCOPE) {
        break;
      }
      tokenizer.nextToken();
    }
    
    if ("long".equals(tokenizer.sval) && name.endsWith("long")) {
      name += " long";
      consumeIdentifier();
    }
    
    Type type;
    if (name.equals("sequence")) {
      consume('<');
      Type baseType = parseType();
      consume('>');
      type = new Type("sequence<" + baseType.getName() + ">", Type.Kind.SEQUENCE, baseType);
    } else if (name.equals("iterable")) {
      consume('<');
      Type keyType = parseType();
      Type valueType = null;
      if (tokenizer.ttype == ',') {
        valueType = parseType();
      }
      consume('>');
      type = new Type("iterable<" + keyType.getName() + (valueType != null ? "," + valueType.getName() : "") +">", Type.Kind.SEQUENCE, keyType);
      type.setIterable(keyType, valueType);
    } else if (tokenizer.ttype == '<') {
      // parse arbitrary generic
      consume('<');
      Type baseType = parseType();
      String genericSignature = "<" + baseType.getName();
      while (tokenizer.ttype == ',') {
        consume(',');
        Type gType = parseType();
        genericSignature += "," + gType.getName();
      }
      genericSignature += ">";
      consume('>');
      type = new Type(getOrMakeType(name) + genericSignature, Type.Kind.GENERIC, baseType);
    } else {
      type = getOrMakeType(name);
      if (tokenizer.ttype == '?') {
        type = new Type(type.getName() + "?", Type.Kind.NULLABLE, type);
        tokenizer.nextToken();
      }
      if (tokenizer.ttype == '[') {
        tokenizer.nextToken();
        consume(']');
        type = new Type(type.getName() + "[]", Type.Kind.ARRAY, type);
      }
    }
    if (tokenizer.ttype == '?') {
      type = new Type(type.getName() + "?", Type.Kind.NULLABLE, type);
      tokenizer.nextToken();
    }
    return type;
  }

  private Type getOrMakeType(String name) {
    Type type;
    type = lib.getType(name);
    if (type == null) {
      type = model.getType(name);
      if (type == null) {
        type = new Type(name);
        model.addHiddenType(type);
      }
    }
    return type;
  }

  private void consume(int type) {
    if (type != tokenizer.ttype) {
      fail("Expected type: " + tokenizer.ttypeToString(type));
    }
    tokenizer.nextToken();
  }
    
  private void consume(int type, String sval) {
    if (!sval.equals(tokenizer.sval)) {
      fail("Expected: " + sval);
    }
    consume(type);
  }

  private String consumeIdentifier() {
    String s = tokenizer.sval;
    consume(Tokenizer.TT_WORD);
    return s;
  }

  public void parse() {
    tokenizer.nextToken();
    parseModuleBody();
    consume(Tokenizer.TT_EOF);
  }
  
  public void parseModuleBody() {
    while(tokenizer.ttype != Tokenizer.TT_EOF && tokenizer.ttype != '}') {
      String sval = tokenizer.sval;
      if (tokenizer.ttype == '[' || "dictionary".equals(sval) || "exception".equals(sval) ||
          "partial".equals(sval) || "callback".equals(sval) || "interface".equals(sval) || 
          "class".equals(sval)) {
        parseClassifier();
      } else if ("typedef".equals(sval)) {
        parseTypedef();
      } else if ("valuetype".equals(sval)) {
        parseValueType();
      } else if ("module".equals(sval)) {
        parseModule();
      } else if ("const".equals(sval)) {
        lib.getGlobals().addProperty(parseConst());
      } else if ("enum".equals(sval)) {
        parseEnum();
      } else if (tokenizer.ttype == Tokenizer.TT_WORD) {
        Type target = parseType();
        consume(Tokenizer.TT_WORD, "implements");
        Type interfaceType = parseType();
        target.addType(interfaceType);
        interfaceType.addImplemenetedBy(target);
        consume(';');
      } else {
        fail("dictionary, callback, exception, interface, typedef, valuetype, module or const expected");
      }
    }
  }
  
  private void parseDictionary() {
    consume(Tokenizer.TT_WORD, "dictionary");
    Type type = parseNewTypeName(Type.Kind.DICTIONARY);
    if (tokenizer.ttype == ':') {
      tokenizer.nextToken();
      type.setSuperType(parseType());
    } 
    consume('{');
    do {
      Type fieldType = parseType();
      String fieldName = consumeIdentifier();
      String value = null;
      if (tokenizer.ttype == '=') {
        consume('=');
        StringBuilder sb = new StringBuilder();
        while(tokenizer.ttype != ';' && tokenizer.ttype != Tokenizer.TT_EOF) {
          sb.append(tokenizer.sval);
          sb.append(' ');
          tokenizer.nextToken();
        }
        value = sb.toString();
      }
      Property property = new Property(0, fieldType, fieldName, value);
      type.addProperty(property);
      documentationProvider.addDocumentation(property);
      consume(';');
    } while (tokenizer.ttype != '}');
    consume('}');
    consume(';');
  }

  private void parseEnum() {
    consume(Tokenizer.TT_WORD, "enum");
    Type type = parseNewTypeName(Type.Kind.ENUM);
    
    do {
      tokenizer.nextToken();
      if (tokenizer.ttype == '}') {
        break;
      }
      type.addEnumLiteral(tokenizer.sval);
      consume('"');
    } while(tokenizer.ttype == ',');
    consume('}');
    consume(';');
  }
  
  private Type parseException() {
    consume(Tokenizer.TT_WORD, "exception");
    Type type = parseNewTypeName(Type.Kind.EXCEPTION);
    
    if (tokenizer.ttype == ':') {
      tokenizer.nextToken();
      type.setSuperType(parseType());
    }
    
    consume('{');
    
    while(tokenizer.ttype != '}' && tokenizer.ttype != Tokenizer.TT_EOF) {
      if (tokenizer.sval.equals("const")) {
        Property constant = parseConst();
        type.addProperty(constant);
        documentationProvider.addDocumentation(constant);
      } else {
        Type pType = parseType();
        String pName = consumeIdentifier();
        Property property = new Property(0, pType, pName, null);
        type.addProperty(property);
        documentationProvider.addDocumentation(property);
        consume(';');
      }
    }
    consume('}');
    consume(';');
    return type;
  }

  private void parseClassifier() {
    List<Operation> constructors = new ArrayList<Operation>();
    Type.Kind kind = Type.Kind.INTERFACE;
    int modifiers = 0;
    if (tokenizer.ttype == '[') {
      do {
        tokenizer.nextToken();
        if (tokenizer.ttype == ']') {
          // empty classifier
          break;
        }
        String option = consumeIdentifier();
        if ("NoInterfaceObject".equals(option)) {
          if (kind != Type.Kind.GLOBAL) {
            kind = Type.Kind.NO_INTERFACE_OBJECT;
          }
        } else if ("Global".equals(option) || "PrimaryGlobal".equals(option)) {
          kind = Type.Kind.GLOBAL;
          if ("PrimaryGlobal".equals(option)) {
            modifiers |= Type.PRIMARY_GLOBAL;
          }
          if (tokenizer.ttype == '=') {  // I think this case has been replaced with [Exposed]
            consume('=');
            if (tokenizer.ttype == '(') {
              consume('(');
              consumeIdentifier();
              while (tokenizer.ttype == ',') {
                consume(',');
                consumeIdentifier();
              }
              consume(')');
            } else {
              consumeIdentifier();
            }
          }
        } else if ("Callback".equals(option)) {
          consume('=');
          consumeIdentifier();
        } else if ("Constructor".equals(option) || "NamedConstructor".equals(option)) {
          String name = "";
          if (option.equals("NamedConstructor")) {
            consume('=');
            name = consumeIdentifier();
          }
          Operation c = new Operation(Artifact.CONSTRUCTOR, null, name);
          if (tokenizer.ttype == '(') {
            parseParameterList(c);
          }
          constructors.add(c);
        } else if ("Exposed".equals(option)) {
          consume('=');  // TODO(haustein) Fully support this.
          if (tokenizer.ttype == '(') {
            consume('(');
            consumeIdentifier();
            while (tokenizer.ttype == ',') {
              consume(',');
              consumeIdentifier();
            }
            consume(')');
          } else {
            consumeIdentifier();
          }
        } else {
          Integer modifier = EXTENDED_ATTRIBUTES.get(option);
          if (modifier == null) {
            throw new RuntimeException("Unrecognized option: " + option);
          } else if (modifier == IGNORE_EQUALS) {
            if (tokenizer.ttype == '=') {
              consume('=');
              consumeIdentifier();
              // ignore crap like Window&Worker of alg | alg2
              while (tokenizer.ttype == '&' || tokenizer.ttype == '|') {
                consume(tokenizer.ttype);
                consumeIdentifier();
              }
            }
          } else if (modifier == IGNORE_PAREN) {
            if (tokenizer.ttype == '(') {
              consume('(');
              while (tokenizer.ttype != ')') {
                tokenizer.nextToken();
              }
              consume(')');
            }
          } else if (modifier != IGNORE) {
            modifiers |= modifier;
          }
        }
        
      } while(tokenizer.ttype == ',');
      consume(']');
    }
    if ("partial".equals(tokenizer.sval)) {
      kind = Type.Kind.PARTIAL;
      tokenizer.nextToken();
    } else if ("callback".equals(tokenizer.sval)) {
      tokenizer.nextToken();
      if (!tokenizer.sval.equals("interface")) {
        do {
          tokenizer.nextToken();
        } while (tokenizer.ttype != ';');
        tokenizer.nextToken();
        return;
      }
      kind = Type.Kind.CALLBACK_INTERFACE;
    }
    Type type = null;
    if ("interface".equals(tokenizer.sval) || "class".equals(tokenizer.sval)) {
      type = parseInterface(kind);
    } else if ("exception".equals(tokenizer.sval)) {
      type = parseException();
    } else if ("dictionary".equals(tokenizer.sval)) {
      parseDictionary();
    } else {
      throw new RuntimeException(
          "dictionary, interface, callback or exception expected, got: " + tokenizer.sval);
    }
    if (type != null) {
      type.setModifier(modifiers);
      for (Operation constructor: constructors) {
        if (constructor.getName().isEmpty()) {
          constructor.setName(type.getName());
        }
        type.addConstructor(constructor);
        documentationProvider.addDocumentation(constructor);
      }
    }
  }

  enum IgnoreType {
    PAREN, EQUALS, BOOL;
  }

  private void parseModule() {
    consume(Tokenizer.TT_WORD, "module");
    // name
    consumeIdentifier();
    consume('{');
    parseModuleBody();
    consume('}');
    consume(';');
  }

  private void parseParameterList(Operation op) {
    consume('(');
    while(tokenizer.ttype != ')') {
      parseOptions();
      int modifiers = 0;
      if ("optional".equals(tokenizer.sval)) {
        modifiers = Parameter.OPTIONAL;
        consumeIdentifier();
      }
      if ("in".equals(tokenizer.sval) || 
          "out".equals(tokenizer.sval) ||
          "inout".equals(tokenizer.sval)) {
        consumeIdentifier();
      }
      parseOptions();
      
      Type pType = parseType();
      if (tokenizer.ttype == Tokenizer.TT_ELLIPSIS) {
        modifiers |= Parameter.VARIADIC;
        tokenizer.nextToken();
      }
      String pName = consumeIdentifier();
      Parameter parameter = new Parameter(modifiers, pType, pName);
      op.addParameter(parameter);
      
      if (tokenizer.ttype == '=') {
        tokenizer.nextToken();  // =
        if (tokenizer.ttype == '-') {
          tokenizer.nextToken();
        }
        tokenizer.nextToken();  // value
      }
      
      if(tokenizer.ttype == ',') {
        tokenizer.nextToken();
      } else if (tokenizer.ttype != ')') {
        fail("',' or ')' expected");
      }
    }
    consume(')');
    if ("raises".equals(tokenizer.sval)) {
      consumeIdentifier();
      consume('(');
      parseType();
      consume(')');
    }
  }
  
  private void parseOptions() {
    while (tokenizer.ttype == '[') {
      do {
        tokenizer.nextToken();
      } while (tokenizer.ttype != ']');
      tokenizer.nextToken();
    }
  }

  private void parseTypedef() {
    consume(Tokenizer.TT_WORD, "typedef");
  
    Type oldType = parseType();
    Type newType = parseNewTypeName(Kind.ALIAS);
    if (oldType != newType) {
      newType.setSuperType(oldType);
    }
    consume(';');
  }

  private void parseValueType() {
    consume(Tokenizer.TT_WORD, "valuetype");
    Type newType = parseNewTypeName(Type.Kind.PRIMITIVE);
    Type oldType = parseType();
    if (oldType != newType) {
      newType.setSuperType(oldType);
    }
    consume(';');
  }

}