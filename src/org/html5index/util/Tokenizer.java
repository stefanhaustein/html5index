package org.html5index.util;

public class Tokenizer {

  public static final int TT_EOF = -1;
  public static final int TT_NUMBER = -2;
  public static final int TT_WORD = -3;

  public static final int TT_EQ = -4;
  public static final int TT_NE = -5;
  public static final int TT_LE = -6;
  public static final int TT_GE = -7;
  public static final int TT_LOGICAL_AND = -8;
  public static final int TT_LOGICAL_OR = -9;
  public static final int TT_SCOPE = -10;
  public static final int TT_COMMENT = -11;
  public static final int TT_LINE_COMMENT = -12;
  public static final int TT_ELLIPSIS = 13;
  public static final int TT_WHITESPACE = 32;

  int last;
  int pos;
  int len;
  int row;
  int rowStart;
  String expr;

  public String sval;
  public double nval;
  public int ttype;

  private boolean reportWhitespace;
  private boolean reportComments;
  
  public Tokenizer(String expression) {
    this.expr = expression;
    this.len = expression.length();
  }
  
  public void setReportWhitespace(boolean reportWhitespace) {
    this.reportWhitespace = reportWhitespace;
  }

  private void advance() {
    // whitespace
    last = pos;
    while (pos < len && expr.charAt(pos) <= ' ') {
      if (expr.charAt(pos) == '\n') {
        row++;
        rowStart = pos + 1;
      }
      pos++;
    }
    if (last != pos && reportWhitespace) {
      ttype = ' ';
      sval = expr.substring(last, pos);
      return;
    }
    
    if (pos >= len) {
      ttype = TT_EOF;
      return;
    }

    char c = expr.charAt(pos++);
    char d = pos < len ? expr.charAt(pos) : 0;
    sval = "" + c;
    ttype = c;
    switch (c) {
    case '.':
      if (d == '.' && pos + 1 < len && expr.charAt(pos + 1) == '.') {
        pos += 2;
        ttype = TT_ELLIPSIS;
        sval = "...";
      }
      break;
    case ':':
      if (d == ':') {
        pos++;
        ttype = TT_SCOPE;
        sval = "::";
      }
      break;
    case '!':
      if (d == '=') {
        pos++;
        ttype = TT_NE;
        sval = "!=";
      }
      break;
    case '<':
      if (d == '=') {
        pos++;
        ttype = TT_LE;
        sval = "<=";
      }
      break;
    case '>':
      if (d == '=') {
        pos++;
        ttype = TT_GE;
        sval = ">=";
      }
      break;
    case '=':
      if (d == '=') {
        pos++;
        ttype = TT_EQ;
        sval = "==";
      }
      break;
    case '#':
      while (pos < len && expr.charAt(pos) != '\n') {
        pos++;
      }
      advance();
      return;
    case '/':
      if (d == '/') {
        pos++;
        int start = pos;
        while (pos < len && expr.charAt(pos) != '\n') {
          pos++;
        }
        pos++;
        row++;
        rowStart = pos;
        if (reportComments) {
          ttype = TT_LINE_COMMENT;
          sval = expr.substring(start, pos);
        } else {
          advance();
        }
        return;
      } else if (d == '*') {
        pos++;
        int start = pos;
        while (pos < len && (expr.charAt(pos-1) != '*' || 
            expr.charAt(pos) != '/')) {
          if (expr.charAt(pos) == '\n') {
            row++;
            rowStart = pos + 1;
          }
          pos++;
        }
        pos++;
        if (reportComments) {
          ttype = TT_COMMENT;
          sval = expr.substring(start, pos - 2);
        } else {
          advance();
        }
        return;
      }
      break;
    case '"':
    case '\'':
      parseQuoted();
      return;
    }
    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
      parseIdentifier();
    } else if (c >= '0' && c <= '9') {
      parseNumber();
    }
  }

  private void parseNumber() {
    StringBuilder sb = new StringBuilder();
    sb.append((char) ttype);

    if (ttype == '0' && pos < len && expr.charAt(pos) == 'x') {
      pos++;
      sb.append('x');
      while (pos < len) {
        char c = expr.charAt(pos);
        if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
          break;
        }
        sb.append(c);
        pos++;
      }
      nval = Long.parseLong(sb.substring(2), 16);
    } else {
      boolean seenE = false;
      while (pos < len) {
        char c = expr.charAt(pos);
        if (!seenE && (c == 'e' || c == 'E')) {
          seenE = true;
          if (pos + 1 < len && expr.charAt(pos + 1) == '-') {
            sb.append(c);
            c = expr.charAt(++pos);
          }
        } else if (c != '.' && (c < '0' || c > '9')) {
          break;
        }
        sb.append(c);
        pos++;
      }
      nval = Double.parseDouble(sval);
    }
    sval = sb.toString();
    ttype = TT_NUMBER;
    
  }

  private void parseIdentifier() {
    StringBuilder sb = new StringBuilder();
    sb.append((char) ttype);
    while (pos < len) {
      char c = expr.charAt(pos);
      if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_') {
        break;
      }
      sb.append(c);
      pos++;
    }
    sval = sb.toString();
    ttype = TT_WORD;
  }

  public void parseQuoted() {
    StringBuilder sb = new StringBuilder();
    while (pos < len && expr.charAt(pos) != ttype) {
      sb.append(expr.charAt(pos));
      pos++;
    }
    if (pos == len) {
      ttype = TT_EOF;
    } else {
      pos++;
    }
    sval = sb.toString();
  }

  public String getPositionDescription() {
    return (row + 1) + ":" + (pos - rowStart) + "; token: '" + sval + "' type: " + ttypeToString(ttype) + " Context: " + expr.substring(rowStart, pos); 
  }

  public String ttypeToString(int ttype) {
    switch(ttype) {
    case TT_EOF: return "TT_EOF";
    case TT_NUMBER: return "TT_NUMBER";
    case TT_EQ: return "TT_EQ";
    case TT_GE: return "TT_GE";
    case TT_LE: return "TT_LE";
    case TT_LOGICAL_AND: return "TT_LOGICAL_AND";
    case TT_LOGICAL_OR: return "TT_LOGICAL_OR";
    case TT_WORD: return "TT_WORD";
    case TT_SCOPE: return "TT_SCOPE";
    default: return ""  + ttype + (ttype > ' ' ? " ('" + (char) ttype + "')" : "");
    }
  }
  
  /**
   * Returns the raw, unparsed token value.
   */
  public String getRaw() {
    return expr.substring(last, pos);
  }
  
  public int nextToken() {
    sval = null;
    nval = 0;
    advance();
    return ttype;
  }

  public void fail(String s) {
    throw new RuntimeException(s + " @ " + getPositionDescription());   
  }

  public void setReportComments(boolean b) {
    this.reportComments = b;
  }

  public int getPos() {
    return pos;
  }
}