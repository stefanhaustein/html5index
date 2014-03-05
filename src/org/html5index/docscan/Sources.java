package org.html5index.docscan;

import org.html5index.model.DocumentationProvider;
import org.html5index.model.DocumentationProvider.Category;

public class Sources {
  public static final DocumentationProvider[] SOURCES = {
    new ExplicitIdlSpecScan("ECMAScript", null,
        "http://www.ecma-international.org/ecma-262/5.1/", "/idl/ecmascript.idl")
     .addTutorial("Webplatform: Programming Basics", "http://docs.webplatform.org/wiki/concepts/programming/programming_basics"),
    new Html5SpecScan("DOM", Category.SEMANTICS, "http://dom.spec.whatwg.org/",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/dom.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/elements.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/semantics.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/sections.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/grouping-content.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/text-level-semantics.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/edits.html",
//        "http://www.whatwg.org/specs/web-apps/current-work/multipage/embedded-content-1.html",
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
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/commands.html")
      .addTutorial("A dive into plain Javascript", "http://blog.adtile.me/2014/01/16/a-dive-into-plain-javascript/"),
    new Html5SpecScan("Browser", Category.PERFORMANCE,
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/browsers.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/history.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/webappapis.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/timers.html")
      .addTutorial("Treehouse: Getting started with the History API", "http://blog.teamtreehouse.com/getting-started-with-the-history-api")
      .addTutorial("Dive into HTML5: Manipulating history for fun & profit", "http://diveintohtml5.info/history.html"),
    new Html5SpecScan("Drag and Drop", Category.DEVICE_ACCESS,
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/dnd.html")
      .addTutorial("MDN Drag and Drop Tutorial", "https://developer.mozilla.org/en-US/docs/DragDrop/Drag_and_Drop")
      .addTutorial("HTML5 Rocks Drag and Drop Tutorial", "http://www.html5rocks.com/en/tutorials/dnd/basics/"),
    new Html5SpecScan("Web Sockets and Messaging", Category.CONNECTIVITY,
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/comms.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/network.html",
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/web-messaging.html")
      .addTutorial("Treehouse Websocket Introduction", "http://blog.teamtreehouse.com/an-introduction-to-websockets")
      .addTutorial("HTML5 Rocks Websocket Tutorial", "http://www.html5rocks.com/en/tutorials/websockets/basics/"),
    new Html5SpecScan("Animation Timing", Category.MULTIMEDIA, "http://www.w3.org/TR/animation-timing/")
      .addTutorial("MDN: Window.requestAnimationFrame()", "https://developer.mozilla.org/en-US/docs/Web/API/window.requestAnimationFrame"),
    new Html5SpecScan("Pointer Lock", Category.MULTIMEDIA, "http://www.w3.org/TR/pointerlock/")
       .addTutorial("HTML5 Rocks: Pointer Lock and First Person Shooter Controls", "http://www.html5rocks.com/en/tutorials/pointerlock/intro/"),
    new Html5SpecScan("Media", Category.MULTIMEDIA,
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/the-video-element.html")
      .addTutorial("HTML5 Rocks Video Tutorial", "http://www.html5rocks.com/en/tutorials/video/basics/")
      .addTutorial("MDN: Using HTML5 Video and Audio", "https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Using_HTML5_audio_and_video"),
    new Html5SpecScan("Offline", Category.OFFLINE_STORAGE,
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/offline.html")
      .addTutorial("HTML5 Rocks: A Beginner's Guide to Using the Application Cache", "http://www.html5rocks.com/en/tutorials/appcache/beginner/"),
    new Html5SpecScan("Canvas", Category.GRAPHICS,
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/the-canvas-element.html")
      .addTypeIdMap("CanvasRenderingContext2D", "2dcontext")
      .addTutorial("MDN Canvas Tutorial", "https://developer.mozilla.org/en-US/docs/Web/Guide/HTML/Canvas_tutorial"),
    new Html5SpecScan("Web Workers", Category.PERFORMANCE,
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/workers.html")
      .addTutorial("MDN: Using web workers", "https://developer.mozilla.org/en-US/docs/Web/Guide/Performance/Using_web_workers")
      .addTutorial("HTML 5: The Basics of Web Workers", "http://www.html5rocks.com/en/tutorials/workers/basics/"),
    new Html5SpecScan("Web Storage", Category.OFFLINE_STORAGE,
        "http://www.whatwg.org/specs/web-apps/current-work/multipage/webstorage.html"),
    new Html5SpecScan("File API", Category.OFFLINE_STORAGE, "http://www.w3.org/TR/FileAPI/",
        "http://www.w3.org/TR/file-writer-api/")
      .addTutorial("HTML5 Rocks: File Tutorial", "http://www.html5rocks.com/en/tutorials/file/dndfiles/")
      .addTutorial("Treehouse: FileReader Tutorial", "http://blog.teamtreehouse.com/reading-files-using-the-html5-filereader-api"),
    new Html5SpecScan("File System API", Category.OFFLINE_STORAGE, "http://www.w3.org/TR/file-system-api/")
      .addTutorial("HTML5 Rocks: Exploring the FileSystem APIs", "http://www.html5rocks.com/en/tutorials/file/filesystem/"),
    new Html5SpecScan("Fullscreen", Category.DEVICE_ACCESS, "http://www.w3.org/TR/fullscreen/")
      .addTutorial("MDN: Fullscreen Tutorial", "https://developer.mozilla.org/en-US/docs/Web/Guide/API/DOM/Using_full_screen_mode")
      .addTutorial("David Walsch's Fullscreen Tutorial", "http://davidwalsh.name/fullscreen"),
    new Html5SpecScan("Selectors", Category.STYLING, "http://www.w3.org/TR/selectors-api/")
      .addTutorial("MDN: Locating DOM elements using selectors", "https://developer.mozilla.org/en-US/docs/Web/Guide/API/DOM/Locating_DOM_elements_using_selectors"),
    new Html5SpecScan("Shadow DOM", Category.PERFORMANCE, "http://www.w3.org/TR/shadow-dom/")
      .addTutorial("HTML5 Rocks: Shadow DOM 101", "http://www.html5rocks.com/en/tutorials/webcomponents/shadowdom/"),
    new Html5SpecScan("CSS Object Model", Category.STYLING, "http://www.w3.org/TR/cssom/")
      .addTutorial("Divya Manian: CSS Object Model", "http://nimbupani.com/css-object-model.html"),
    new Html5SpecScan("Indexed DB", Category.OFFLINE_STORAGE, "http://www.w3.org/TR/IndexedDB/")
      .addTutorial("MDN: Indexed DB Basic Concepts", "https://developer.mozilla.org/en-US/docs/IndexedDB/Basic_Concepts_Behind_IndexedDB")
      .addTutorial("MDN: Using  IndexedDB", "https://developer.mozilla.org/en-US/docs/IndexedDB/Using_IndexedDB")
      .addTutorial("HTML5 Rocks: A simple TODO list using HTML5 IndexedDB", "http://www.html5rocks.com/en/tutorials/indexeddb/todo/"),
    new Html5SpecScan("WebRTC", Category.CONNECTIVITY, "http://dev.w3.org/2011/webrtc/editor/webrtc.html")
      .addTutorial("HTML5 Rocks: Getting Started with WebRTC", "http://www.html5rocks.com/en/tutorials/webrtc/basics/")
      .addTutorial("HTML5 Rocks: WebRTC in the real world: STUN, TURN and signaling", "http://www.html5rocks.com/en/tutorials/webrtc/infrastructure/")
      .addTutorial("MDN: Taking webcam photos", "https://developer.mozilla.org/en-US/docs/WebRTC/Taking_webcam_photos")
      .addTutorial("Peer-to-peer communications with WebRTC", "https://developer.mozilla.org/en-US/docs/WebRTC/Peer-to-peer_communications_with_WebRTC"),
    new Html5SpecScan("Web Audio", Category.MULTIMEDIA, "http://www.w3.org/TR/webaudio/")
      .addTutorial("HTML5 Rocks: Getting Started with Web Audio", "http://www.html5rocks.com/en/tutorials/webaudio/intro/")
      .addTutorial("MDN: Web Audio API", "https://developer.mozilla.org/en-US/docs/Web_Audio_API"),
    new Html5SpecScan("SVG", Category.GRAPHICS, "http://www.w3.org/TR/SVG/single-page.html"),
    new ExplicitIdlSpecScan("Typed Arrays", Category.PERFORMANCE,
        "http://www.khronos.org/registry/typedarray/specs/latest/", 
        "https://www.khronos.org/registry/typedarray/specs/latest/typedarray.idl")
    .addTutorial("MDN Typed Arrays Tutorial", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Typed_arrays")
    .addTutorial("HTML5 Rocks: Binary Data in the Browser", "http://www.html5rocks.com/en/tutorials/webgl/typed_arrays/"),
   new ExplicitIdlSpecScan("WebGL", Category.GRAPHICS,
       "http://www.khronos.org/registry/webgl/specs/latest/1.0/", 
       "https://www.khronos.org/registry/webgl/specs/latest/1.0/webgl.idl")
    .addTutorial("HTML5 Rocks: WebGL Fundamentals", "http://www.html5rocks.com/en/tutorials/webgl/webgl_fundamentals/")
    .addTutorial("MDN: Getting Started with WebGL", "https://developer.mozilla.org/en-US/docs/Web/WebGL/Getting_started_with_WebGL")
    .addTutorial("Learning WebGL: The Lessons", "http://learningwebgl.com/blog/?page_id=1217"),
  };
}
