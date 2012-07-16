package com.github.peter200lx.toolbelt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author peter200lx
 *
 * Container object for Sets of Materials.
 * At its most basic, simply a pass-through to HashSet<Material>
 * If ranks are enabled, it will have separate lists for each rank.
 */
public class SetMat {

	/**
	 * Initialize the Material list.
	 *
	 * @param log pass in the "Minecraft" logger for debug output
	 * @param modName name of the Bukkit plugin
	 * @param listName name of this list in config.yml
	 */
	public SetMat(Logger log, String modName, String listName) {
		super();
		this.log = log;
		this.modName = modName;
		this.listName = listName;
	}

	/**
	 * "Minecraft" logger for debug output.
	 */
	private final Logger log;

	/**
	 * Name of the minecraft plugin.
	 */
	private final String modName;

	/**
	 * Name of this list in config.yml .
	 */
	private final String listName;

	/**
	 * unranked Material list.
	 */
	private HashSet<Material> list = null;
	/**
	 * ranked Material lists.
	 * A mapping of rank name -> Material list for that rank
	 */
	private HashMap<String, HashSet<Material>> ranked =
			new HashMap<String, HashSet<Material>>();

	/**
	 * Check if unranked list is empty.
	 *
	 * @return true if unranked list is empty, false otherwise
	 */
	public final boolean isEmpty() {
		if (list == null) {
			return true;
		}
		return list.isEmpty();
	}

	/**
	 * Check if the listed ranks and unranked lists are empty.
	 * Compares the rank names against the name in Ranked.
	 * If a list name matches, then checks if the first matched list is empty.
	 * Otherwise it checks to see if the unranked list is empty.
	 *
	 * @param ranks list of rank names to check
	 * @return true if matching list is empty, false otherwise
	 */
	public final boolean isEmpty(List<String> ranks) {
		HashSet<Material> checkList = list;
		if (ranks != null) {
			for (String rank:ranks) {
				if (ranked.containsKey(rank)) {
					checkList = ranked.get(rank);
					break;
				}
			}
		}
		if (checkList == null) {
			return true;
		}
		return checkList.isEmpty();
	}

	/**
	 * Check if unranked list contains a Material.
	 *
	 * @param mat Material to test
	 * @return true if Material is present, false otherwise
	 */
	public final boolean contains(Material mat) {
		if (list == null) {
			return false;
		}
		return list.contains(mat);
	}

	/**
	 * Check if listed ranks and unranked lists contains a Material.
	 * Compares the rank names against the name in Ranked.
	 * If a list name matches, then checks if the first matched list contains
	 * the Material in question. Otherwise it checks to see if the unranked
	 * list contains the Material.
	 *
	 * @param ranks list of rank names to check
	 * @param mat Material to test
	 * @return true if any
	 */
	public final boolean contains(List<String> ranks, Material mat) {
		HashSet<Material> checkList = list;
		if (ranks != null) {
			for (String rank:ranks) {
				if (ranked.containsKey(rank)) {
					checkList = ranked.get(rank);
					break;
				}
			}
		}
		if (checkList == null) {
			return false;
		}
		return checkList.contains(mat);
	}

	/**
	 * Initialize the unranked Material list from an Integer List.
	 *
	 * @param input Integer list from ConfigurationSection
	 * @param useDefStop true to initialize the list with unsupported Materials
	 * @param baseName base string from config.yml (used for logging warnings)
	 * @return true if no errors occur, false otherwise
	 */
	public final boolean loadMatList(List<Integer> input,
			boolean useDefStop, String baseName) {
		list = intL2matHS(input, useDefStop, baseName);
		if (list == null) {
			return false;
		} else if (list.isEmpty()) {
			list = null;
			return true;
		} else {
			return true;
		}
	}

