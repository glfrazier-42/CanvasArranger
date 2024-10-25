package com.github.glfrazier.canvasarranger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class Edge {

	public static enum Side {
		top, bottom, left, right
	};

	public static enum ArrowType {
		none, arrow
	}

	private String id;
	private String color;
	private String fromNode;
	private String toNode;
	private Side fromSide;
	private Side toSide;
	private ArrowType fromEnd;
	private ArrowType toEnd;
	private String label;

	public Edge(JsonObject jEdge) {
		id = jEdge.getString("id");
		try {
			color = jEdge.getString("color");
		} catch (NullPointerException e) {
			color = null;
		}
		fromNode = jEdge.getString("fromNode");
		toNode = jEdge.getString("toNode");
		fromSide = Side.valueOf(jEdge.getString("fromSide"));
		toSide = Side.valueOf(jEdge.getString("toSide"));
		try {
			fromEnd = ArrowType.valueOf(jEdge.getString("fromEnd"));
		} catch (NullPointerException e) {
			fromEnd = null;
		}
		try {
			toEnd = ArrowType.valueOf(jEdge.getString("toEnd"));
		} catch (NullPointerException e) {
			toEnd = null;
		}
		try {
			label = jEdge.getString("label");
		} catch (NullPointerException e) {
			label = null;
		}
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Edge)) {
			return false;
		}
		Edge e = (Edge) o;
		return id.equals(e.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public JsonValue toJson() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("id", id);
		builder.add("fromNode", fromNode);
		builder.add("toNode", toNode);
		builder.add("fromSide", fromSide.toString());
		builder.add("toSide", toSide.toString());
		if (color != null) {
			builder.add("color", color);
		}
		if (fromEnd != null) {
			builder.add("fromEnd", fromEnd.toString());
		}
		if (toEnd != null) {
			builder.add("toEnd", toEnd.toString());
		}
		if (label != null) {
			builder.add("label", label);
		}
		return builder.build();
	}

	public String getFromNode() {
		return fromNode;
	}

	public String getToNode() {
		return toNode;
	}

	public Side getFromSide() {
		return fromSide;
	}

	public void setFromSide(Side fromSide) {
		this.fromSide = fromSide;
	}

	public Side getToSide() {
		return toSide;
	}

	public void setToSide(Side toSide) {
		this.toSide = toSide;
	}

}
