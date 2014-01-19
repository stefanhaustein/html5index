package org.html5index.model;

import java.util.ArrayList;
import java.util.List;

public class Operation extends Member {
  private ArrayList<Parameter> parameters = new ArrayList<Parameter>();
  private String body;

  public Operation(String name, Type type) {
    super(name, type);
  }

  public void addParameter(Parameter param) {
    parameters.add(param);
    param.owner = this;
    param.getType().addReference(this);
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getBody() {
    return body;
  }
  
  public String toString() {
    if (name.equals("(main)")) {
      return "(main)";
    }
    StringBuilder sb = new StringBuilder(name);
    sb.append('(');
    if (parameters.size() == 1) {
      Type t = parameters.get(0).getType();
      if (t != null) {
        sb.append(parameters.get(0).getType().getName());
      } else {
        sb.append("?");
        
      }
    } else if (parameters.size() > 1) {
      sb.append("\u2026");
    }
    sb.append(')');
    return sb.toString();
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  public String getTitle() {
    StringBuilder sb = new StringBuilder();
    if (this != owner.getConstructor()) {
      if (owner.metaTypeOf != null) {
        sb.append("static ");
      }
      if (type == null) {
        sb.append("void ");
      } else {
        sb.append(type.getLink());
        sb.append(' ');
      }
    }
    sb.append("<b>");
    sb.append(name);
    sb.append("</b>(");
    boolean first = true;
    for (Parameter param : getParameters()) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(param.getTitle());
    }
    sb.append(")");
    return sb.toString();
  }


  private Parameter getParameterOrNull(int index) {
    return index < parameters.size() ? parameters.get(index) : null;
  }    
  
  private static Parameter mergeParameter(Model model, Parameter p1, Parameter p2) {
    if (p1 == null || p2 == null) {
      if (p1 == null) {
        p1 = p2;
      }
      Parameter np = new Parameter(p1.getName(), p1.getType(), p1.getModifiers() | Parameter.OPTIONAL);
      return np;
    }

    String name = p1.name;
    if (name.indexOf(p2.name) == -1 && p2.name.indexOf(name) == -1) {
      name += "_" + p2.name;
    }
    Type t = p1.getType();
    if (t != p2.getType()) {
      t = model.getType("any");
    }
    return new Parameter(name, t, p1.modifiers | p2.modifiers);
  }
    
  public void merge(Model model, Operation merge) {
    ArrayList<Parameter> merged = new ArrayList<Parameter>();
    int len = Math.max(parameters.size(), merge.parameters.size());
    for (int i = 0; i < len; i++) {
      merged.add(mergeParameter(model, getParameterOrNull(i), merge.getParameterOrNull(i)));
    }
    parameters.clear();
    // Make sure owner is set correctly...
    for(Parameter p: merged) {
      addParameter(p);
    }
  }

  public void clearParmeters() {
    parameters.clear();
  }


  public void setType(Type type) {
    this.type = type;
    
  }

}