	/**
	 * Convert an Integer list into a Material list.
	 *
	 * @param input Integer list from ConfigurationSection
	 * @param useDefStop true to initialize the list with unsupported Materials
	 * @param baseName base string from config.yml (used for logging warnings)
	 * @return Material list containing mapped items from Integer list
	 */
	private HashSet<Material> intL2matHS(List<Integer> input,
			boolean useDefStop, String baseName) {
		HashSet<Material> ret = new HashSet<Material>();
		if (input == null) {
			log.warning("[" + modName + "] " + baseName + "." + listName
					+ " is returning null");
			return null;
		} else if (input.isEmpty()) {
			return ret;
		}
		if (useDefStop) {
			ret.add(Material.AIR);
			ret.add(Material.BED_BLOCK);
			ret.add(Material.PISTON_EXTENSION);
			ret.add(Material.PISTON_MOVING_PIECE);
			ret.add(Material.FIRE);
			ret.add(Material.CHEST);
		}
		for (Integer entry : input) {
			if (entry > 0) {
				Material type = Material.getMaterial(entry);
				if (type != null) {
					ret.add(type);
					continue;
				}
			}
			log.warning("[" + modName + "] " + baseName + "." + listName
					+ ": '" + entry + "' is not a Material type");
			return null;
		}
		return ret;
	}

	/**
	 * Load the ranked lists (if any) from the config.yml file.
	 *
	 * @param sect section of config.yml to load ranked lists from
	 * @param ranks object that provides all configured ranks
	 * @param baseName base string from config.yml (used for logging warnings)
	 * @return true if no errors occur, false otherwise
	 */
	public final boolean loadRankedMatLists(ConfigurationSection sect,
			Ranks ranks, String baseName) {
		if (ranks == null) {
			return false;
		}
		if (ranks.getRanks() == null || sect == null) {
			return true;
		}
		for (String rank:ranks.getRanks()) {
			if (sect.contains(rank)) {
				if (sect.isList(rank + "." + listName)) {
					List<Integer> intL = sect.getIntegerList(
							rank + "." + listName);

					HashSet<Material> temp = intL2matHS(intL, false,
							baseName + "." + rank);
					if (temp == null) {
						return false;
					}
					ranked.put(rank, temp);
				}
			}
		}
		return true;
	}

	/**
	 * Log all Materials in unranked list.
	 *
	 * @param func name of calling function (for printout)
	 * @param baseName base string from config.yml (portion before list name)
	 */
	public final void logMatSet(String func, String baseName) {
		if (list == null) {
			log.info("[" + modName + "][" + func + "] " + baseName + "."
					+ listName + ": is empty");
			return;
		}
		for (Material mat: list) {
			log.info("[" + modName + "][" + func + "] " + baseName
					+ "." + listName + ": " + mat.toString());
		}
	}

	/**
	 * Log all Materials in ranked lists (excluding unranked).
	 *
	 * @param func name of calling function (for printout)
	 * @param baseName base string from config.yml (portion before list name)
	 */
	public final void logRankedMatSet(String func, String baseName) {
		for (Entry<String, HashSet<Material>> rank:ranked.entrySet()) {
			for (Material mat: rank.getValue()) {
				log.info("[" + modName + "][" + func + "] " + baseName + "."
						+ rank.getKey() + "." + listName + ": "
						+ mat.toString());
			}
			if (rank.getValue().isEmpty()) {
				log.info("[" + modName + "][" + func + "] " + baseName + "."
						+ rank.getKey() + "." + listName + ": is empty");
			}
		}
	}

	/**
	 * Manually overwrite the unranked list.
	 *
	 * @param newList unranked Material list to write
	 */
	public final void setList(HashSet<Material> newList) {
		this.list = newList;
	}

	/**
	 * Manually overwrite the ranked lists object.
	 *
	 * @param newMap ranked lists object to write
	 */
	public final void setRankedList(HashMap<String, HashSet<Material>> newMap) {
		this.ranked = newMap;
	}

	/**
	 * Performs a deep copy of all contained lists.
	 *
	 * @return a new deep copy of the original object
	 */
	public final SetMat copy() {
		SetMat newVersion = new SetMat(log, modName, listName);
		if (list != null) {
			newVersion.setList(new HashSet<Material>(list));
		}
		newVersion.setRankedList(
				new HashMap<String, HashSet<Material>>(ranked));
		return newVersion;
	}
}
