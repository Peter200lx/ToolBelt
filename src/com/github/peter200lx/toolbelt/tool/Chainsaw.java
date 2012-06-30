package com.github.peter200lx.toolbelt.tool;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.Tool;

public class Chainsaw extends Tool  {

	public Chainsaw(GlobalConf gc) {
		super(gc);
	}

	public static String name = "saw";

	private int widthCube;
	private double radiusSphere;

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		List<Block> toChange;
		Player subject = event.getPlayer();
		if(!delayElapsed(subject.getName()))
			return;
		Block target;
		List<String> subRanks = gc.ranks.getUserRank(subject);
		switch(event.getAction()) {
		case LEFT_CLICK_BLOCK:
			target = event.getClickedBlock();
			toChange = getCube(target,widthCube,subRanks);
			break;
		case RIGHT_CLICK_BLOCK:
			target = event.getClickedBlock();
			toChange = getSphere(target,radiusSphere,subRanks);
			break;
		default:
			return;
		}
		if(toChange == null) {
			log.warning("["+gc.modName+"][Chainsaw] Got a null block selection");
			return;
		}
		for(Block cur: toChange) {
			if(spawnBuild(cur,event.getPlayer())) {
				if(isUseEvent()) {
					if(safeBreak(cur,event.getPlayer(),true))
						this.updateUser(subject, cur.getLocation(), 0, (byte)0);
				}else {
					cur.setTypeId(0,true);
					this.updateUser(subject, cur.getLocation(), 0, (byte)0);
				}
			}
		}
	}

	private List<Block> getCube(Block center, int width, List<String> subRanks) {
		int bound = (width-1)/2;
		List<Block> toRet = new ArrayList<Block>();
		for (int x = -bound; x <= bound; ++x) {
			for (int y = -bound; y <= bound; ++y) {
				for (int z = -bound; z <= bound; ++z) {
					Block loc = center.getRelative(x, y, z);
					if(!noOverwrite(subRanks,loc.getType())){
						toRet.add(loc);
					}
				}
			}
		}
		return toRet;
	}

	private List<Block> getSphere(Block center, double radius, List<String> subRanks) {
		List<Block> toRet = new ArrayList<Block>();
		int round = (int) Math.round(radius-0.001);
		for (int x = -round; x <= round; ++x) {
			for (int y = -round; y <= round; ++y) {
				for (int z = -round; z <= round; ++z) {
					Block loc = center.getRelative(x, y, z);
					if(loc.getLocation().toVector().isInSphere(
							center.getLocation().toVector(), radius)) {
						if(!noOverwrite(subRanks,loc.getType())){
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
			gc.pl.print(PrintEnum.CMD, sender, "Click with the "+ChatColor.GOLD+
					getType()+ChatColor.WHITE+" to cut down large chunks of trees");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the repeat delay
		if(!loadRepeatDelay(tSet,conf,-1))
			return false;

		widthCube = conf.getInt(tSet+"."+name+".widthCube", 3);
		radiusSphere = conf.getDouble(tSet+"."+name+".radiusSphere", 2.5);
		if(isDebug()) {
			log.info("["+gc.modName+"][loadConf] Chainsaw Cube size set to "+widthCube);
			log.info("["+gc.modName+"][loadConf] Chainsaw Sphere radius set to "+radiusSphere);
		}

		if(!loadOnlyAllow(tSet, conf))
			return false;

		if(!loadStopOverwrite(tSet, conf))
			return false;

		return true;
	}
}
