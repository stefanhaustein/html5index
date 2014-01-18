package org.html5index.htmlgenerator;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import org.html5index.docscan.Sources;
import org.html5index.idl.IdlParser;
import org.html5index.model.DocumentationProvider;
import org.html5index.model.Library;
import org.html5index.model.Model;
import org.html5index.model.Operation;
import org.html5index.model.Parameter;
import org.html5index.model.Property;
import org.html5index.model.Type;
import org.html5index.util.HtmlWriter;

public class Generator {

  Model model = new Model();
  
  static HtmlWriter createWriter(String name) throws IOException {
    HtmlWriter writer = new HtmlWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream("gen/"+ name +".html"), "utf-8")));
    writer.markup("<html><head><title>");
    writer.text(name);
    writer.markup("</title>\n");
    writer.markup("<link rel='icon' type='image/png' href='html5index-miniicon.png'>\n");
    writer.markup("<link rel='stylesheet' type='text/css' href='style.css'>\n");
    writer.markup("</head>\n<body>");
    return writer;
  }
  
  static void closeWriter(HtmlWriter writer) throws IOException {
    writer.markup("</body></html>\n");
    writer.close();
  }
  
  void readModel() throws IOException {
    for (DocumentationProvider provider: Sources.SOURCES) {
      Library lib = new Library(provider.getTitle(), true);
      lib.setDocumentationProvider(provider);
      String idl = provider.getIdl();
      
      
      try {
        new IdlParser(model, lib, idl).parse();
      } catch(Exception e) {
        System.out.println(idl);
        throw new RuntimeException(e);
      }
      model.addLibrary(lib);
    }
  }
  
  public void writeAll() throws IOException {
    writeIndex();
    writeAbout();
    writeModel();
  }
  
  public void writeModel() throws IOException {
    HtmlWriter writer = createWriter("Libraries");

    writer.markup("<h3>Libraries</h3>\n");

    writer.markup("<p><a href='All Types.html' target='lib'>All</a></p>");
    
    writer.markup("\n<ul class='plain'>\n");
    for (Library lib: model.getLibraries()) {
      writer.markup("<li><a href='").text(lib.getName() + ".html").markup("' target='lib'>");
      writer.text(lib.getName());
      writer.markup("</a></li>\n");
      writeLibrary(lib);
    }
    writer.markup("</ul>\n");
    closeWriter(writer);
    
    writeAllTypesIndex();
  }
  
  public void writeIndex() throws IOException {
    HtmlWriter writer = new HtmlWriter(new OutputStreamWriter(new FileOutputStream("gen/index.html")));
    writer.markup("<html><head><title>HTML 5 Javascript API Index</title>\n");
    writer.markup("<link rel='icon' type='image/png' href='html5index-miniicon.pn'>\n");
    writer.markup("</head>");
    writer.markup("<frameset cols='20%,80%'>\n");
    writer.markup("<frameset rows='40%,60%'>\n");
    writer.markup("<frame src='Libraries.html' name='overview' title='Libraries'>\n");
    writer.markup("<frame src='All Types.html' name='lib' title='Library Overview'>\n");
    writer.markup("</frameset>\n");
    writer.markup("<frame src='about.html' name='type' title='Type Description' scrolling='yes'>\n");
    writer.markup("</frameset>\n");
    writer.markup("<body>\n");
    writeAboutContent(writer, false);
    closeWriter(writer);
  }
  
  
  public void writeLinkedType(HtmlWriter writer, Type type) throws IOException {
    if (type == null) {
      writer.text("void");
    } else if (type.getKind() == Type.Kind.UNION) {
      boolean first = true;
      writer.text("(");
      for (Type t: type.getTypes()) {
        if (first) {
          first = false;
        } else {
          writer.text(" or ");
        }
        writeLinkedType(writer, t);
      }
      writer.text(")");
    } else if (type.getKind() == Type.Kind.PRIMITIVE || 
        (type.getLibrary() != null && type.getLibrary().getName().equals("hidden"))) {
      writer.text(type.getName());
    } else if (type.getKind() == Type.Kind.ARRAY) {
      writeLinkedType(writer, type.getSuperType());
      writer.text("[]");
    } else if (type.getKind() == Type.Kind.NULLABLE) {
      writeLinkedType(writer, type.getSuperType());
      writer.text("?");
    } else if (type.getKind() == Type.Kind.SEQUENCE) {
      writer.text("sequence<");
      writeLinkedType(writer, type.getSuperType());
      writer.text(">");
    } else {
      writer.markup("<a href='").text(type.getLibrary().getName() + " - " + type.getName() + ".html").markup("' target='type'>");
      writer.text(type.getName());
      writer.markup("</a>");
    }
  }
  

  public void writeLibrary(Library lib) throws IOException {
    HtmlWriter writer = createWriter(lib.getName());
    writer.markup("<h3><a href='").text(lib.getName() + " - Overview.html").markup("' target='type'>");
    writer.text(lib.getName());
    writer.markup("</a></h3>\n");

    writeTypesIndex(writer, lib.getTypes());
    closeWriter(writer);
    
    writer = createWriter(lib.getName() + " - Overview");
    writeHeader(writer, null);
    writer.markup("<h2>").text(lib.getName()).markup(" Overview</h2>");
    Map<String,String> tutorials = lib.getDocumentationProvider().getTutorials();
    if (tutorials != null && tutorials.size() > 0) {
      writer.markup("<h3>Tutorials</h3><ul>");
      for (Map.Entry<String, String> entry : tutorials.entrySet()) {
        writer.markup("<li><a href='").text(entry.getValue());
        if (entry.getValue().startsWith("https:")) {
          writer.markup("' target='_top'>");
        } else {
          writer.markup("'>");
        }
        writer.text(entry.getKey());
        writer.markup("</a></li>");
      }
      writer.markup("</ul>");
    }

    writer.markup("<h3>Specification(s)</h3>");
    writer.markup("</p><ul>");
    for (String url: lib.getDocumentationProvider().getUrls()) {
      writer.markup("<li><a href='").text(url).markup("'>");
      writer.text(url);
      writer.markup("</a></li>");
    } 
    writer.markup("</ul>");

    writer.markup("<h3>Declared Types</h3>");
    writer.markup("<table><tr>");
    for (Type.Kind kind: Type.Kind.values()) {
      if (listKind(kind) > 0) {
        writer.markup("<th>").text(kind.toString()).markup("</th>");
      }
    }
    writer.markup("</tr><tr>");
    for (Type.Kind kind: Type.Kind.values()) {
      if (listKind(kind) > 0) {
        writer.markup("<td><ul class='plain'>");
        for (Type type: lib.getTypes()) {
          if (type.getKind() == kind) {
            writer.markup("<li>");
            writeLinkedType(writer, type);
            writer.markup("</li>");
          }
        }
        writer.markup("</ul></td>");
      }
    }
    writer.markup("</tr></table>");
    closeWriter(writer);
  }
  
  int listKind(Type.Kind kind) {
    if (kind == Type.Kind.GLOBAL || kind == Type.Kind.CLASS) {
      return 2;
    }
    if (kind == Type.Kind.DICTIONARY || kind == Type.Kind.ENUM || 
        kind == Type.Kind.INTERFACE) {
      return 1;
    }
    return 0;
  }
  
  public void writeAllTypesIndex() throws IOException {
    HtmlWriter writer = createWriter("All Types");
    writer.markup("<h3><a href='about.html' target='type'>All Types</a></h3>");
    TreeSet<Type> all = new TreeSet<Type>();
    for (Library lib: model.getLibraries()) {
      all.addAll(lib.getTypes());
    }
    writeTypesIndex(writer, all);

    closeWriter(writer);
  }
  
  public void writeTypesIndex(HtmlWriter writer, Iterable<Type> list) throws IOException {
  //  ArrayList<Type> globals = new ArrayList<Type>();
    ArrayList<Type> types = new ArrayList<Type>();
    
    for (Type t: list) {
      if (listKind(t.getKind()) > 1) {
        types.add(t);
      }
      writeType(t);
    }

    /*
    if (globals.size() > 0) {
      writer.markup("<h4>Global</h4>");
      writer.markup("<ul class='plain'>\n");
      for (Type type: globals) {
        writer.markup("<li>");
        writeLinkedType(writer, type);
        writer.markup("</li>\n");
      }
      writer.markup("</ul>\n");
    } */
    if (types.size() > 0) {
      writer.markup("<ul class='plain'>\n");
      for (Type type: types) {
        writer.markup("<li>");
        writeLinkedType(writer, type);
        writer.markup("</li>\n");
      }
      writer.markup("</ul>\n");
    }
  }
  
  public void writeHeader(HtmlWriter writer, Library lib) throws IOException {
    writer.markup("<small>");
    writer.markup("<a href='index.html' target='_top'>HTML5 JS API Index</a> &ndash; ");
    if (lib != null) {
      writer.markup("<a href='").text(lib.getName()).markup(".html'>");
      writer.text(lib.getName());
      writer.markup("</a> &ndash; ");
    }
    writer.markup("</small>");
  }
  
  
  public void writeType(Type type) throws IOException {
    HtmlWriter writer = createWriter(type.getLibrary().getName() + " - " + type.getName());
    
    writeHeader(writer, type.getLibrary());
    
    writer.markup("<h2>");

    String url = type.getDocumentationLink();
    if (url != null) {
      writer.markup("<a href='").text(url).markup("'>");
      writer.text(type.getName());
      writer.markup("</a>");
    } else {
      writer.text(type.getName());
    }

    writer.markup("</h2>\n");
    
    switch(type.getKind()) {
    case INTERFACE: 
      writer.markup(
          "<p>This type groups properties and / or operations together for documentation " +
          "purposes and does not have an explicit Javascript representation.</p>");
      break;
    case DICTIONARY:
      writer.markup(
          "<p>This type represents a collection of object properties and does not have " +
          "an explicit Javascript representation.</p>");
      break;
    case ENUM:
      writer.markup("<p>(Enum types still need to be documented here.)</p>");
      break;
    }
    
    if (type.getSuperType() != null) {
      writer.markup("<p>");
      if (type.getKind() == Type.Kind.ALIAS) {
        writer.text("Alias for ");
      } else {
        writer.text("Extends ");
      }
      writeLinkedType(writer, type.getSuperType());
      writer.markup(".</p>");
    }

    if (type.getTypes().size() != 0) {
      writer.markup("<p>").text("Implements ");
      boolean first = true;
      for (Type t: type.getTypes()) {
        if (first) {
          first = false;
        } else {
          writer.text(", ");
        }
        writeLinkedType(writer, t);
      }
      writer.markup(".</p>");
    }
    
    if (type.getImplementedBy().size() != 0) {
      writer.markup("<p>").text("Implemented by ");
      boolean first = true;
      for (Type t: type.getImplementedBy()) {
        if (first) {
          first = false;
        } else {
          writer.text(", ");
        }
        writeLinkedType(writer, t);
      }
    }
    
    if (type.getDocumentationSummary() != null) {
      writer.markup("<p>").text(type.getDocumentationSummary()).markup("</p>");
    }
    
    ArrayList<Property> constants = new ArrayList<Property>();
    ArrayList<Property> staticProperties = new ArrayList<Property>();
    
    for (Property p: type.getMetaType().getOwnProperties()) {
      if (p.isConstant()) {
        constants.add(p);
      } else {
        staticProperties.add(p);
      }
    }

    Collection<Property> properties = type.getOwnAndInterfaceProperties();
    if (properties.size() != 0 || type.getMetaType().getOwnProperties().size() != 0) {
      writer.markup("<table class='members'><tr><th colspan='2'>Properties</th></tr>\n");
      writeProperties(writer, constants);
      writeProperties(writer, staticProperties);
      writeProperties(writer, properties);
      writer.markup("</table>");
    }
    
    if (type.getConstructor() != null) {
      writer.markup("<table class='members'><tr><th>Constructor</th></tr>");
      String constructorName = type.getConstructor().getName();
      if (constructorName.equals("")) {
        constructorName = type.getName();
      }
      writer.markup("<tr><td>").text(constructorName);
      writeParams(writer, type.getConstructor());
      writer.markup("</td></tr></table>");
    }

    Collection<Operation> operations = type.getOwnAndInterfaceOperations();
    if (operations.size() != 0 || type.getMetaType().getOwnOperations().size() != 0) {
      writer.markup("<table class='members'><tr><th colspan='2'>Operations</th></tr>\n");
      writeOperations(writer, type.getMetaType().getOwnOperations());
      writeOperations(writer, operations);
      writer.markup("</table>");
    }
    closeWriter(writer);
  }

  void writeAbout() throws IOException {
    HtmlWriter writer = createWriter("about");
    writeAboutContent(writer, true);
    closeWriter(writer);
  }
  
  void writeAboutContent(HtmlWriter writer, boolean inFrame) throws IOException {
    writer.markup("<div style='float:right;padding:10px'><img src='html5index.png'></div>");
    writer.markup("<h2>HTML 5 Javascript API Index</h2>\n");
    
    writer.markup("<p>");
    writer.text("This HTML 5 Javascript API Index is generated by extracting and parsing ");
    writer.text("IDL fragments from the various HTML 5 specifications. ");
    writer.markup("</p><p>\n");
    
    if (inFrame) {
      writer.text("This site relies on HTML frames. If you don't see the index of libraries ");
      writer.text("and types to the left of this document, load the full frame set by ");
      writer.text("following this ");
      writer.markup("<a href='index.html' target='_top'>link</a>.</p>\n");
    } else {
      writer.text("This site relies on HTML frames, but your browser does not seem ");
      writer.text("to support frames. You may want to try a different browser to ");
      writer.text("view this site.");
    }
    writer.markup("<h3>Covered Libraries</h3>\n<p>");
    
    boolean first = true;
    for (Library lib: model.getLibraries()) {
      if (first) {
        first = false;
      } else {
        writer.text(", ");
      }
      writer.markup("<a href='").text(lib.getName() + " - Overview.html").markup("'>");
      writer.text(lib.getName());
      writer.markup("</a>");
    }
    writer.markup("</p><hr><center>\n");
    
    writer.markup("<script async src='//pagead2.googlesyndication.com/pagead/js/adsbygoogle.js'></script>");
    writer.markup("<!-- about -->");
    writer.markup("<ins class='adsbygoogle'");
    writer.markup("     style='display:inline-block;width:728px;height:90px'");
    writer.markup("     data-ad-client='ca-pub-2730368453635186'");
    writer.markup("     data-ad-slot='5067641553'></ins>");
    writer.markup("<script>(adsbygoogle = window.adsbygoogle || []).push({});</script>");
    
    writer.markup("</center>");
  }
  
  void writeParams(HtmlWriter writer, Operation operation) throws IOException {
    writer.text("(");
    boolean firstParam = true;
    for (Parameter p: operation.getParameters()) {
      if (firstParam) {
        firstParam = false;
      } else {
        writer.text(", ");
      }
      if ((p.getModifiers() & Parameter.OPTIONAL) != 0) {
        writer.text("optional ");
      }
      writeLinkedType(writer, p.getType());
      writer.text(" " + p.getName());
      if ((p.getModifiers() & Parameter.VARIADIC) != 0) {
        writer.text("...");
      }
    }
    writer.text(")");
  }

  public void writeOperations(HtmlWriter writer, Collection<Operation> operations) throws IOException {
    for (Operation operation: operations) {
      writer.markup("<tr><td>");
      if (operation.isStatic()) {
        writer.text("static ");
      }
      writeLinkedType(writer, operation.getType());
      writer.markup("</td><td>");
      
      String docUrl = operation.getDocumentationLink();
      String summary = operation.getDocumentationSummary();
      if (summary != null) {
        writer.markup("<dl><dt>");
      }
      if (docUrl != null) {
        writer.markup("<a href='").text(docUrl).markup("'>");
        writer.text(operation.getName());
        writer.markup("</a>");
      } else {
        writer.text(operation.getName());
      }
      writeParams(writer, operation);
      if (summary != null) {
        writer.markup("</dt><dd>");
        writer.text(summary);
        writer.markup("</dd></dl>");
      }
      writer.markup("</td>");
    }
  }
  
  public void writeProperties(HtmlWriter writer, Iterable<Property> properties) throws IOException {
    for (Property property: properties) {
      writer.markup("<tr><td>");
      if (property.isConstant()) {
        writer.text("const ");
      } else if (property.isStatic()) {
        writer.text("static ");
      }
      writeLinkedType(writer, property.getType());
      writer.markup("</td><td>");
      
      String summary = property.getDocumentationSummary();
      if (summary != null) {
        writer.markup("<dl><dt>");
      }
      String docUrl = property.getDocumentationLink();
      if (docUrl != null) {
        writer.markup("<a href='").text(docUrl).markup("'>");
        writer.text(property.getName());
        writer.markup("</a>");
      } else {
        writer.text(property.getName());
      }
      if (property.getInitialValue() != null) {
        writer.text(" = " + property.getInitialValue());
      }
      if (summary != null) {
        writer.markup("</dt><dd>");
        writer.text(summary);
        writer.markup("</dd></dl>");
      }
    }
  }

  
  
  public static void main(String[] args) throws IOException {
    Generator generator = new Generator();
    generator.readModel();
    generator.writeAll();
    
    HtmlWriter.copyFile("res/static/html5index.png", "gen/html5index.png");
    HtmlWriter.copyFile("res/static/html5index-miniicon.png", "gen/html5index-miniicon.png");
    HtmlWriter.copyFile("res/static/style.css", "gen/style.css");
  }
}
