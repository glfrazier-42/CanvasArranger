package com.github.glfrazier.canvasarranger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

public class CanvasArranger implements Runnable {

	private String canvasFileName;
	private File canvasFile;

	private Map<String, Node> nodes;
	private List<String> nodeOrdering;
	private String rootNodeID;

	private Map<String, Edge> edges;
	private Map<String, Annotation> annotations;
	private long modTime;
	private boolean showAnnotations;
	private boolean pendingExit;
	private Node annotationsNode;

	public CanvasArranger(String filename) throws IOException {
		canvasFileName = filename;
		File f = new File(filename);
		canvasFile = new File(canvasFileName);
		showAnnotations = false;
		nodes = new HashMap<String, Node>();
		nodeOrdering = new LinkedList<String>();
		edges = new HashMap<String, Edge>();
	}

	private synchronized boolean loadCanvas() {
		boolean modified = false;
		Throwable badparse = null;
		for (int i = 0; i < 20; i++) {
			try {
				nodes.clear();
				nodeOrdering.clear();
				edges.clear();
				if (canvasFile.length() == 0) {
					return false;
				}
				Path cPath = canvasFile.toPath();
				byte[] buffer = Files.readAllBytes(cPath);
				ByteArrayInputStream in = new ByteArrayInputStream(buffer);
				JsonReader reader = Json.createReader(in);
				JsonObject content = reader.readObject();
				reader.close();
				in.close();

				JsonArray nodeList = content.getJsonArray("nodes");
				parseNodeList(nodeList);
				JsonArray edgeList = content.getJsonArray("edges");
				parseEdgeList(edgeList);
				JsonObject annotations = content.getJsonObject("annotations");
				parseAnnotations(annotations);
				if (rootNodeID != null && !nodes.containsKey(rootNodeID)) {
					rootNodeID = null;
					modified = true;
				}
				if (rootNodeID == null) {
					if (!nodeOrdering.isEmpty()) {
						rootNodeID = nodeOrdering.get(0);
						modified = true;
					}
				}
				return modified;
			} catch (Throwable t) {
				badparse = t;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				break;
			}
		}
		System.err.println("Failed to load the canvas 20 times:");
		badparse.printStackTrace();
		System.exit(-1);
		// Unreachable, but it makes Eclipse happy.
		return false;
	}

