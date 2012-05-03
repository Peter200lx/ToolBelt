package com.github.peter200lx.toolbelt;

import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;

public class SetMat {

	public SetMat(Logger log, String modName) {
		super();
		this.log = log;
		this.modName = modName;
	}

	private final Logger log;

	private final String modName;

	HashSet<Material> list;

	public boolean isEmpty() {
		if(list == null)
			return true;
		return list.isEmpty();
	}

	public boolean contains(Material mat) {
		if(list == null)
			return false;
		return list.contains(mat);
	}

	public boolean loadMatList(List<Integer> input,
			boolean useDefStop, String warnMessage) {
		if(input == null) {
			log.warning("["+modName+"] "+warnMessage+" is returning null");
			return false;
		}else if(input.isEmpty()) {
			list = null;
			return true;
		}
		list = new HashSet<Material>();
		if(useDefStop){
			list.add(Material.AIR);
			list.add(Material.BED_BLOCK);
			list.add(Material.PISTON_EXTENSION);
			list.add(Material.PISTON_MOVING_PIECE);
			list.add(Material.FIRE);
			list.add(Material.CHEST);
		}
		for(Integer entry : input) {
			if(entry > 0) {
				Material type = Material.getMaterial(entry);
				if(type != null) {
					list.add(type);
					continue;
				}
			}
			log.warning("["+modName+"] "+warnMessage + ": '" + entry +
					"' is not a Material type" );
			return false;
		}
		return true;
	}

	public void logMatSet(String func, String summary) {
		if(list == null) {
			log.info("["+modName+"]["+func+"] "+summary+" is empty");
			return;
		}
		for(Material mat: list) {
			log.info("["+modName+"]["+func+"] "+summary+" "+mat.toString());
		}
	}
}
