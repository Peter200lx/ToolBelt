package com.github.peter200lx.toolbelt;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author peter200lx
 *
 * Structure to initialize and access the Ranks configured by the server admin.
 */
public class Ranks {

	/**
	 * Initialize the Ranks structure by reading the ranksDef
	 *     ConfigurationSection from config.yml (Specifically RanksDef).
	 *
	 * This is done in a three-step process.
	 *     1) The config.yml parsed to read all possible rank names, which are
	 *         stored in posNames.
	 *     2) posNames is parsed to convert the rank names into a pool of
	 *         RankNode objects (posNodes).
	 *     3) The pool of RankNodes is checked to find any nodes not referenced
	 *         by any other nodes, which are then stored in entryPoints.
	 *
	 * @param sect ConfigurationSection to read from (ranksDef in config.yml)
	 * @param modName name of Bukkit Plugin for defining permission prefix
	 */
	public Ranks(ConfigurationSection sect, String modName) {
		prefix = modName.toLowerCase() + ".rank.";
		entryPoints = new LinkedList<RankNode>();
		if (sect == null) {
			return;
		}
		List<String> posNames = new LinkedList<String>();
		LinkedList<RankNode> posNodes = new LinkedList<RankNode>();
		//1) Sanitize our input into list of String names
		for (Entry<String, Object> entry :sect.getValues(false).entrySet()) {
			if ((entry.getKey().toLowerCase().equals(global))) {
				throw new RuntimeException("Can't declare a rank with a "
						+ "reserved name: " + entry.getKey());
			}
			if (posNames.contains(entry.getKey().toLowerCase())) {
				throw new RuntimeException("More then one rank '"
						+ entry.getKey() + "' have been declared");
			}
			posNames.add(entry.getKey().toLowerCase());
		}

		if (posNames.isEmpty()) {
			return;
		}

		//2) Convert String names to RankNode objects
		for (String nodeName:posNames) {
			addNode(sect, posNames, posNodes, nodeName, null);
		}
		//3) If any other node contains a given node, don't add to entryPoints
		for (RankNode node:posNodes) {
			boolean contained = false;
			for (RankNode others:posNodes) {
				if ((others != node) && (others.contains(node))) {
					contained = true;
					break;
				}
			}
			if (!contained) {
				entryPoints.add(node);
			}
		}
	}

	/**
	 * Name of the terminating "node". This is represented by the null next
	 *     object in the data structure itself. It is used in config.yml in the
	 *     ranksDef ConfigurationSection.
	 */
	private final String global = "unranked";
	/**
	 * String specifying the subsection in config.yml that specifies the next
	 *     RankNode to reference.
	 */
	private final String fbName = "fallback";
	/**
	 * Shared portion of permission string for the Ranks.
	 *     (in ToolBelt: "toolbelt.rank.")
	 */
	private final String prefix;

	/**
	 * Ordered list of starting nodes. When checking to see if a user has a
	 *     specific rank, this list will be searched in a depth-first approach.
	 */
	private List<RankNode> entryPoints;

	/**
	 * Get the shared portion of the permission string for rank checking.
	 *
	 * @return shared portion of permission string
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Get a list of user's rank and fallback ranks. If there are no Ranks
	 *     defined, or if the user has no rank permission(s), return null.
	 *
	 * @param user Player to check Permissions on
	 * @return null if user has no rank, List of rank names otherwise
	 */
	public List<String> getUserRank(Player user) {
		if (entryPoints.isEmpty()) {
			return null;
		}
		List<String> ret = new LinkedList<String>();
		RankNode node = null;
		for (RankNode entry:entryPoints) {
			node = entry;
			while (node != null) {
				if (user.hasPermission(prefix + node.getName())) {
					break;
				} else {
					node = node.getNext();
				}
			}
			if (node != null) {
				break;
			}
		}
		while (node != null) {
			ret.add(node.getName());
			node = node.getNext();
		}
		if (ret.isEmpty()) {
			return null;
		} else {
			return ret;
		}
	}

	/**
	 * Get a complete list of all Ranks defined in config.yml. This list will
	 *     not have duplicates.
	 *
	 * @return List of all possible Ranks, empty list if none defined.
	 */
	public List<String> getRanks() {
		List<String> ret = new LinkedList<String>();
		for (RankNode entry:entryPoints) {
			for (RankNode node:entry) {
				if (!ret.contains(node.getName())) {
					ret.add(node.getName());
				}
			}
		}
		return ret;
	}

