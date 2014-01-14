package org.html5index.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

public class HtmlWriter {
  private Writer writer;
  
  public static String htmlEscape(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch(c) {
      case '<': 
        sb.append("&lt;");
        break;
      case '>': 
        sb.append("&gt;");
        break;
      case '\'': 
        sb.append("&apos;");
        break;
      case '\"': 
        sb.append("&quot;");
        break;
      case '&': 
        sb.append("&amp;");
        break;
      default:
        if (c > 127) {
          sb.append("&#" + ((int) c) + ";");
        } else {
          sb.append(c);
        }
      }
    }
    return sb.toString();
  }
  
  public static void copyFile(String from, String to) throws IOException {
    DataInputStream dis = new DataInputStream(new FileInputStream(from));
    byte[] buf = new byte[(int) new File(from).length()];
    dis.readFully(buf);
    dis.close();
    FileOutputStream fos = new FileOutputStream(to);
    fos.write(buf);
    fos.close();
  }
  
  
  public HtmlWriter(Writer writer) {
    this.writer = writer;
  }

  public HtmlWriter markup(String s) throws IOException {
    writer.write(s);
    return this;
  }
  
  public HtmlWriter text(String s) throws IOException {
    writer.write(htmlEscape(s));
    return this;
  }


  public void close() throws IOException {
    writer.close();
  }

  /** Extract the first sentences */
  public static String summary(String text) {
    String[] parts = text.trim().split("\\.\\s+");
    StringBuilder pending = new StringBuilder();
    ArrayList<String> sentences = new ArrayList<String>();
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      pending.append(part);
      if (i != parts.length - 1) {
        pending.append(".");
      }
      if (!part.endsWith("i.e.") && !part.endsWith("e.g.") && !part.endsWith(":")) {
        sentences.add(pending.toString());
        pending.setLength(0);
      }
    }
    if (pending.length() > 0 && pending.charAt(pending.length() - 1) != ':') {
      sentences.add(pending.toString());
    }
    
    StringBuilder sb = new StringBuilder();
    for (String sentence: sentences) {
      if (sb.length() > 0) {
        sb.append(' ');
      }
      sb.append(sentence);
      if (sb.length() > 256) {
        break;
      }
    }
    return sb.toString();
  }
}
