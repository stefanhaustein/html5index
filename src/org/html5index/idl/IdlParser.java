package org.html5index.idl;

import java.util.ArrayList;
import java.util.List;

import org.html5index.model.Library;
import org.html5index.model.Model;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Property;
import org.html5index.model.Type;
import org.html5index.model.Type.Kind;
import org.html5index.util.Tokenizer;


public class IdlParser {
  Tokenizer tokenizer;
  Model model;
  Library lib;
  
  public IdlParser(Model model, Library lib, String idl) {
    this.model = model;
    this.lib = lib;
    tokenizer = new Tokenizer(idl);
  }
 
  Type parseNewTypeName(Type.Kind kind) {
    String name = consumeIdentifier();
    Type type = lib.getType(name);
    if (type == null) {
      type = model.getType(name);
    }
    if (type == null) {
      type = new Type(name, kind);
    } else {
      type.setKind(kind);
    }
    lib.addType(type);
    return type;
  }
  
  private Type parseInterface(Operation constructor) {
    //consume(Tokenizer.TT_WORD, "interface");
    tokenizer.nextToken();
    Type type;
    String specKey = null;
    type = parseNewTypeName(Type.Kind.CLASS);
    if (constructor != null) {
      constructor.setName(type.getName());
      type.setConstructor(constructor);
    }
    
    if (tokenizer.ttype == ':' || tokenizer.sval.equals("extends")) { // DOM spec error?
      tokenizer.nextToken();
      Type superType = parseType();
      type.setSuperType(superType);
    }
    
    if (tokenizer.ttype == ';') {
      tokenizer.nextToken();
      return type;
    }
    
    consume('{');
    while (tokenizer.ttype != '}') {
      parseOptions();

      Type owner = type;
      if ("static".equals(tokenizer.sval)) {
        consumeIdentifier();
        owner = owner.getMetaType();
      }
      String sval = tokenizer.sval;
      if ("stringifier".equals(sval)) {
        consumeIdentifier();
        consume(';');
      } else if ("readonly".equals(sval) || "attribute".equals(sval)) {
        Property property = parseProperty();
        property.setNameQualifier(specKey);
        owner.addProperty(property);
      } else if ("const".equals(sval)) {
        Property c = parseConst();
        c.setNameQualifier(specKey);
        type.getMetaType().addProperty(c);
      } else {
        Operation operation = parseOperation();
        Operation old = owner.getOperation(operation.getName());
        operation.setNameQualifier(specKey);
        if (old != null) {
          old.merge(model, operation);
        } else {
          owner.addOperation(operation);
        }
      }
    }
    consume('}');
    consume(';');
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
    Property property = new Property(name, type, true, sb.toString().trim());
    property.setDocumentationProvider(lib.getDocumentationProvider());
    return property;
  }

  private Property parseProperty() {
    if (tokenizer.sval.equals("readonly")) {
      tokenizer.nextToken();
    }
    consume(Tokenizer.TT_WORD, "attribute");
    Type propertyType = parseType();
    
    String name = tokenizer.sval;
    consume(Tokenizer.TT_WORD);
    consume(';');
    Property property = new Property(name, propertyType, false, null);
    property.setDocumentationProvider(lib.getDocumentationProvider());
    return property;
  }
 
