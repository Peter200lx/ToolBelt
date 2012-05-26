package com.github.peter200lx.toolbelt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class SetMat {

	public SetMat(Logger log, String modName, String listName) {
		super();
		this.log = log;
		this.modName = modName;
		this.listName = listName;
	}

	private final Logger log;

	private final String modName;

	private final String listName;

	HashSet<Material> list = null;
	HashMap<String,HashSet<Material>> ranked = new HashMap<String,HashSet<Material>>();

	public boolean isEmpty() {
		if(list == null)
			return true;
		return list.isEmpty();
	}

	public boolean isEmpty(List<String> ranks) {
		HashSet<Material> checkList = list;
		if(ranks != null) {
			for(String rank:ranks) {
				if(ranked.containsKey(rank)) {
					checkList = ranked.get(rank);
					break;
				}
			}
		}
		if(checkList == null)
			return true;
		return checkList.isEmpty();
	}

	public boolean contains(Material mat) {
		if(list == null)
			return false;
		return list.contains(mat);
	}

	public boolean contains(List<String> ranks, Material mat) {
		HashSet<Material> checkList = list;
		if(ranks != null) {
			for(String rank:ranks) {
				if(ranked.containsKey(rank)) {
					checkList = ranked.get(rank);
					break;
				}
			}
		}
		if(checkList == null)
			return false;
		return checkList.contains(mat);
	}

	public boolean loadMatList(List<Integer> input,
			boolean useDefStop, String baseName) {
		list = intL2matHS(input,useDefStop,baseName);
		if(list == null)
			return false;
		else if(list.isEmpty()) {
			list = null;
			return true;
		}else
			return true;
	}

	private HashSet<Material> intL2matHS(List<Integer> input,
			boolean useDefStop, String baseName) {
		HashSet<Material> ret = new HashSet<Material>();
		if(input == null) {
			log.warning("["+modName+"] "+baseName+"."+listName+" is returning null");
			return null;
		}else if(input.isEmpty()) {
			return ret;
		}
		if(useDefStop){
			ret.add(Material.AIR);
			ret.add(Material.BED_BLOCK);
			ret.add(Material.PISTON_EXTENSION);
			ret.add(Material.PISTON_MOVING_PIECE);
			ret.add(Material.FIRE);
			ret.add(Material.CHEST);
		}
		for(Integer entry : input) {
			if(entry > 0) {
				Material type = Material.getMaterial(entry);
				if(type != null) {
					ret.add(type);
					continue;
				}
			}
			log.warning("["+modName+"] "+baseName+"."+listName +
					": '" + entry + "' is not a Material type" );
			return null;
		}
		return ret;
	}

	public boolean loadRankedMatLists(ConfigurationSection sect,
			Ranks ranks, String baseName) {
		if(ranks == null)
			return false;
		if(ranks.getRanks() == null || sect == null)
			return true;
		for(String rank:ranks.getRanks()) {
			if(sect.contains(rank)) {
				if(sect.isList(rank+"."+listName)) {
					List<Integer> intL = sect.getIntegerList(rank+"."+listName);

					HashSet<Material> temp = intL2matHS(intL,false,baseName+"."+rank);
					if(temp == null)
						return false;
					ranked.put(rank, temp);
				}
			}
		}
		return true;
	}

	public void logMatSet(String func, String summary) {
		if(list == null) {
			log.info("["+modName+"]["+func+"] "+summary+"."+
					listName+": is empty");
			return;
		}
		for(Material mat: list) {
			log.info("["+modName+"]["+func+"] "+summary+"."+
					listName+": "+mat.toString());
		}
	}

	public void logRankedMatSet(String func, String summary) {
		for(Entry<String, HashSet<Material>> rank:ranked.entrySet()) {
			for(Material mat: rank.getValue()) {
				log.info("["+modName+"]["+func+"] "+summary+"."+rank.getKey()+
						"."+listName+": "+mat.toString());
			}
			if(rank.getValue().isEmpty()) {
				log.info("["+modName+"]["+func+"] "+summary+"."+rank.getKey()+
						"."+listName+": is empty");
			}
		}
	}

	public void SetList(HashSet<Material> newList) {
		this.list = newList;
	}

	public void SetRankedList(HashMap<String,HashSet<Material>> newMap) {
		this.ranked = newMap;
	}

	public SetMat copy() {
		SetMat newVersion = new SetMat(log,modName,listName);
		if(list != null)
			newVersion.SetList(new HashSet<Material>(list));
		newVersion.SetRankedList(new HashMap<String,HashSet<Material>>(ranked));
		return newVersion;
	}
}
