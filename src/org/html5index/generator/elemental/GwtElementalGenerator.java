package org.html5index.generator.elemental;

import org.html5index.docscan.DefaultModelReader;
import org.html5index.model.Artifact;
import org.html5index.model.Library;
import org.html5index.model.Model;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Property;
import org.html5index.model.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

/**
 * Generates Elemental2.0 @JsType interfaces for all browser APIs.
 */
public class GwtElementalGenerator implements Runnable {
  File root = new File("gen/elemental");
  Model model;

  // MUST BE SORTED for binary search
  static final String[] NUMBER_TYPES = {
      "int",
      "long",
      "long long",
      "number",
      "octet",
      "short",
      "unsigned int",
      "unsigned long",
      "unsigned long long",
      "unsigned short",
  };

  public GwtElementalGenerator(Model model) {
    this.model = model;
  }

  public void run() {
    try {
      root.mkdirs();
      writePackageInfo("gwt", "Elemental 2.0 interfaces for Web programming.");

      Map<String, Type> mergedTypes = new HashMap<>();
      for (Library l : model.getLibraries()) {
        for (Type t : l.getTypes()) {
          Type merged = mergedTypes.get(t.getName());
          if (merged == null) {
            merged = t;
            mergedTypes.put(t.getName(), t);
          } else {
            for (Property p : t.getOwnProperties()) {
              merged.addProperty(p);
            }
            for (Operation op : t.getOwnOperations()) {
              merged.addOperation(op);
            }
            for (Type impls : t.getTypes()) {
              merged.addType(impls);
            }
            for (Type implBy : t.getImplementedBy()) {
              merged.addImplemenetedBy(implBy);
            }
            for (String e : t.getEnumLiterals()) {
              merged.addEnumLiteral(e);
            }
            for (Operation ctor : t.getConstructors()) {
              merged.addConstructor(ctor);
            }
          }
        }
      }
      for (Type t : mergedTypes.values()) {
        if (t.getKind() == Type.Kind.ALIAS || t.getKind() == Type.Kind.PARTIAL ||
            t.getKind() == Type.Kind.SEQUENCE) {
          continue;
        }
        System.out.println("Writing " + mapJavaType(t));
        generateInterface(t);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      System.out.println("Done");
    }
  }

  private void writePackageInfo(String pkg, String javadoc) throws IOException {
    File pkgDir = new File(root, "gwt");
    pkgDir.mkdirs();
    File file = new File(pkgDir, "package-info.java");
    PrintWriter packageInfo =
        new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
    JavaWriter writer = new JavaWriter(packageInfo);
    writer.emitJavadoc(javadoc);
    writer.emitPackage(pkg);
    writer.close();
  }

  public String mapJavaType(Type type) {
    String name = "void";
    if (type != null) {
      name = type.getName();

      // This should really be a type flag.
      boolean optional = name.endsWith("?");
      if (optional) {
        name = name.substring(0, name.length() - 1);
      }
      if (isNumber(type)) {
        return numberType(type.getName());
      } else if (name.equals("Boolean")) {
        name = "JsBoolean";
      } else if (name.equals("Number")) {
        name = "JsNumber";
      } else if (name.equals("any")) {
        name = "Object";
      } else if (name.equalsIgnoreCase("object")) {
        name = "JsObject";
      } else if (name.equals("DOMString") || name.equals("string")) {
        name = "String";
      } else if (name.equals("Array")) {
        name = "JsArray";
      } else if (name.startsWith("sequence<")) {
        name = "JsArrayLike" + name.substring(name.indexOf('<'));
      }
    }
    return name;
  }

  public String qualifiedType(Type type) {

    String name = mapJavaType(type);
    Type baseType = isArray(type) ? type.getSuperType() : type;
    String baseName = mapJavaType(baseType);

    if (!"String".equals(baseName) && !"Object".equals(baseName) && !isNumber(baseType)) {
      name = "gwt." + name;
    }

    if (type == null) {
      return name;
    }
    switch (type.getKind()) {
      case UNION:
        System.err.println("Union type encountered " + type.getName());
        return name;
      default:
        // TODO: Package javaType!
        return name;
    }
  }

  private String numberType(String name) {
    if (name.contains("long")|| name.equals("number")) {
      return "double";
    }
    if ("octet".equals(name)) {
      return "byte";
    }
    if (name.contains("unsigned")) {
      name = name.substring("unsigned ".length());
    }
    return name;
  }

  public String documentation(Artifact a) {
    String result = a.getDocumentationSummary();
    return result == null ? "" : result;
  }

  public void generateOperation(JavaWriter writer, Operation op, Type enclosingType) throws IOException {
    StringBuilder javaDoc = new StringBuilder();

    writer.emitEmptyLine();
    if (op.getDocumentationSummary() != null) {
      javaDoc.append(documentation(op));
    }
    javaDoc.append("\n");

    int pcount = 0;
    for (Parameter p : op.getParameters()) {
      javaDoc.append("@param " + name(p, pcount++) + " " + documentation(p) + "\n");
    }

    if (op.hasModifier(Operation.CONSTRUCTOR)) {
      // TODO (cromwellian): handle constructor
    }

    writer.emitJavadoc(javaDoc.toString());
    List<String> params = new ArrayList<>();
    pcount = 0;
    for (Parameter p : op.getParameters()) {
      params.add((isGeneric(p, op, pcount) ? "T" : javaType(p.getType())) + (p.isVariadic() ? "..." : ""));
      params.add(name(p, pcount++));
    }
    writer.beginMethod(isGenericGetter(op) ? "T" : javaType(op.getType()), op.getName(),
        op.hasModifier(Operation.STATIC) ? EnumSet.of(Modifier.STATIC) : Collections.emptySet(), params, null);
    if (op.hasModifier(Operation.STATIC)) {
      String args = "";
      String returnStmt = op.getType() == null ? "" : "return ";
      String passedParams = "";
      int i = 0;
      for (Parameter p : op.getParameters()) {
        args += "$" + i;
        passedParams += ", ";
        passedParams += p.getName();
        if (i < op.getParameters().size() - 1) {
          args += ", ";
        }
        i++;
      }
      writer.emitStatement("%sJs.js(\"%s.%s(%s);\"%s)", returnStmt,
          enclosingType.getName(),
          op.getName(),
          args,
          passedParams);
    }
    writer.endMethod();
    if (isGeneric(op.getType())) {
      System.err.println("Rawtype detected, unknown parameter " + op);
    }

    for (Operation overload : op.getOverloads()) {
      generateOperation(writer, overload, enclosingType);
    }
  }

  private boolean isGeneric(Type type) {
    if (type != null) {
      for (Operation op : type.getOwnOperations()) {
        if (isGenericGetter(op)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isGenericGetter(Operation op) {
    return op.getSpecial() == Operation.Special.GETTER && !isNumber(op.getType()) &&
        !"String".equals(javaType(op.getType()));
  }

  private boolean isGeneric(Parameter p, Operation op, int pCount) {
    if (op.getSpecial() == Operation.Special.SETTER && pCount == 0) {
      if (!isNumber(p.getType()) && !"String".equals(javaType(p.getType()))) {
        return true;
      }
    }
    return false;
  }

  private String name(Parameter p, int paramNum) {
    return p.getName() != null ? p.getName() : "p" + paramNum;
  }

  private String javaType(Type type) {
    if (isArray(type) && !isNumber(type.getSuperType())) {
      return "JsArrayLike<" + qualifiedType(handleTypeDef(type.getSuperType())) + ">";
    }
    return qualifiedType(handleTypeDef(type));
  }

  private boolean isArray(Type type) {
    if (type != null && type.getKind() == Type.Kind.ARRAY) {
      return true;
    }
    return false;
  }

  private String javaType(Parameter p) {
    return javaType(p.getType());
  }

  public void generateProperties(JavaWriter writer, Type type) throws Exception {
    int paramCount = 0;
    for (Property p : type.getOwnProperties()) {
      writer.emitEmptyLine();
      if (!p.hasModifier(Artifact.STATIC) && !p.hasModifier(Artifact.CONSTANT)) {
        writer.emitJavadoc(documentation(p));
        maybeNullable(writer, type);
        writer.emitAnnotation("JsProperty");
        // getter
        writer.beginMethod(javaType(p.getType()), name(p), Collections.emptySet());
        writer.endMethod();
        writer.emitEmptyLine();
        // setter
        if (!p.hasModifier(Artifact.READ_ONLY)) {
          writer.emitAnnotation("JsProperty");
          writer.beginMethod((isNullable(type) ? "@Nullable" : "") + javaType(type), name(p), Collections.emptySet(),
              javaType(p.getType()), "value");
          writer.endMethod();
        }
      } else if (p.hasModifier(Artifact.CONSTANT)) {
        writer.emitField(javaType(p.getType()), name(p), EnumSet.of(Modifier.STATIC),
            constantValue(p));
      }
      else {
        // TODO: handle statics
      }
    }
  }

  private void maybeNullable(JavaWriter writer, Type type) throws IOException {
    if (isNullable(type)) {
      writer.emitAnnotation("@Nullable");
    }
  }

  private boolean isNullable(Type type) {
    return type != null && type.getName().endsWith("?");
  }

  private String constantValue(Property prop) {
    return isString(prop) ? JavaWriter.stringLiteral(prop.getInitialValue()) :
        isNumber(prop.getType()) ? numberValue(prop.getInitialValue()) : prop.getInitialValue();
  }

  private boolean isNumber(Type type) {
    if (type == null) {
      return false;
    }
    type = handleTypeDef(type);
    String name = type.getName();
    if (name.endsWith("?")) {
      name = name.substring(0, name.length() - 1);
    }
    return Arrays.binarySearch(NUMBER_TYPES, name) >= 0;
  }

  private Type handleTypeDef(Type type) {
    if (type != null && type.getKind() == Type.Kind.ALIAS) {
      return type.getSuperType();
    }
    return type;
  }

  private String numberValue(String initialValue) {
    if ("inf".equals(initialValue.trim())) {
      return "Double.POSITIVE_INFINITY";
    } else if ("-inf".equals(initialValue.trim())) {
      return "Double.NEGATIVE_INFINITY";
    } else if ("NaN".equals(initialValue.trim())) {
      return "Double.NaN";
    }
    return initialValue;
  }

  private boolean isString(Property prop) {
    return javaType(prop.getType()).equals("String");
  }

  private String name(Property p) {
    return p.getName();
  }

  public void generateInterface(Type type) {
    try (JavaWriter writer = getJavaWriter(mapJavaType(type))) {
      Collection<Operation> constructors = type.getConstructors();


      writer.emitPackage("gwt");
      if (type.getDocumentationSummary() != null) {
        writer.emitJavadoc(documentation(type));
      }
      writer.emitAnnotation("JsType" + (constructors.size() == 0 ?
          "" : ("(prototype = \"" + constructors.iterator().next().getName() + "\")")));
      List<String> implementsTypes = new ArrayList<>();
      if (type.getSuperType() != null) {
        implementsTypes.add(javaType(type.getSuperType()));
      } else if (type.getTypes().isEmpty()) {
        implementsTypes.add("gwt.JsObject");
      }
      for (Type t : type.getTypes()) {
        implementsTypes.add(javaType(t));
      }
      String genericParam = "";
      for (Operation op : type.getOwnOperations()) {
        if (op.getSpecial() == Operation.Special.GETTER && !op.getParameters().isEmpty() &&
            isNumber(op.getParameters().iterator().next().getType())) {
          if (!isNumber(op.getType())) {
            genericParam = "<T extends " + javaType(op.getType()) + ">";
          }
          implementsTypes.add("JsArrayLike" + (!"".equals(genericParam) ? "<T>" : ""));
          writer.emitAnnotation(
              "IterateAsArray(getter = \"" + op.getName() + "\", length=\"length\")");
        }
      }
      try {
        String typeName = javaType(type);
        // we use "String" as return type everywhere, but we still generate a JsString class
        // to represent native String object IDL access not covered by java.lang.String
        if ("String".equals(typeName)) {
          typeName = "JsString";
        }
        writer.beginType(typeName + genericParam, "interface", EnumSet.of(Modifier.PUBLIC), null,
            implementsTypes.toArray(new String[implementsTypes.size()]));
        if (constructors.size() == 0) {
        } else {
        }

        generateProperties(writer, type);

        for (Operation op : type.getOwnOperations()) {
          generateOperation(writer, op, type);
        }
      } finally {
        writer.endType();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private JavaWriter getJavaWriter(String name) throws Exception {
    if ("String".equals(name)) {
      name = "JsString";
    }
    File pkg = new File(root, "gwt");
    pkg.mkdirs();
    File file = new File(pkg, name + ".java");
    PrintWriter pw =
        new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
    return new JavaWriter(pw);
  }

  public void generateType(Type type) throws IOException {
    switch (type.getKind()) {
      case INTERFACE:
        generateInterface(type);
        break;
      default:
        System.out.println("// Skipped: " + type);
    }
  }

  public void generateLibrary(Library library) throws IOException {
    for (Type type : library.getTypes()) {
      generateType(type);
    }
  }

  public static void main(String[] args) {
    GwtElementalGenerator gen = new GwtElementalGenerator(DefaultModelReader.readModel());
    gen.run();
  }
}
