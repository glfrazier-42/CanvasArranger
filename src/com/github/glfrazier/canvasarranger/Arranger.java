package com.github.glfrazier.canvasarranger;

import static java.lang.Math.max;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Arranger {

	private static final int COL_SEPARATION = 80;
	private static final int ROW_SEPARATION = 80;

	private Anode root;

	public Arranger(Node root, Map<String, Node> nodes, Map<String, Edge> edges) {
		this.root = new Anode(root, 0);
		Set<Edge> edgeSet = new HashSet<>();
		edgeSet.addAll(edges.values());
		Map<Node, Anode> nodeMap = new HashMap<>();
		nodeMap.put(root, this.root);
		buildHierarchy(this.root, nodes, edges, edgeSet, nodeMap);
	}

	private void buildHierarchy(Anode root, Map<String, Node> nodes, Map<String, Edge> edges, Set<Edge> edgeSet,
			Map<Node, Anode> nodeMap) {
		List<Anode> nodesToProcess = new LinkedList<>();
		nodesToProcess.add(root);
		while (!nodesToProcess.isEmpty()) {
			Anode anode = nodesToProcess.remove(0);
			Node n = anode.node;
			for (Iterator<Edge> iter = edgeSet.iterator(); iter.hasNext();) {
				Edge e = iter.next();
				if (e.getFromNode().equals(n.getID())) {
					iter.remove();
					Node dst = nodes.get(e.getToNode());
					if (!nodeMap.containsKey(dst)) {
						Anode a = new Anode(dst, anode.depth + 1);
						nodeMap.put(dst, a);
						nodesToProcess.add(a);
						anode.children.add(new Aedge(e, anode, a));
					} else {
						anode.backlinks.add(new Aedge(e, anode, nodeMap.get(dst)));
					}
				}
			}
		}
	}

	public boolean arrange() {
		int midpointX = root.node.getX() + root.node.getWidth() / 2;
		boolean modified = arrangeChildren(root, midpointX, root.node.getY());
		modified |= fixEdges(root);
		return modified;
	}

	private boolean fixEdges(Anode root) {
		boolean modified = false;
		List<Anode> nodesToProcess = new LinkedList<>();
		nodesToProcess.add(root);
		while (!nodesToProcess.isEmpty()) {
			Anode node = nodesToProcess.remove(0);
			for (Aedge e : node.children) {
				if (e.edge.getFromSide() != Edge.Side.bottom || e.edge.getToSide() != Edge.Side.top) {
					modified = true;
					e.edge.setFromSide(Edge.Side.bottom);
					e.edge.setToSide(Edge.Side.top);
				}
				nodesToProcess.add(e.to);
			}
			if (!node.backlinks.isEmpty()) {
				int nodeCenter = node.node.getX() + (node.node.getWidth() / 2);
				for (Aedge e : node.backlinks) {
					Anode other = e.to;
					int oCenter = other.node.getX() + other.node.getWidth() / 2;
					if (oCenter < nodeCenter) {
						if (e.edge.getFromSide() != Edge.Side.left || e.edge.getToSide() != Edge.Side.right) {
							modified = true;
							e.edge.setFromSide(Edge.Side.left);
							e.edge.setToSide(Edge.Side.right);
						}
					} else {
						if (e.edge.getFromSide() != Edge.Side.right || e.edge.getToSide() != Edge.Side.left) {
							modified = true;
							e.edge.setFromSide(Edge.Side.right);
							e.edge.setToSide(Edge.Side.left);
						}
					}
				}
			}
		}
		return modified;
	}

	/**
	 * Locate the abstract box that this node is at the top of at (x,y), where (x,y)
	 * is the center-top of the box.
	 * 
	 * @param node
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean arrangeChildren(Anode node, int x, int y) {
		boolean modified = false;
		Point[] dims = node.getAggregateDims();
		int boxWidth = dims[0].x;
		int oldX = node.node.getX();
		int oldY = node.node.getY();
		int newX = x - node.node.getWidth() / 2;
		int newY = y;
		modified = (oldX != newX || oldY != newY);
		node.node.setX(newX);
		node.node.setY(newY);
		int nextY = y + node.node.getHeight() + ROW_SEPARATION;
		int nextX = x - boxWidth / 2;
		int index = 0;
		sortNodesLeftToRight(node.children);
		for (Aedge e : node.children) {
			Anode child = e.to;
			index += 1;
			modified |= arrangeChildren(child, nextX + dims[index].x / 2, nextY);
			nextX += COL_SEPARATION + dims[index].x;
		}
		return modified;
	}

	private static final Comparator<Aedge> leftToRight = new Comparator<>() {

		@Override
		public int compare(Aedge o1, Aedge o2) {
			return Integer.compare(o1.to.node.getX(), o2.to.node.getX());
		}
	};

	private void sortNodesLeftToRight(List<Aedge> children) {
		SortedSet<Aedge> sorted = new TreeSet<Aedge>(leftToRight);
		sorted.addAll(children);
		children.clear();
		for(Aedge ae : sorted) {
			children.add(ae);
		}
	}

	private static class Anode {

		Node node;
		Anode parent;
		List<Aedge> children;
		List<Aedge> backlinks;
		int depth;

		public Anode(Node n, int depth) {
			if (n == null) {
				throw new NullPointerException();
			}
			node = n;
			parent = null;
			children = new ArrayList<Aedge>();
			backlinks = new ArrayList<Aedge>();
			this.depth = depth;
		}

		public String toString() {
			return "anode:" + node.toString();
		}

		/**
		 * Returns the aggregate dimensions (width and height) of this node and, in
		 * order, the aggregate dims of its children.
		 * 
		 * @return
		 */
		public Point[] getAggregateDims() {
			Point[] result = new Point[children.size() + 1];
			int childrenWidth = 0;
			int childrenHeight = 0;
			if (children.size() > 0) {
				int index = 1;
				for (Aedge edge : children) {
					Anode child = edge.to;
					Point[] dims = child.getAggregateDims();
					childrenWidth += dims[0].x;
					childrenHeight = (int) max(dims[0].y, childrenHeight);
					result[index++] = dims[0];
				}
				childrenWidth += (children.size() - 1) * COL_SEPARATION;
			}
			int myWidth = max(node.getWidth(), childrenWidth);
			int myHeight = node.getHeight() + ROW_SEPARATION + childrenHeight;
			Point mySize = new Point(myWidth, myHeight);
			result[0] = mySize;
			return result;
		}

	}

	private static class Aedge {
		Edge edge;
		Anode from;
		Anode to;

		public Aedge(Edge e, Anode src, Anode dst) {
			edge = e;
			from = src;
			to = dst;
		}

		public String toString() {
			return "Aedge " + edge + ": " + from + " => " + to;
		}
	}
}
