package org.html5index.generator;

import org.html5index.docscan.DefaultModelReader;
import org.html5index.model.Model;

public class AllGenerator {

	public static void main(String[] args) {
		Model model = DefaultModelReader.readModel();

		new HtmlGenerator(model).run();
		new JsonGenerator(model).run();
		new JsdocGenerator(model).run();
	}

}
