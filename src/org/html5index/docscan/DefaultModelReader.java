package org.html5index.docscan;

import org.html5index.model.DocumentationProvider;
import org.html5index.model.Library;
import org.html5index.model.Model;

public class DefaultModelReader {
	public static Model readModel() {
		Model model = new Model();
		for (DocumentationProvider provider: Sources.SOURCES) {
			Library lib = new Library(provider.getTitle(), true);
			model.addLibrary(lib);
			provider.readDocumentation(lib);
		}
		return model;
	}
}
