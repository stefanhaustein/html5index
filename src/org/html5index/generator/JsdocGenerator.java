package org.html5index.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;

import org.html5index.docscan.DefaultModelReader;
import org.html5index.model.Artifact;
import org.html5index.model.Library;
import org.html5index.model.Model;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Property;
import org.html5index.model.Type;


public class JsdocGenerator implements Runnable {
  File root = new File("gen/jsdoc");
  Model model;
  
  // MUST BE SORTED for binary search
  static final String[] NUMBER_TYPES = {
	  "int", 
	  "long", 
	  "long long", 
	  "short",
	  "unsigned int",
	  "unsigned long",
	  "unsigned long long",
	  "unsigned short",
  };
  
  public JsdocGenerator(Model model) {
	  this.model = model;
  }
  
  public void run() {
    try {
      root.mkdirs();
      
      File file = new File(root, "index.html");
      PrintWriter indexWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
      
      indexWriter.println("<html>");
      indexWriter.println("<head><title>JsDoc Export</title></head>");
      indexWriter.println("<p><em>Experimental</em> HTML5 API JsDoc export</p>");
      indexWriter.println("<body>");
      indexWriter.println("<ul>");
      
      for (Library l: model.getLibraries()) {
        generateLibrary(l, indexWriter);
      }
      
      indexWriter.println("</ul>");
      indexWriter.println("</body>");
      indexWriter.println("</html>");
      indexWriter.close();
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void generateExport(PrintWriter writer, Type type) throws IOException {
    if (type.getKind() == Type.Kind.INTERFACE) {
      writer.println("goog.provides('" + type.getName() + "')");
    }
  }

  public String jsdocType(Type type) {
	String name = type.getName();
	
	// This should really be a type flag.
	boolean optional = name.endsWith("?");
	if (optional) {
		name = name.substring(0, name.length() - 1);
	}
	
	if (name.equals("any")) {
		name = "*";
	} else if (name.equals("object")) {
		name = "Object";
	} else if (name.equals("DOMString")) {
		name = "string"; 
	}
	
	switch(type.getKind()) {
	case PRIMITIVE:
	   return (Arrays.binarySearch(NUMBER_TYPES, name) < 0) ? name : "number";
	case UNION: 
		StringBuilder sb = new StringBuilder("(");
		for (Type t: type.getTypes()) {
			if (sb.length() > 1) {
				sb.append('|');
			}
			sb.append(jsdocType(t));
		}
		sb.append(')');
		return sb.toString();
	default:
		// TODO: Package name!
		return name;
	}
  }
  
  public String documentation(Artifact a) {
    String result = a.getDocumentationSummary();
    return result == null ? "" : result;
  }
  
  public void generateOperation(PrintWriter out, Operation op) {
	out.println();
    out.println("/**");
    if (op.getDocumentationSummary() != null) {
      out.println(" * " + documentation(op).replace("\n", "\n * "));
      out.println(" *");
    }
    for (Parameter p: op.getParameters()) {
      out.println(" * @param {" + jsdocType(p.getType()) + "} " + p.getName() + " " + documentation(p));
    }
    if (op.hasModifier(Operation.CONSTRUCTOR)) {
      out.println(" * @constructor");
    } else if (op.getType() != null) {
      out.println(" * @return {" + jsdocType(op.getType()) + "}");
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
    out.print(") {");
    
    if (op.hasModifier(Operation.CONSTRUCTOR)) {
    	out.println();
    	generateProperties(out, op.getOwner(), false);
    }
    
    out.println("};");
  }
  
  public void generateProperties(PrintWriter out, Type type, boolean statics) {
	String indent = statics ? "" : "  ";
	for (Property p: type.getOwnProperties()) {
	  if (p.hasModifier(Property.STATIC) != statics) {
		  continue;
	  }
	  out.println();
	  out.println(indent + "/**");
	  if (p.getDocumentation() != null) {
		  out.println(indent + " * " + documentation(p).replace("\n", "\n" + indent + " * "));
	  }
	  out.println(indent + " * @type {" + jsdocType(p.getType()) + "}");
	  out.println(indent + " */");
	  out.println(indent + (statics ? jsdocType(type) : "this") + "." + p.getName() + " = null;");
	}
  }
  
  public void generateInterface(PrintWriter out, Type type) {
    Collection<Operation> constructors = type.getConstructors();
    if (constructors.size() == 0) {
      out.println();
      out.println("/**");
      out.println(" * @interface");
      out.println(" */");
      out.println(type.getName() + " = function() {");
      generateProperties(out, type, false);
      out.println("};");
    } else {
      generateOperation(out, constructors.iterator().next());
    }
    
    generateProperties(out, type, true);

    for (Operation op: type.getOwnOperations()) {
      generateOperation(out, op);
    }    

    out.println();
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
  
  public void generateLibrary(Library library, PrintWriter indexWriter) throws IOException {
	String name = library.getName() + ".js";
	indexWriter.println("<li><a href='" + name + "'>" + name + "</a></li>");  
	
    File file = new File(root, name);
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
  
  
  public static void main(String[] args) {
    JsdocGenerator gen = new JsdocGenerator(DefaultModelReader.readModel());
    gen.run();
  }
}
