package org.html5index.util;

import org.html5index.docscan.Sources;
import org.html5index.idl.IdlParser;
import org.html5index.model.Artifact;
import org.html5index.model.DocumentationProvider;
import org.html5index.model.Library;
import org.html5index.model.Model;

import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Load up all Blink-IDLs from HEAD and look for types we don't have.
 */
// TODO: load these directly from the Blink git repo
public class IdlCompletenessChecker {

  public static final String ONLY_ONE = "OnlyOne";

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("The first argument must be a JAR file containing IDL files.");
      System.exit(-1);
    }
    new IdlCompletenessChecker().exec(args[0]);
  }

  private void exec(String jarFile) throws IOException {
    Model normalModel = readModel();
    Model blinkModel = readBlinkModel(jarFile);
    checkNormalAgainstBlink(normalModel, blinkModel);
  }

  private void checkNormalAgainstBlink(Model normalModel, Model blinkModel) {
    Set<String> allTypes = normalModel.getAllTypeNames();
    Set<String> blinkTypes = blinkModel.getAllTypeNames();
    Set<String> difference = new HashSet<String>(blinkTypes);
    difference.removeAll(allTypes);
    for (String type : difference) {
      System.out.format("Type %s is in Blink but not in any of the source specs.\n", type);
    }
  }

  Model readModel() throws IOException {
    Model model = new Model();
    for (DocumentationProvider provider : Sources.SOURCES) {
      Library lib = new Library(provider.getTitle(), true);
      model.addLibrary(lib);
      provider.readDocumentation(lib);
    }
    return model;
  }

  Model readBlinkModel(String jarFile) throws IOException {
    Model model = new Model();
    Library lib = new Library(ONLY_ONE, false);
    lib.setDocumentationProvider(new DocumentationProvider() {
      @Override
      public String getTitle() {
        return ONLY_ONE;
      }

      @Override
      public Category getCategory() {
        return Category.MULTIMEDIA;
      }

      @Override
      public String getSummary(Artifact artifact) {
        return ONLY_ONE;
      }

      @Override
      public String getLink(Artifact artifact) {
        return ONLY_ONE;
      }

      @Override
      public Iterable<String[]> getUrls() {
        return Collections.EMPTY_LIST;
      }

      @Override
      public Map<String, String> getTutorials() {
        return Collections.EMPTY_MAP;
      }

      @Override
      public void addDocumentation(Artifact artifact) {

      }

      @Override
      public void readDocumentation(Library lib) {

      }
    });
    model.addLibrary(lib);
    ZipFile jar = new ZipFile(jarFile);
    Enumeration<? extends ZipEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      ZipEntry jarEntry = entries.nextElement();
      if (!jarEntry.getName().endsWith(".idl")) {
        continue;
      }
      String wholeFile = new Scanner(jar.getInputStream(jarEntry)).useDelimiter("\\A").next().replaceAll("[ ]+", " ");
      try {
        new IdlParser(lib, wholeFile).parse();
      } catch (Exception e) {
        System.err.println("Can't parse " + jarEntry.getName() + " because " + e + ":" + e.getMessage());
      }
    }
    return model;
  }
}
