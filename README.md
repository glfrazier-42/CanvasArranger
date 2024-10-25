# CanvasArranger
Auto-arrange the cards, notebooks and media on an Obsidian canvas.

The arranging runs as a separate process. It looks in the Obsidian
vault for *.canvas files. When a canvas is modified, if that canvas
is being auto-arranged, the CanvasArranger will adjust the locations
of the items on the canvas in accord with the user's specifications.

Multiple layout options will be supported. In this early development
phase, only down/centered hierarchies are supported.

## Building

To build, one must obtain a Json implementation. To date, only the
glassfish javax.json-1.1.4.jar
```
  https://repo1.maven.org/maven2/org/glassfish/javax.json/1.1.4/javax.json-1.1.4.jar
```
has been tested.
