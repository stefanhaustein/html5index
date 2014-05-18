package org.html5index.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.html5index.docscan.DefaultModelReader;
import org.html5index.model.Library;
import org.html5index.model.Model;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Property;
import org.html5index.model.Type;

public class JsonGenerator implements Runnable {
	Model model;
	Writer writer;
	File root = new File("gen/json");
	
	public JsonGenerator(Model model) {
		this.model = model;
	}

	String getQualifiedName(Type type) {
		// TODO: Package name?!
		return type.getName();
	}
	
	String getRef(Type type) {
		return "#descriptions/" + getQualifiedName(type);
	}
	
	void writeType(JsonWriter out, Type type) {
		if (type == null) {
			return;
		}
		String name = type.getName();
		
		// This should really be a a wrapper in the representation (perhaps (type|null) as in Jsdoc?)
		boolean optional = name.endsWith("?");
		if (optional) {
			name = name.substring(0, name.length() - 1);
		}
		
		if (name.equals("any")) {
			out.writeString("type", "object");
			return;
		} 
		if (name.equals("DOMString")) {
			out.writeString("type", "string");
			return;
		}
		if (name.equals("Array")) {
			out.writeString("type", "array");
			return;
		}

		switch(type.getKind()) {
		case PRIMITIVE:
			out.writeString("type", (Arrays.binarySearch(JsdocGenerator.NUMBER_TYPES, name) < 0) ? name : "number");
			break;
		case UNION: 
			out.writeString("type", "object");
			out.openArray("oneOf");
			for (Type t: type.getTypes()) {
				out.openObject();
				writeType(out, t);
				out.closeObject();
			}
			out.closeArray();
			break;
		default:
			out.writeString("type", "object");
			if (!name.equals("Object")) {
				out.writeString("$ref", getRef(type));
			}
		}
	}
	
	public void run() {
		try {
			root.mkdirs();
		      
			File file = new File(root, "index.html");
			PrintWriter indexWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			
			indexWriter.println("<html>");
			indexWriter.println("<head><title>JSON Export</title></head>");
			indexWriter.println("<p><em>Experimental</em> HTML5 API JSON export</p>");
			indexWriter.println("<body>");
			indexWriter.println("<ul>");
			indexWriter.println("<li><a href='ECMAScript.json'>ECMAScript only</a> (use this for experiments and testing)</li>");
			indexWriter.println("<li><a href='HTML5.json'>HTML 5</a></li>");
			indexWriter.println("</ul>");
			indexWriter.println("<p>Please copy the files if you plan to work with them -- ");
			indexWriter.println("hot-linking will probably overload this server.</p>");
			indexWriter.println("<p>For a rough description of the format, please refer to ");
			indexWriter.println("<a href='http://tidej.net/javascript-schema.html'>this link</a>.");
			indexWriter.println("</body>");
			indexWriter.println("</html>");
			indexWriter.close();
			
			generateModel(true);
			generateModel(false);
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}	
	}

	public void generateModel(boolean ecmaOnly) throws IOException {
		String name = ecmaOnly ? "ECMAScript" : "HTML5";
		String fileName = name + ".json";
		
		File file = new File(root, fileName);
		
		JsonWriter out = new JsonWriter(new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
		
		out.writeString("name", name);
		out.writeString("description", "Generated " + new Date() + (ecmaOnly ? "" : " from the HTML5 specifications") + " by the html5index.org generator.");
		out.openObject("definitions");

		// TODO(haustein) Make this more similar to Window...
		ArrayList<Type> globals = new ArrayList<Type>();
		
		for (Library library: model.getLibraries()) {
			if (!ecmaOnly || library.getName().equals("ECMAScript")) {
				for (Type type: library.getTypes()) {
					if (type.getKind() == Type.Kind.GLOBAL) {
						globals.add(type);
					} else {
						generateType(out, type);
					}
				}
			}
		}
		out.closeObject(); // definitions

		out.openObject("globals");
		generateGlobals(out, globals);
		out.closeObject();
		
		out.close();
	}

	void generateProperty(JsonWriter out, Property property) {
		out.openObject(property.getName());
		writeType(out, property.getType());
		out.closeObject();
	}

	void generateParameterList(JsonWriter out, Operation operation) {
		out.openArray("parameter");
		for (Parameter p: operation.getParameters()) {
			out.openObject();
			out.writeString("name", p.getName());
			out.writeBoolean("optional", p.hasModifier(Parameter.OPTIONAL));
			writeType(out, p.getType());
			out.closeObject();
		}
		out.closeArray();
	}

	void generateConstructor(JsonWriter out, Operation operation) {
		out.openObject();
		generateParameterList(out, operation);
		out.closeObject();
	}

	void generateOperation(JsonWriter out, Operation operation) {
		out.openObject(operation.getName());
		out.writeBoolean("static", operation.hasModifier(Operation.STATIC));
		writeType(out, operation.getType());
		generateParameterList(out, operation);
		out.closeObject();
	}
	
	void generateGlobals(JsonWriter out, Collection<Type> globals) {
		out.openObject("properties");
		for (Type g: globals) {
			for (Property p: g.getOwnAndInterfaceProperties()) {
				generateProperty(out, p);
			}
		}
		out.closeObject();

		out.openObject("operations");
		for (Type g: globals) {
			for (Operation op: g.getOwnAndInterfaceOperations()) {
				generateOperation(out, op);
			}
		}
		out.closeObject();
	}
	
	void generateInterface(JsonWriter out, Type type) {
		out.openObject(getQualifiedName(type));

		out.writeString("type", "object");
		out.writeString("library", type.getLibrary().getName());
		if (type.getSuperType() != null) {
			out.writeString("extends", getRef(type.getSuperType()));
		}
		Collection<Type> interfaces = type.getTypes();
		if (interfaces.size() > 0) {
			out.openArray("implements");
			for (Type i: interfaces) {
				out.writeString(getRef(i));
			}
			out.closeArray();
		}
		
		Collection<Property> properties = type.getOwnAndInterfaceProperties();
		if (properties.size() > 0) {
			out.openObject("properties");
			for (Property p: properties) {
				generateProperty(out, p);
			}
			out.closeObject();
		}
		
		Collection<Operation> constructors = type.getConstructors();
		if (constructors.size() > 0) {
			out.openArray("constructors");
			for (Operation o: constructors) {
				generateConstructor(out, o);
			}
			out.closeArray();
		}
		
		Collection<Operation> operations = type.getOwnAndInterfaceOperations();
		if (operations.size() > 0) {
			out.openObject("operations");
			for (Operation o: operations) {
				generateOperation(out, o);
			}
			out.closeObject();
		}
		
		out.closeObject();  // Interface
	}
	
	void generateType(JsonWriter out, Type type) {
		switch(type.getKind()) {
		case INTERFACE:
			generateInterface(out, type);
			break;
		default:
			// do nothing for now...
		}
	}
	
	public static void main(String[] args) {
		JsonGenerator gen = new JsonGenerator(DefaultModelReader.readModel());
	    gen.run();
	}
}