	/**
	 * Print Ranks structure to logger. The format is with each entryPoint
	 *     listed in the first column, and then the entire linked list that
	 *     is referenced by that entryPoint tabbed in one. This means that each
	 *     entryPoint will be listed twice, once as an entryPoint, and once as
	 *     the first item in the linked list.
	 *
	 * @param log Java Logger to dump list to.
	 */
	public void printRanks(Logger log) {
		for (RankNode entry:entryPoints) {
			log.info(entry.getName());
			for (RankNode node:entry) {
				log.info("\t" + node.getName());
			}
			log.info("\t" + global);
		}
	}

	/**
	 * Add RankNodes from names in posNames to nodes in posNodes. This is a
	 *     recursive function that runs until it hits one with a fallback to
	 *     the global unranked "node", or until it hits a node already
	 *     initialized. This way nodes will always be initialized with all
	 *     fallback nodes already defined.
	 *
	 * @param sect ConfigurationSection to read from (ranksDef in config.yml)
	 * @param posNames List of all possible rank names
	 * @param posNodes List of all RankNodes already initialized
	 * @param newName name of current rank
	 * @param entryName name of parent RankNode (used to prevent loops)
	 */
	private void addNode(ConfigurationSection sect, List<String> posNames,
			List<RankNode>posNodes, String newName, String entryName) {
		for (RankNode node: posNodes) {
			if (node.getName().equals(newName)) {
				return;
			}
		}
		String fallback = sect.getString(newName + "." + fbName).toLowerCase();
		if (fallback.equals(global)) {
			posNodes.add(new RankNode(newName));
		} else if (!posNames.contains(fallback)) {
			throw new RuntimeException("Rank >" + newName
					+ "< is trying to fallback to >" + fallback
					+ "< Which is not a defined rank");
		} else {
			if (entryName == null) {
				entryName = newName;
			} else if (entryName.equals(newName)) {
				throw new RuntimeException("Loop detected in fallbacks, >"
						+ newName + "< falls back to itself through some "
						+ "level of children");
			}
			addNode(sect, posNames, posNodes, fallback, entryName);
			for (RankNode node: posNodes) {
				if (node.getName().equals(fallback)) {
					posNodes.add(new RankNode(newName, node));
					return;
				}
			}
		}
	}

	/**
	 * @author peter200lx
	 *
	 * Data structure to hold ranks/groups defined by server admin. This is
	 *     implemented as a linked list where multiple nodes can point at
	 *     the same node, with no cycles.
	 */
	private class RankNode implements Iterable<RankNode> {

		/**
		 * Initialize a node with a reference to the next node. This code will
		 *     verify that we are not creating a loop with this addition.
		 *
		 * @param name User specified name for node (pulled from config.yml)
		 * @param next RankNode to reference
		 */
		RankNode(String name, RankNode next) {
			this.name = name;
			if (!setNext(next)) {
				throw new RuntimeException("Loop detected in fallbacks, >"
						+ this.name + "< falls back to itself through some "
						+ "level of children");
			}
		}

		/**
		 * Initialize a node with no next reference.
		 *
		 * @param name User specified name for node (pulled from config.yml)
		 */
		RankNode(String name) {
			this.name = name;
		}

		/**
		 * User specified name of RankNode. Specified in ranksDef in config.yml
		 */
		private String name;
		/**
		 * Reference to next node in list.
		 */
		private RankNode next = null;

		/**
		 * Return node name.
		 *
		 * @return name of node
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Set reference to next RankNode.
		 *
		 * @param nextNode next RankNode to reference.
		 * @return true of reference set, false if param is already present
		 */
		public boolean setNext(RankNode nextNode) {
			if (contains(nextNode)) {
				return false;
			}
			this.next = nextNode;
			return true;
		}

		/**
		 * Get reference to next RankNode from current node.
		 *
		 * @return next RankNode in list
		 */
		public RankNode getNext() {
			return this.next;
		}

		/**
		 * Determine if referenced node is linked to by current node.
		 *
		 * @param node node to find
		 * @return true if node is linked from starting node, false otherwise
		 */
		public boolean contains(RankNode node) {
			if (next == null) {
				return false;
			} else if (next == node) {
				return true;
			} else {
				return next.contains(node);
			}
		}

		/**
		 * Create a RankNode Iterator object.
		 *
		 * @return RankNode Iterator object
		 */
		public Iterator<RankNode> iterator() {
			return new RankNodeIter(this);
		}

		/**
		 * @author peter200lx
		 *
		 * Iterator implementation for RankNode data structure.
		 */
		private class RankNodeIter implements Iterator<RankNode> {

			/**
			 * Initialize Iterator with the starting node.
			 *
			 * @param first starting node
			 */
			RankNodeIter(RankNode first) {
				next = first;
			}

			/**
			 * Reference to next node for Iterator.
			 */
			private RankNode next;

			@Override
			public boolean hasNext() {
				if (next == null) {
					return false;
				}
				return true;
			}

			@Override
			public RankNode next() {
				if (next == null) {
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
