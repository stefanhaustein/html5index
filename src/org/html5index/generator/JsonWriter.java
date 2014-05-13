package org.html5index.generator;

import java.io.PrintWriter;

public class JsonWriter {
	PrintWriter out;
	String indent = "  ";
	
	JsonWriter(PrintWriter printWriter) {
		this.out = printWriter;
		out.println("{");
	}
	
	void openObject(String name) {
		out.println(indent + "'" + name + "': {");
		indent = indent + "  ";
	}
	
	void closeObject() {
		indent = indent.substring(0, indent.length() - 2);
		out.println(indent + "},");
	}
	
	void writeString(String name, String value) {
		out.println(indent + "'" + name + "': '" + value + "',");
	}
	
	void close() {
		out.println("}");
		out.close();
	}
}
