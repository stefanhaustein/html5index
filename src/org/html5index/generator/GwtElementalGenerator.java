package org.html5index.generator;

import com.squareup.javawriter.JavaWriter;
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
        generateInterface(t);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
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

  public String jsdocType(Type type) {
    if (type == null) {
      return "void";
    }
    String name = type.getName();

    // This should really be a type flag.
    boolean optional = name.endsWith("?");
    if (optional) {
      name = name.substring(0, name.length() - 1);
    }

    if (name.equals("any")) {
      name = "JsObject";
    } else if (name.equals("object")) {
      name = "gwt.JsObject";
    } else if (name.equals("DOMString") || name.equals("string")) {
      name = "String";
    } else if (name.equals("Array")) {
      name = "gwt.JsArray";
    } else if (name.startsWith("sequence<")) {
      name = "gwt.JsArrayLike" + name.substring(name.indexOf('<'));
    } else {
      name = "gwt." + name;
    }

    switch (type.getKind()) {
      case PRIMITIVE:
        return (Arrays.binarySearch(NUMBER_TYPES, name) < 0) ? name : numberType(name);
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
    if (name.contains("unsigned")) {
      name = name.substring("unsigned ".length());
    }
    return name;
  }

  public String documentation(Artifact a) {
    String result = a.getDocumentationSummary();
    return result == null ? "" : result;
  }

  public void generateOperation(JavaWriter writer, Operation op) throws IOException {
    StringBuilder javaDoc = new StringBuilder();

    writer.emitEmptyLine();
    if (op.getDocumentationSummary() != null) {
      javaDoc.append(documentation(op));
    }

    int pcount = 0;
    for (Parameter p : op.getParameters()) {
      javaDoc.append(
          "@param {" + javaType(p) + "} " + name(p, pcount++) + " " + documentation(p) + "\n");
    }

    if (op.hasModifier(Operation.CONSTRUCTOR)) {
      // TODO (cromwellian): handle constructor
    } else if (op.getType() != null) {
      javaDoc.append("@return {" + javaType(op.getType()) + "}\n");
    }

    writer.emitJavadoc(javaDoc.toString());
    List<String> params = new ArrayList<>();
    pcount = 0;
    for (Parameter p : op.getParameters()) {
      params.add(javaType(p.getType()));
      params.add(name(p, pcount++));
    }
    writer.beginMethod(javaType(op.getType()), op.getName(), Collections.emptySet(), params, null);
    writer.endMethod();
  }

  private String name(Parameter p, int paramNum) {
    return p.getName() != null ? p.getName() : "p" + paramNum;
  }

  private String javaType(Type type) {
    return jsdocType(type);
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
        writer.emitAnnotation("JsProperty");
        // getter
        writer.beginMethod(javaType(p.getType()), name(p), Collections.emptySet());
        writer.endMethod();
        // setter
        if (!p.hasModifier(Artifact.READ_ONLY)) {
          writer.beginMethod(javaType(type), name(p), Collections.emptySet(),
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

  private String constantValue(Property prop) {
    return isString(prop) ? JavaWriter.stringLiteral(prop.getInitialValue()) :
        isNumber(prop.getType()) ? numberValue(prop.getInitialValue()) : prop.getInitialValue();
  }

  private boolean isNumber(Type type) {
    return Arrays.binarySearch(NUMBER_TYPES, type.getName()) >= 0;
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
    try (JavaWriter writer = getJavaWriter(type.getName())) {
      Collection<Operation> constructors = type.getConstructors();

        if (type.getDocumentation() != null) {
          writer.emitJavadoc(documentation(type));
        }
        writer.emitPackage("gwt");
        writer.emitAnnotation("JsType" + (constructors.size() == 0 ?
            "" : ("(prototype = \"" + constructors.iterator().next().getName() + "\")")));
        List<String> implementsTypes = new ArrayList<>();
        if (type.getSuperType() != null) {
          implementsTypes.add(javaType(type.getSuperType()));
        }
        for (Type t : type.getTypes()) {
          implementsTypes.add(javaType(t));
        }
      try {
        writer.beginType(javaType(type), "interface", EnumSet.of(Modifier.PUBLIC), null,
            implementsTypes.toArray(new String[implementsTypes.size()]));
        if (constructors.size() == 0) {
        } else {
        }

        generateProperties(writer, type);

        for (Operation op : type.getOwnOperations()) {
          generateOperation(writer, op);
        }
      } finally {
        writer.endType();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private JavaWriter getJavaWriter(String name) throws Exception {
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