  private Operation parseOperation() {
    StringBuilder special = new StringBuilder();
    while ("getter".equals(tokenizer.sval) || "setter".equals(tokenizer.sval) ||
        "deleter".equals(tokenizer.sval) || "creator".equals(tokenizer.sval) ||
        "legacycaller".equals(tokenizer.sval)) {
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
    String name;
    if (tokenizer.ttype == '(' && special.length() != 0) {
      name = special.toString();
    } else {
      name = consumeIdentifier();
    }
    Operation op = new Operation(name, type);
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
        name = "unsigned " + consumeIdentifier();
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
    
    if (name.equals("sequence")) {
      consume('<');
      Type baseType = parseType();
      consume('>');
      return new Type("sequence<" + baseType.getName() + ">", Type.Kind.SEQUENCE, baseType);
    }
    
    Type type = lib.getType(name);
    if (type == null) {
      type = model.getType(name);
      if (type == null) {
        type = new Type(name);
        model.addHiddenType(type);
      }
    }

    if (tokenizer.ttype == '?') {
      type = new Type(type.getName() + "?", Type.Kind.NULLABLE, type);
      tokenizer.nextToken();
    }
    if (tokenizer.ttype == '[') {
      tokenizer.nextToken();
      consume(']');
      type = new Type(type.getName() + "[]", Type.Kind.ARRAY, type);
    } 
    if (tokenizer.ttype == '?') {
      type = new Type(type.getName() + "?", Type.Kind.NULLABLE, type);
      tokenizer.nextToken();
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
      if (tokenizer.ttype == '[') {
        parseWithOptions();
      } else if ("dictionary".equals(sval)) {
        parseDictionary();
      } else if ("partial".equals(sval)) {
        consumeIdentifier();
      } else if ("callback".equals(sval)) {
        parseCallback();
      } else if ("interface".equals(sval) || "class".equals(sval)) {
        parseInterface(null);
      } else if ("typedef".equals(sval)) {
        parseTypedef();
      } else if ("valuetype".equals(sval)) {
        parseValueType();
      } else if ("module".equals(sval)) {
        parseModule();
      } else if ("const".equals(sval)) {
        lib.getGlobals().addProperty(parseConst());
      } else if ("exception".equals(sval)) {
        parseException();
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
        fail("exception, interface, typedef, valuetype, module or const expected");
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
      Property property = new Property(fieldName, fieldType, false, value);
      property.setDocumentationProvider(lib.getDocumentationProvider());
      type.addProperty(property);
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
        type.getMetaType().addProperty(parseConst());
      } else {
        Type pType = parseType();
        String pName = consumeIdentifier();
        type.addProperty(new Property(pName, pType, false, null));
        consume(';');
      }
    }
    consume('}');
    consume(';');
    return type;
  }

  private void parseCallback() {
    consumeIdentifier();
    if ("interface".equals(tokenizer.sval)) {
      parseInterface(null);
    } else {
      // TODO
      do {
        tokenizer.nextToken();
      } while (tokenizer.ttype != ';');
      tokenizer.nextToken();
    }
  }
  
  private void parseWithOptions() {
    // Precondition: on '[';
    Operation result = null;
    boolean global = false;
    boolean noInterfaceObject = false;
    do {
      tokenizer.nextToken();
      String option = consumeIdentifier();
      if ("NoInterfaceObject".equals(option)) {
        noInterfaceObject = true;
      } else if ("Global".equals(option)) {
        global = true;
      } else if ("Callback".equals(option)) {
        consume('=');
        consumeIdentifier();
      } else if ("ArrayClass".equals(option) || 
          "TreatNonCallableAsNull".equals(option) ||
          "OverrideBuiltins".equals(option) ||
          "Unforgeable".equals(option) ||
          "Supplemental".equals(option)) {
      } else if ("Constructor".equals(option) || "NamedConstructor".equals(option)) {
        String name = "";
        if (option.equals("NamedConstructor")) {
          consume('=');
          name = consumeIdentifier();
        }
        Operation c = new Operation(name, null);
        c.setDocumentationProvider(lib.getDocumentationProvider());
        if (tokenizer.ttype == '(') {
          parseParameterList(c);
          if (result == null) {
            result = c;
          } else {
            result.merge(model, c);
          }
        }
      } else {
        throw new RuntimeException("Unrecognized option: " + option);
      }
    } while(tokenizer.ttype == ',');
    consume(']');
    
    if ("partial".equals(tokenizer.sval)) {
      tokenizer.nextToken();
    }
    if ("interface".equals(tokenizer.sval)) {
      Type type = parseInterface(result);
      if (global) {
        type.setKind(Type.Kind.GLOBAL);
      } else if (noInterfaceObject) {
        type.setKind(Type.Kind.INTERFACE);
      }
    } else if ("callback".equals(tokenizer.sval)) {
      parseCallback();
    } else if ("exception".equals(tokenizer.sval)) {
      Type type = parseException();
      if (result != null) {
        type.setConstructor(result);
      }
      
    } else {
      throw new RuntimeException(
          "interface, callback or exception expected, got: " + tokenizer.sval);
    }
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
    op.setDocumentationProvider(lib.getDocumentationProvider());
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
      Parameter parameter = new Parameter(pName, pType, modifiers);
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