	/**
	 * Create the Json object that represents the arranged canvas, write the Json to
	 * a temp file. If the last-modified time for the canvas file is unchanged
	 * (i.e., there are no new modifications), copy the temp file over the canvas
	 * file and then update the <code>modTime</code>. If the canvas file has
	 * changed, we simply discard the write.
	 * 
	 * @param t the modification time of the canvas that is being arranged
	 * @throws IOException
	 */
	private synchronized void saveCanvas(long t) throws IOException {
		JsonObjectBuilder canvasBuilder = Json.createObjectBuilder();
		JsonArray nodes = buildNodeArray();
		canvasBuilder.add("nodes", nodes);
		JsonArray edges = buildEdgeArray();
		canvasBuilder.add("edges", edges);
		long oldModTime = modTime;
		modTime = t;
		JsonObject annotations = buildAnnotationsObject();
		modTime = oldModTime;
		canvasBuilder.add("annotations", annotations);

		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);
		JsonWriterFactory writerFactory = Json.createWriterFactory(config);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		JsonWriter writer = writerFactory.createWriter(bout);
		writer.write(canvasBuilder.build());
		writer.close();
		byte[] buffer = bout.toByteArray();
//		File tmpfile = File.createTempFile("canvas", ".json");
//		FileOutputStream out = new FileOutputStream(tmpfile);
//		out.write(buffer);
//		out.close();
//		Path srcPath = tmpfile.toPath();
		Path dstPath = canvasFile.toPath();
		Files.write(dstPath, buffer);
//		tmpfile.delete();
	}

	private JsonObject buildAnnotationsObject() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("modified", modTime);
		if (rootNodeID != null) {
			builder.add("root", rootNodeID);
			System.out.println("building annotations obj, root= " + rootNodeID);
		}
		return builder.build();
	}

	private JsonArray buildEdgeArray() {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Edge e : edges.values()) {
			builder.add(e.toJson());
		}
		return builder.build();
	}

	private JsonArray buildNodeArray() {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for (Node n : nodes.values()) {
			builder.add(n.toJson());
		}
		return builder.build();
	}

	private void parseAnnotations(JsonObject annotations) {
		try {
			modTime = annotations.getInt("modified");
		} catch (NullPointerException e) {
			System.err.println("Annotations do not include modification time.");
			modTime = 0;
		}
		try {
			rootNodeID = annotations.getString("root");
		} catch (NullPointerException e) {
			rootNodeID = null;
		}
		System.out.println("Parsed annotations, root = " + rootNodeID);
	}

	private void parseEdgeList(JsonArray edgeList) {
		for (JsonValue entry : edgeList) {
			JsonObject jEdge = entry.asJsonObject();
			try {
				Edge e = new Edge(jEdge);
				edges.put(e.toString(), e);
			} catch (IllegalArgumentException e) {
				System.err.println("Canvas contains an illegal edge entry: " + e);
			}
		}
		this.edges = edges;
	}

	private void parseNodeList(JsonArray nodeList) {
		for (JsonValue entry : nodeList) {
			JsonObject jNode = entry.asJsonObject();
			try {
				Node n = new Node(jNode);
				nodes.put(n.getID(), n);
				nodeOrdering.add(n.getID());
			} catch (IllegalArgumentException e) {
				System.err.println("Canvas contains an illegal node entry: " + e);
			}
		}
		this.nodes = nodes;
	}

	@Override
	public void run() {
		File f = new File(canvasFileName);
		modTime = 0;
		while (!pendingExit) {
			long t = f.lastModified();
			if (t != modTime) {
				System.out.println("\nCustom loop: file changed.");
				boolean modified = loadCanvas();
				System.out.println("loadCanvas returned " + modified);
				modified |= processCommands();
				Arranger arranger = null;
				if (rootNodeID != null) {
					Node root = nodes.get(rootNodeID);
					if (root == null) {
						System.err.println("rootNodeID=" + rootNodeID + " but root=" + root);
						System.err.println("nodes:");
						for (String id : nodes.keySet()) {
							System.err.println("  " + id + ": " + nodes.get(id));
						}
						System.err.println("Node ordering: ");
						for (String id : nodeOrdering) {
							System.err.println("  " + id);
						}
						System.exit(-1);
					}
					arranger = new Arranger(nodes.get(rootNodeID), nodes, edges);
				}
				if (arranger != null) {
					modified |= arranger.arrange();
				}
				if (modified && f.lastModified() == t) {
					try {
						saveCanvas(t);
					} catch (IOException e) {
						System.err.println("Failed to update the canvas: " + e);
					}
				} else {
					modTime = t;
				}

			}
		}
		System.out.println("Exiting per your request.");

	}

	private static final Pattern COMMAND_PATTERN = Pattern.compile("^\\<ca(.*)\\>\\s*$");

	private boolean processCommands() {
		boolean modified = false;
		boolean repaintAnnotations = false;
		CommandProperties cmds = new CommandProperties();
		cmds.setNoPropertyBehavior(CommandProperties.NoPropertyBehavior.RETURN_NULL);
		Node responseNode = null;
		for (Iterator<String> iter = nodes.keySet().iterator(); iter.hasNext();) {
			String id = iter.next();
			Node n = nodes.get(id);
			if (n.isCard()) {
				Matcher matcher = COMMAND_PATTERN.matcher(n.getText());
				if (matcher.matches()) {
					System.out.println("Pattern matched for " + n);
					modified = true;
					repaintAnnotations = true;
					boolean remove = true;
					String s = matcher.group(1);
					System.out.println("Commands = <" + s + ">");
					Reader reader = new StringReader(s);
					try {
						cmds.load(reader);
						System.out.println("Number of cmds: " + cmds.size());
					} catch (IOException e) {
						cmds.clear();
						n.setText("Failed to parse commands: " + s);
						remove = false;
					}

					System.out.println("show_annotations is either here or not here...");
					if (cmds.containsKey("show_annotations")) {
						System.out.println("show_annotations is here!");
						showAnnotations = cmds.getBooleanProperty("show_annotations");
						if (showAnnotations) {
							System.out.println("And it is true");
							annotationsNode = n;
							remove = false;
						}
					} else {
						System.out.println("show_annotations is not present.");
					}
					if (cmds.getBooleanProperty("exit", false)) {
						pendingExit = true;
					}

					if (remove) {
						iter.remove();
						nodeOrdering.remove(id);
					}
				} else {
					System.out.println("pattern did not match.");
				}
			}
		}
		if (repaintAnnotations) {
			repaintAnnotationsNode();
			modified = true; // It should already be true...
		}
		return modified;
	}

	private void repaintAnnotationsNode() {
		if (annotationsNode == null) {
			System.err.println("There is no annotations node to repaint!");
			return;
		}
		JsonObject annos = buildAnnotationsObject();
		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);
		JsonWriterFactory writerFactory = Json.createWriterFactory(config);
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		JsonWriter writer = writerFactory.createWriter(ostream);
		writer.write(annos);
		writer.close();
		String msg = new String(ostream.toByteArray());
		annotationsNode.setText(msg);
	}

	public static void main(String[] args) throws Exception {
		String fname = args[0];
		CanvasArranger arranger = new CanvasArranger(fname);
		Thread t = new Thread(arranger);
		t.setDaemon(true);
		t.start();
		t.join();
		System.out.println("Exiting normally.");
	}
}
