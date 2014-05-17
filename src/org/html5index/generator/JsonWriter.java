package org.html5index.generator;

import java.io.PrintWriter;

public class JsonWriter {
	PrintWriter out;
	String indent = " ";
	
	JsonWriter(PrintWriter printWriter) {
		this.out = printWriter;
		out.println("{");
	}
	
	void openObject() {
		out.println(indent + "{");
		indent += " ";
	}
	
	void openObject(String name) {
		out.println(indent + "'" + name + "': {");
		indent += " ";
	}
	
	void closeObject() {
		indent = indent.substring(0, indent.length() - 1);
		out.println(indent + "},");
	}
	
	void writeString(String value) {
		out.println(indent + "'" + value + "',");
	}

	void writeString(String name, String value) {
		out.println(indent + "'" + name + "': '" + value + "',");
	}
	
	void writeBoolean(String name, boolean value) {
		if (value) {
			out.println(indent + "'" + name + "': true,");
		}
	}
	
	void close() {
		out.println("}");
		out.close();
	}

	public void openArray(String name) {
		out.println(indent + "'" + name + "' : [");
		indent += " ";
	}
	
	public void closeArray() {
		indent = indent.substring(0, indent.length() - 1);
		out.println(indent + "],");
	}
}
