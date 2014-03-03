package org.html5index.htmlgenerator;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.html5index.docscan.Sources;
import org.html5index.idl.IdlParser;
import org.html5index.model.Artifact;
import org.html5index.model.DocumentationProvider;
import org.html5index.model.DocumentationProvider.Category;
import org.html5index.model.Library;
import org.html5index.model.Member;
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
    writer.markup("<link href='http://fonts.googleapis.com/css?family=Open+Sans:400,700' rel='stylesheet' type='text/css'>");
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
      model.addLibrary(lib);
      provider.readDocumentation(lib);
    }
  }
  
  public void writeAll() throws IOException {
    writeIndex();
    writeAbout();
    writeModel();
    writeGlobalIndex();
  }
  
  public void writeModel() throws IOException {
    HtmlWriter writer = createWriter("Libraries");

    writer.markup("<h3><a href='All Types.html' target='lib'>Libraries</a></h3>\n");
    
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
    writer.markup("<html><head><title>HTML 5 JavaScript API Index</title>\n");
    writer.markup("<link rel='icon' href='favicon.ico'>");
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
      
      if (type.getKind() == Type.Kind.PARTIAL) {
        writer.text(" (" + type.getLibrary().getName() + ")");
      }
    }
  }
  
  public String kindTitle(Type.Kind kind) {
    if (kind == Type.Kind.NO_OBJECT) {
      return "NoObject";
    }
    String s = kind.toString().replace("_", "-<br>");
    return s.charAt(0) + s.substring(1).toLowerCase();
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
        writer.markup("<li><a class='ext' href='").text(entry.getValue());
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
    for (String url[]: lib.getDocumentationProvider().getUrls()) {
      writer.markup("<li><a class='ext' href='").text(url[0]).markup("'>");
      writer.text(url[1]);
      writer.markup("</a></li>");
    } 
    writer.markup("</ul>");

    Map<Type.Kind, List<Type>> kindMap = new TreeMap<Type.Kind, List<Type>>();
    for (Type type: lib.getTypes()) {
      Type.Kind kind = type.getKind();
      if (listKind(kind) > 0) {
        List<Type> types = kindMap.get(kind);
        if (types == null) {
          types = new ArrayList<Type>();
          kindMap.put(kind, types);
        }
        types.add(type);
      }
    }
    
    if (kindMap.size() > 0) {
      writer.markup("<h3>Declared Types</h3>");
      writer.markup("<table class='types'><tr>");
      
      for (Type.Kind kind: kindMap.keySet()) {
        writer.markup("<th>" + kindTitle(kind) + "</th>");
      }
      writer.markup("</tr><tr>");
      for (List<Type> list: kindMap.values()) {
        writer.markup("<td><ul class='plain'>");
        for (Type t: list) {
          writer.markup("<li>");
          writeLinkedType(writer, t);
          writer.markup("</li>");
        }
        writer.markup("</ul></td>");
      }
      writer.markup("</tr></table>");
    }
    closeWriter(writer);
  }
  
  int listKind(Type.Kind kind) {
    if (kind == Type.Kind.GLOBAL || kind == Type.Kind.INTERFACE || kind == Type.Kind.PARTIAL ||
        kind == Type.Kind.PARTIAL  || kind == Type.Kind.NO_OBJECT || 
        kind == Type.Kind.CALLBACK_INTERFACE || kind == Type.Kind.ARRAY_OBJECT) {
      return 2;
    }
    if (kind == Type.Kind.DICTIONARY || kind == Type.Kind.ENUM) {
      return 1;
    }
    return 0;
  }
  
  public void writeAllTypesIndex() throws IOException {
    HtmlWriter writer = createWriter("All Types");
    writer.markup("<h3><a href='about.html' target='type'>All Types</a></h3>");
    TreeSet<Type> all = new TreeSet<Type>();
    for (Library lib: model.getLibraries()) {
      for (Type t: lib.getTypes()) {
        if (t.getKind() != Type.Kind.PARTIAL) {
          all.add(t);
        }
      }
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
   // writer.markup("<div style='position: absolute; left:0; top:0; width: 100%; background-color:#d9d9d9; padding:8px 16px'>");
    writer.markup("<div style='float:right'><a href='Global Index.html'><small>Global Index</small></a></div>");
    writer.markup("<small>");
    writer.markup("<a href='index.html' target='_top'>HTML5 JS API Index</a>");
    if (lib != null) {
      writer.markup("  &gt; <a href='").text(lib.getName()).markup(" - Overview.html'><b>");
      writer.text(lib.getName()).markup("</b> Tutorials &amp; Specs");
      writer.markup("</a>");
    }
    writer.markup("</small></div>");
  //  writer.markup("<div style='margin:16px'>&nbsp</div>");
  }
  
  
  public void writeType(Type type) throws IOException {
    HtmlWriter writer = createWriter(type.getLibrary().getName() + " - " + type.getName());
    
    writeHeader(writer, type.getLibrary());
    
    writer.markup("<h2>");

    String url = type.getDocumentationLink();
    if (url != null) {
      writer.markup("<a class='ext' href='").text(url).markup("'>");
      writer.text(type.getName());
      writer.markup("</a>");
    } else {
      writer.text(type.getName());
    }
    writer.markup("</h2>\n");
    
    boolean popen = false;
    if (type.getSuperType() != null && type.getKind() != Type.Kind.PARTIAL) {
      writer.markup("<p>");
      popen = true;
      if (type.getKind() == Type.Kind.ALIAS) {
        writer.text("Alias for ");
      } else {
        writer.text("Extends ");
      }
      writeLinkedType(writer, type.getSuperType());
      writer.text(". ");
    } 
    if (type.getTypes().size() != 0) {
      if (!popen) {
        writer.markup("<p>");
        popen = true;
      }
      writer.text("Implements ");
      boolean first = true;
      for (Type t: type.getTypes()) {
        if (first) {
          first = false;
        } else {
          writer.text(", ");
        }
        writeLinkedType(writer, t);
      }
      writer.text(".");
    }
    if (popen) {
      writer.markup("</p>");
    }
    
    if (type.getImplementedBy().size() != 0) {
      writer.markup("<p>").text(type.getKind() == Type.Kind.NO_OBJECT ? "Implemented by " : "Extended by ");
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

    switch(type.getKind()) {
    case PARTIAL:
      writer.markup("<p>This page describes " + type.getLibrary().getName() + 
          " extensions to the origial ");
      writeLinkedType(writer, type.getSuperType());
      writer.markup(" type.</p>");
      break;
    case NO_OBJECT: 
      writer.markup(
          "<p>This type groups properties and / or operations together for documentation " +
          "purposes and does not have an explicit JavaScript representation.</p>");
      break;
    case DICTIONARY:
      writer.markup(
          "<p>This type represents a collection of object properties and does not have " +
          "an explicit JavaScript representation.</p>");
      break;
    case CALLBACK_INTERFACE:
      writer.markup("<p>This type represents an interface implemented by the user for " +
          "callback functionality.</p>");
      break;
    case ENUM:
      writer.markup("<p>This type represents the following set of enum literals:</p>");
      writer.markup("<ul>");
      for (String s: type.getEnumLiterals()) {
        writer.markup("<li>");
        writer.text(s);
        writer.markup("</li>");
      }
      writer.markup("</ul>");
      break;
    }

    
    Collection<Property> properties = type.getOwnAndInterfaceProperties();
    if (properties.size() != 0) {
      ArrayList<Property> constants = new ArrayList<Property>();
      ArrayList<Property> staticProperties = new ArrayList<Property>();
      ArrayList<Property> regularProperties = new ArrayList<Property>();
      
      for (Property p: properties) {
        if (p.hasModifier(Artifact.CONSTANT)) {
          constants.add(p);
        } else if (p.hasModifier(Artifact.STATIC)){
          staticProperties.add(p);
        } else {
          regularProperties.add(p);
        }
      }
      writer.markup("<table class='members'><tr><th colspan='2'>Properties</th></tr>\n");
      writeProperties(writer, constants);
      writeProperties(writer, staticProperties);
      writeProperties(writer, regularProperties);
      writer.markup("</table>");
    }
    
    if (type.getConstructors().size() > 0) {
      writer.markup("<table class='members' id='");
      writer.text(type.getName());
      writer.markup("'><tr><th>Constructor</th></tr>");
      for (Operation constructor: type.getConstructors()) {
        if (constructor.getName().equals(type.getName())) {
          writer.markup("<tr><td>");
        } else {
          writer.markup("<tr id='");
          writer.text(constructor.getName());
          writer.markup("'><td>");
        }
        writer.text(constructor.getName());
        writeParams(writer, constructor);
        writer.markup("</td></tr>");
      }
      writer.markup("</table>");
    }

    Collection<Operation> operations = type.getOwnAndInterfaceOperations();
    Collection<Operation> staticOperations = new ArrayList<Operation>();
    Collection<Operation> regularOperations = new ArrayList<Operation>();
    
    for (Operation op: operations) {
      if (op.hasModifier(Artifact.STATIC)) {
        staticOperations.add(op);
      } else {
        regularOperations.add(op);
      }
    }
    
    if (operations.size() != 0 ) {
      writer.markup("<table class='members'><tr><th colspan='2'>Operations</th></tr>\n");
      writeOperations(writer, staticOperations);
      writeOperations(writer, regularOperations);
      writer.markup("</table>");
    }
    
    Collection<Member> references = type.getReferences();
    Map<Type,Set<Member>> refByType = new TreeMap<Type,Set<Member>>();
    for (Member m: references) {
      Type owner = m.getOwner();
      if (owner != type && owner != null) {
        Set<Member> refs = refByType.get(type);
        if (refs == null) {
          refs = new TreeSet<Member>();
          refByType.put(owner, refs);
        }
        refs.add(m);
      }
    }
    
    if (refByType.size() > 0) {
      writer.markup("<table class='members'><tr><th colspan='2'>Referenced by</th></tr>");
      writer.markup("<tr>");
      for (Map.Entry<Type, Set<Member>> e: refByType.entrySet()) {
        Type owner = e.getKey();
        writer.markup("<tr><td>");
        writeLinkedType(writer, owner);
        writer.markup("</td><td>");
        boolean first = true;
        for (Member m: e.getValue()) {
          if (first) {
            first = false;
          } else {
            writer.text(", ");
          }
          writeMemberLink(writer, m);
        }
        writer.markup("</td></tr>");
      }
      writer.markup("</table>");
    }
    closeWriter(writer);
  }

  
  void writeMemberLink(HtmlWriter writer, Member m) throws IOException {
    Type owner = m.getOwner();
    writer.markup("<a href='");
    writer.text(owner.getLibrary().getName() + " - " + owner.getName() + ".html#" + m.getName());
    writer.markup("'>");
    writer.text(m.getName());
    writer.markup("</a>");
    if (m instanceof Operation) {
      Operation op = (Operation) m;
      writer.text(op.getParameters().size() == 0  ? "()" : "(...)");
    }
  }
  
  void writeAbout() throws IOException {
    HtmlWriter writer = createWriter("about");
    writeAboutContent(writer, true);
    closeWriter(writer);
  }
  
  void writeAboutContent(HtmlWriter writer, boolean inFrame) throws IOException {
    writer.markup("<div style='float:right;padding:0 0 10px 10px;text-align:right'><small>");
    writer.markup("<a href='Global Index.html'>Global Index</a>");
   // if (inFrame) {
   //   writer.markup("&nbsp;&nbsp;&nbsp;<span style='position:relative; top:2px'>");
   //   writer.markup("<div style='display:inline-block' class='g-plusone' data-size='small' data-annotation='none'></span></div>");
  //  }
    writer.markup("</small>"); //<br><br>");
//    writer.markup("<img src='http://www.w3.org/html/logo/downloads/HTML5_Logo_128.png' title='HTML 5 Logo by W3C'>");
    writer.markup("</div>");
    writer.markup("<h2>The HTML 5 JavaScript API Index</h2>\n");
    
    writer.markup("<p>");
    writer.markup("<table class='rays'>");
    Category[] cats = Category.values();
    for (int i = 0; i < cats.length; i += 2) {
      writer.markup("<tr>");
      Category cat = cats[i];
      for (int col = 0; col < 5; col++) {
        if (col == 2) {
          cat = cats[i + 1];
          writer.markup("<td title='HTML 5 Logo by W3C'><div style='width:200px'></div>");
        } else if (col == 0 || col == 4) {
          writer.markup(col < 2 ? "<td style='text-align:right;width:50%'>" : "<td style='width:50%'>");
          boolean first = true;
          for (Library lib: model.getLibraries()) {
            if (lib.getDocumentationProvider().getCategory() == cat) {
              if  (first) {
                first = false;
              } else {
                writer.text(", ");
              }
              writer.markup("<a href='").text(lib.getName() + " - Overview.html").markup("'>");
              writer.text(lib.getName());
              writer.markup("</a>");
            }
          }
        } else if (col == 1 || col == 3) {
          writer.markup("<td>");
          writer.markup("<img src='img/").text(cat.toString()).markup(".png' title='").text(cat.toString()).markup("'>");
        }
        writer.markup("</td>");
      }         
      writer.markup("</tr>");
    }
    writer.markup("</table>");
    writer.markup("</p><p>");
    writer.text("Do you think ");
    writer.markup("<a class='ext' href='http://vanilla-js.com/' target='_top'>vanilla.js</a>");
    writer.text(" is the best JavaScript framework? ");
    writer.text("Have you always missed something similar to \"JavaDoc\" for JavaScript ").markup("&mdash; ");
    writer.text("something that is complete, easy to navigate, up to date and not vendor specific?");
    writer.markup("</p>\n<p>");
    writer.text("This HTML 5 JavaScript API index is automatically generated from the ");
    writer.text("HTML 5 specification documents by scanning them for IDL fragments. ");
    writer.text("The index generator parses the IDL code and link it up to matching headings, ");
    writer.text("creating a cross-reference that can be conveniently navigated using ");
    writer.text("the frames to the left* or following the links above.");
    writer.markup("</p>\n<p>");

    writer.text("Some links and summaries are still missing ");
    writer.text("(some specs unfortunately don't use ids that can be inferred), but ");
    writer.text("all the types and signatures should be there already ");
    writer.markup("(<a class='ext' href='https://github.com/stefanhaustein/html5index/issues' target='_top'>issue tracker</a>).");
    writer.markup("</p>\n<p>");
    writer.text("Note that this index is most useful for looking up method names and ");
    writer.text("signatures if you are already familiar with HTML 5 and JavaScript. ");
    writer.text("However, for getting started quickly, we also include links to ");
    writer.text("corresponding tutorials on the library overview pages.");
    writer.markup("</p>\n<p>");

    writer.markup("</p><hr><center>\n");
    
    if (inFrame) {
      writer.markup("<script async src='//pagead2.googlesyndication.com/pagead/js/adsbygoogle.js'></script>");
      writer.markup("<!-- about -->");
      writer.markup("<ins class='adsbygoogle'\n");
      writer.markup("     style='display:inline-block;width:728px;height:90px'\n");
      writer.markup("     data-ad-client='ca-pub-2730368453635186'\n");
      writer.markup("     data-ad-slot='5067641553'></ins>\n");
      writer.markup("<script>(adsbygoogle = window.adsbygoogle || []).push({});</script>\n");
      
      writer.markup("<script type='text/javascript'>\n");
      writer.markup("(function() {\n");
      writer.markup("  var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;\n");
      writer.markup("  po.src = 'https://apis.google.com/js/platform.js';\n");
      writer.markup("  var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);\n");
      writer.markup("})()\n");
      writer.markup("</script>\n");
    }
    writer.markup("</center><hr><p id='frames'><small style='color:gray'>*) ");
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
    writer.markup("</small></p>");
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
      writer.markup("<tr id='").text(operation.getName()).markup("'><td>");
      if (operation.isStatic()) {
        writer.text("static ");
      }
      Type type = operation.getType();
      if (type == null) {
        writer.text("void");
      } else if (type == operation.getOwner()) {
        writer.text(type.getName());
      } else {
        writeLinkedType(writer, operation.getType());
      }
        
      writer.markup("</td><td>");
      
      String docUrl = operation.getDocumentationLink();
      String summary = operation.getDocumentationSummary();
      if (summary != null) {
        writer.markup("<dl><dt>");
      }
      if (docUrl != null) {
        writer.markup("<a class='ext' href='").text(docUrl).markup("'>");
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
      writer.markup("<tr id='").text(property.getName()).markup("'><td>");
      if (property.hasModifier(Artifact.CONSTANT)) {
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
        writer.markup("<a class='ext' href='").text(docUrl).markup("'>");
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

  public void writeGlobalIndex() throws IOException {
    HtmlWriter writer = createWriter("Global Index");
    writer.markup("<small><a href='index.html' target='_top'>HTML5 JS API Index</a></small>");
    writer.markup("<h2>Global Index</h2><p><b>");
    
    for (char c = 'A'; c <= 'Z'; c++) {
      writer.markup("<a href='#" + c + "'>" + c + "</a> ");
    }
    writer.markup("</b></p>");
    TreeSet<Artifact> index = new TreeSet<Artifact>();
    
    for (Library lib: model.getLibraries()) {
      index.add(lib);
      for (Type t: lib.getTypes()) {
        if (t.getKind() == Type.Kind.PARTIAL) {
          continue;
        }
        index.add(t);
        for (Operation op: t.getOwnOperations()) {
          index.add(op);
        }
        for (Property p: t.getOwnProperties()) {
          index.add(p);
        }
      }
    }
    
    char current = ' ';
    boolean first = true;
    for (Artifact a: index) {
      String name = a.getName();
      char fc = Character.toUpperCase(name.charAt(0));
      if (fc != current) {
        if (first) {
          first = false;
        } else {
          writer.markup("</ul>");
        }
        writer.markup("<h2 id='").text(String.valueOf(fc)).markup("'>").text(String.valueOf(fc)).markup("</h2>");
        writer.markup("<ul class='plain'>");
        current = fc;
      }
      writer.markup("<li>");
      if (a instanceof Type) {
        writeLinkedType(writer, (Type) a);
        writer.text(" (" + a.getLibrary().getName() + ")");
      } else if (a instanceof Member) {
        Member member = (Member) a;
        writeMemberLink(writer, member);
        writer.text(" (" + member.getOwner().getName() + ")");
      } else if (a instanceof Library) {
        writer.markup("<a href='").text(a.getName() + " - Overview.html");
        writer.text(a.getName());
        writer.markup("</a>");
      }
      writer.markup("</li>");
    }
    writer.markup("</ul>");
    closeWriter(writer);
  }
  
  
  public static void main(String[] args) throws IOException {
    Generator generator = new Generator();
    generator.readModel();
    generator.writeAll();
    
    HtmlWriter.copyFile("res/static/favicon.ico", "gen/favicon.ico");
    HtmlWriter.copyFile("res/static/style.css", "gen/style.css");
  }
}
