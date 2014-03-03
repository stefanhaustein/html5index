package org.html5index.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;

import org.html5index.docscan.Sources;
import org.html5index.model.DocumentationProvider;
import org.html5index.model.Library;
import org.html5index.model.Model;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Type;


public class JsdocGenerator {

  File root = new File("gen/jsdoc");
  Model model = new Model();
  
  void readModel() throws IOException {
    for (DocumentationProvider provider: Sources.SOURCES) {
      Library lib = new Library(provider.getTitle(), true);
      model.addLibrary(lib);
      provider.readDocumentation(lib);
    }
  }
 
  public void generateAll() throws IOException {
    root.mkdirs();
    for (Library l: model.getLibraries()) {
      generateLibrary(l);
    }
  }
  
  public void generateExport(PrintWriter writer, Type type) throws IOException {
    if (type.getKind() == Type.Kind.INTERFACE) {
      writer.println("goog.provides('" + type.getName() + "')");
    }
  }

  public String jsdocType(Type t) {
    return t.toString();
  }
  
  public void generateOperation(PrintWriter out, Operation op) {
    out.println("/**");
    for (Parameter p: op.getParameters()) {
      out.println(" * @param {" + jsdocType(p.getType()) + "} " + p.getName() + " " + p.getDocumentation());
    }
    if (op.hasModifier(Operation.CONSTRUCTOR)) {
      out.println(" * @constructor");
    } else if (op.getType() != null) {
      out.println(" * @return {" + jsdocType(op.getType()));
    }
    out.println(" */");
    
    out.print(op.getOwner().getName());
    if (!op.hasModifier(Operation.CONSTRUCTOR)) {
      if (!op.hasModifier(Operation.STATIC)) {
        out.print(".prototype");
      }
      out.print(".");
      out.print(op.getName());
    }
    out.print (" = function(");
    boolean first = true;
    for (Parameter p: op.getParameters()) {
      if (first) {
        first = false;
      } else {
        out.print(", ");
      }
      out.print(p.getName());
    }
    out.println(") {};");
    out.println("");
  }
  
  public void generateInterface(PrintWriter out, Type type) {
    Collection<Operation> constructors = type.getConstructors();
    if (constructors.size() == 0) {
      out.println("/**");
      out.println(" * @interface");
      out.println(" */");
      out.println(type.getName() + " = function() {};");
      out.println("");
    } else {
      generateOperation(out, constructors.iterator().next());
    }
    
    for (Operation op: type.getOwnOperations()) {
      generateOperation(out, op);
    }
    
    out.println("");
  }
  
  public void generateType(PrintWriter out, Type type) throws IOException {
    switch (type.getKind()) {
    case INTERFACE: 
      generateInterface(out, type);
      break;
    default:
      out.println("// Skipped: " + type);
    }
  }
  
  public void generateLibrary(Library library) throws IOException {
    File file = new File(root, library.getName() + ".js");
    PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

    for (Type type: library.getTypes()) {
      generateExport(out, type);
    }
    
    out.println("");
    out.println("");

    for (Type type: library.getTypes()) {
      generateType(out, type);
    }
    
    out.close();
  }
  
  
  public static void main(String[] args) throws IOException {
    JsdocGenerator gen = new JsdocGenerator();
    gen.readModel();
    gen.generateAll();
  }
  

  
}
