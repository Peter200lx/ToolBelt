package com.github.peter200lx.toolbelt.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import com.github.peter200lx.toolbelt.Tool;

public class Chainsaw extends Tool  {

	public Chainsaw(String modName, Server server, boolean debug,
			boolean permissions, boolean useEvent) {
		super(modName, server, debug, permissions, permissions);
	}

	public static String name = "saw";

	private int widthCube;
	private double radiusSphere;

	private HashMap<String, Long> pCooldown = new HashMap<String, Long>();

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		List<Block> toChange;
		Player subject = event.getPlayer();
		if(pCooldown.containsKey(subject.getName()) &&
				(System.currentTimeMillis() < (pCooldown.get(subject.getName())+500)))
			return;
		pCooldown.put(subject.getName(), System.currentTimeMillis());
		Block target;
		switch(event.getAction()) {
		case LEFT_CLICK_BLOCK:
			target = event.getClickedBlock();
			toChange = getCube(target,widthCube);
			break;
		case RIGHT_CLICK_BLOCK:
			target = event.getClickedBlock();
			toChange = getSphere(target,radiusSphere);
			break;
		default:
			return;
		}
		if(toChange == null) {
			//Change this message if getSphere/Cube can return null
			subject.sendMessage("Plugin had an error!");
			return;
		}
		for(Block cur: toChange) {
			if(spawnBuild(cur,event.getPlayer())) {
				if(isUseEvent()) {
					if(safeBreak(cur,event.getPlayer(),true))
						subject.sendBlockChange(cur.getLocation(), 0, (byte)0);
				}else {
					cur.setTypeId(0,true);
					subject.sendBlockChange(cur.getLocation(), 0, (byte)0);
				}
			}
		}
	}

	private List<Block> getCube(Block center, int width) {
		int bound = (width-1)/2;
		List<Block> toRet = new ArrayList<Block>();
		for (int x = -bound; x <= bound; ++x) {
			for (int y = -bound; y <= bound; ++y) {
				for (int z = -bound; z <= bound; ++z) {
					Block loc = center.getRelative(x, y, z);
					if(!stopOverwrite.contains(loc.getType())       &&
							(onlyAllow.isEmpty() || onlyAllow.contains(loc.getType())) ){
						toRet.add(loc);
					}
				}
			}
		}
		return toRet;
	}

	private List<Block> getSphere(Block center, double radius) {
		List<Block> toRet = new ArrayList<Block>();
		int round = (int) Math.round(radius-0.001);
		for (int x = -round; x <= round; ++x) {
			for (int y = -round; y <= round; ++y) {
				for (int z = -round; z <= round; ++z) {
					Block loc = center.getRelative(x, y, z);
					if(loc.getLocation().toVector().isInSphere(
							center.getLocation().toVector(), radius)) {
						if(!stopOverwrite.contains(loc.getType())       &&
								(onlyAllow.isEmpty() || onlyAllow.contains(loc.getType())) ){
							toRet.add(loc);
						}
					}
				}
			}
		}
		return toRet;
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to cut down large chunks of trees");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the default restriction configuration
		if(!loadGlobalRestrictions(tSet,conf))
			return false;

		widthCube = conf.getInt(tSet+"."+name+".widthCube", 3);
		radiusSphere = conf.getDouble(tSet+"."+name+".radiusSphere", 2.5);
		if(isDebug()) {
			log.info("["+modName+"][loadConf] Chainsaw Cube size set to "+widthCube);
			log.info("["+modName+"][loadConf] Chainsaw Sphere radius set to "+radiusSphere);
		}

		List<Integer> intL = conf.getIntegerList(tSet+"."+name+".onlyAllow");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" it overwrites the global");

			onlyAllow = loadMatList(intL,new HashSet<Material>(),tSet+"."+name+".onlyAllow");
			if(onlyAllow == null)
				return false;

			if(isDebug()) {
				logMatSet(onlyAllow,"loadGlobalRestrictions",name+".onlyAllow:");
				log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" only those materials are usable");
			}
		} else if(isDebug()&& !onlyAllow.isEmpty()) {
			log.info( "["+modName+"][loadConf] As global.onlyAllow has items,"+
					" only those materials are usable");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopOverwrite");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".stopOverwrite has items,"+
						" it overwrites the global");

			stopOverwrite = loadMatList(intL,defStop(),tSet+"."+name+".stopOverwrite");
			if(stopOverwrite == null)
				return false;

			if(isDebug()) logMatSet(stopOverwrite,"loadGlobalRestrictions",
					name+".stopOverwrite:");
		}
		return true;
	}
}
