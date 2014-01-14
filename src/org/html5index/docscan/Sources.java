package org.html5index.docscan;

import org.html5index.model.DocumentationProvider;

public class Sources {
  public static final DocumentationProvider[] SOURCES = {
    new EcmascriptDomScan(),
    new Html5DocScan("DOM", "http://dom.spec.whatwg.org/",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/dom.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/elements.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/semantics.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/sections.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/grouping-content.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/text-level-semantics.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/edits.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/embedded-content-1.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/the-iframe-element.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/the-map-element.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/links.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/tabular-data.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/scripting-1.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/forms.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/the-input-element.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/the-button-element.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/association-of-controls-and-forms.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/interactive-elements.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/commands.html"),
    new Html5DocScan("Browser",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/browsers.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/webappapis.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/timers.html"),
    new Html5DocScan("Drag and Drop",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/dnd.html"),
    new Html5DocScan("Sockets and Messaging",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/comms.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/network.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/web-messaging.html"),
    new Html5DocScan("Audio and Video", 
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html"),
    new Html5DocScan("Offline", 
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/offline.html"),
    new Html5DocScan("Canvas", 
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html")
      .addTutorial("MDN Canvas Tutorial", "https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Canvas_tutorial"),
    new Html5DocScan("Web Workers", 
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/workers.html")
      .addTutorial("MDN Web Worker Tutorial", "https://developer.mozilla.org/en-US/docs/Web/Guide/Performance/Using_web_workers")
      .addTutorial("HTML 5 Rocks Web Worker Tutorial", "http://www.html5rocks.com/en/tutorials/workers/basics/"),
    new Html5DocScan("Web Storage",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/webstorage.html")
  };
}
