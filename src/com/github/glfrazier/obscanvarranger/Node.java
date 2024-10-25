package com.github.glfrazier.obscanvarranger;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class Node {

	public static enum NodeType {
		text, file
	};

	// Fields that are derived from the Obsidian Json
	protected String id;
	protected String color;
	protected int x;
	protected int y;
	protected int width;
	protected int height;
	protected NodeType type;
	protected String content;

	public Node(JsonObject jNode) {
		id = jNode.getString("id");
		try {
			color = jNode.getString("color");
		} catch (NullPointerException e) {
			color = null;
		}
		x = jNode.getInt("x");
		y = jNode.getInt("y");
		width = jNode.getInt("width");
		height = jNode.getInt("height");
		type = NodeType.valueOf(jNode.getString("type"));
		switch (type) {
		case text:
			content = jNode.getString("text");
			break;
		case file:
			content = jNode.getString("file");
			break;
		}
	}

	@Override
	public String toString() {
		return id + "(" + x + "," + y + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Node)) {
			return false;
		}
		Node n = (Node) o;
		return id.equals(n.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public JsonValue toJson() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("id", id);
		builder.add("x", x);
		builder.add("y", y);
		builder.add("width", width);
		builder.add("height", height);
		if (color != null) {
			builder.add("color", color);
		}
		builder.add("type", type.toString());
		switch (type) {
		case text:
			builder.add("text", content);
			break;
		case file:
			builder.add("file", content);
			break;
		}
		return builder.build();
	}

	public boolean isCard() {
		return type == NodeType.text;
	}

	public String getText() {
		return content;
	}

	public void setText(String text) {
		content = text;
	}

	public String getID() {
		return id;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setX(int x) {
		this.x = x;
	}
	public void setY(int y) {
		this.y = y;
	}

}
