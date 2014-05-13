package org.html5index.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.html5index.docscan.DefaultModelReader;
import org.html5index.model.Library;
import org.html5index.model.Model;
import org.html5index.model.Property;
import org.html5index.model.Type;

public class JsonGenerator implements Runnable {
	Model model;
	Writer writer;
	File root = new File("gen/json");
	
	public JsonGenerator(Model model) {
		this.model = model;
	}

	String jsonType(Type t) {
		// Adjust similar to jsdocType in JsdocGenerator
		return t.toString();
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

	public void generateLibrary(Library library, PrintWriter indexWriter) throws IOException {
		String name = library.getName() + ".json";
		indexWriter.println("<li><a href='" + name + "'>" + name + "</a></li>");  
		
		File file = new File(root, name);
		JsonWriter out = new JsonWriter(new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));

		out.writeString("name", name);
		out.openObject("definitions");
		
		for (Type type: library.getTypes()) {
			generateType(out, type);
		}
		 
		out.closeObject(); // definitions
		out.close();
	}

	void generateProperty(JsonWriter out, Property property) {
		out.openObject(property.getName());
		out.writeString("name", property.getName());
		out.writeString("type", jsonType(property.getType()));
		out.closeObject();
	}
	
	void generateInterface(JsonWriter out, Type type) {
		out.openObject(type.getName());
		out.writeString("type", "Object");
		out.openObject("properties");
		for (Property p: type.getOwnProperties()) {
			generateProperty(out, p);
		}
		out.closeObject();
		out.closeObject();
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
