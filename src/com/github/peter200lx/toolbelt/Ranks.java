package com.github.peter200lx.toolbelt;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Ranks {

	public Ranks(ConfigurationSection sect, String modName) {
		prefix = modName.toLowerCase()+".rank.";
		entryPoints = new LinkedList<RankNode>();
		if(sect == null)
			return;
		List<String> posNames = new LinkedList<String>();
		LinkedList<RankNode> posNodes = new LinkedList<RankNode>();
		//Sanitize our input into list of String names
		for(Entry<String, Object> entry :sect.getValues(false).entrySet()) {
			if((entry.getKey().toLowerCase().equals(global)))
				throw new RuntimeException("Can't declare a rank with a reserved name: "
						+entry.getKey());
			if(posNames.contains(entry.getKey().toLowerCase()))
				throw new RuntimeException("More then one rank '"+entry.getKey()+
						"' have been declared");
			posNames.add(entry.getKey().toLowerCase());
		}

		if(posNames.isEmpty()) {
			return;
		}

		//Convert String names to RankNode objects
		for(String nodeName:posNames) {
			addNode(sect,posNames,posNodes,nodeName,null);
		}
		//If any other node contains a given node, don't add it to entryPoints
		for(RankNode node:posNodes) {
			boolean contained = false;
			for(RankNode others:posNodes) {
				if((others != node)&&(others.contains(node))) {
					contained = true;
					break;
				}
			}
			if(!contained)
				entryPoints.add(node);
		}
	}

	private final String global = "unranked";
	private final String fbName = "fallback";
	private final String prefix;

	private List<RankNode> entryPoints;

	public String getPrefix() {
		return prefix;
	}

	public List<String> getUserRank(Player user) {
		if(entryPoints.isEmpty()) {
			return null;
		}
		List<String> ret = new LinkedList<String>();
		RankNode node = null;
		for(RankNode entry:entryPoints) {
			node = entry;
			while(node != null) {
				if(user.hasPermission(prefix+node.getName())) {
					break;
				}else {
					node = node.getNext();
				}
			}
			if(node != null)
				break;
		}
		while(node != null) {
			ret.add(node.getName());
			node = node.getNext();
		}
		if(ret.isEmpty())
			return null;
		else
			return ret;
	}

	public List<String> getRanks() {
		List<String> ret = new LinkedList<String>();
		for(RankNode entry:entryPoints) {
			for(RankNode node:entry) {
				if(!ret.contains(node.getName())) {
					ret.add(node.getName());
				}
			}
		}
		return ret;
	}

	public void printRanks(Logger log) {
		for(RankNode entry:entryPoints) {
			log.info(entry.getName());
			for(RankNode node:entry) {
				log.info("\t"+node.getName());
			}
			log.info("\t"+global);
		}
	}

	private void addNode(ConfigurationSection sect, List<String> posNames,
			List<RankNode>posNodes, String newName, String entryName) {
		for(RankNode node: posNodes) {
			if(node.getName().equals(newName))
				return;
		}
		String fallback = sect.getString(newName+"."+fbName).toLowerCase();
		if(fallback.equals(global)) {
			posNodes.add(new RankNode(newName));
		}else if(!posNames.contains(fallback)) {
			throw new RuntimeException("Rank >"+newName+"< is trying to fallback to >"+
					fallback+"< Which is not a defined rank");
		}else {
			if(entryName == null) {
				entryName = newName;
			}else if(entryName.equals(newName)) {
				throw new RuntimeException("Loop detected in fallbacks, >"+newName+
						"< falls back to itself through some level of children");
			}
			addNode(sect,posNames,posNodes,fallback,entryName);
			for(RankNode node: posNodes) {
				if(node.getName().equals(fallback)) {
					posNodes.add(new RankNode(newName,node));
					return;
				}
			}
		}
	}

	private class RankNode implements Iterable<RankNode> {

		RankNode(String name, RankNode next) {
			this.name = name;
			if(!setNext(next)) {
				throw new RuntimeException("Loop detected in fallbacks, >"+this.name+
						"< falls back to itself through some level of children");
			}
		}

		RankNode(String name) {
			this.name = name;
		}

		private String name;
		private RankNode next = null;

		public String getName() {
			return this.name;
		}

		public boolean setNext(RankNode next) {
			if(contains(next)) {
				return false;
			}
			this.next = next;
			return true;
		}

		public RankNode getNext() {
			return this.next;
		}

		public boolean contains(RankNode node) {
			if(next == null)
				return false;
			else if(next == node)
				return true;
			else
				return next.contains(node);
		}

		public Iterator<RankNode> iterator() {
			return new RankNodeIter(this);
		}

		private class RankNodeIter implements Iterator<RankNode> {

			RankNodeIter(RankNode first) {
				next = first;
			}

			private RankNode next;

			@Override
			public boolean hasNext() {
				if(next == null)
					return false;
				return true;
			}

			@Override
			public RankNode next() {
				if(next == null){
		            throw new NoSuchElementException();
			    }
				RankNode cur = next;
				next = cur.getNext();
			    return cur;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}
	}
}
