package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.material.MaterialData;

import com.github.peter200lx.toolbelt.Tool;

public class Paint extends Tool  {

	public Paint(String modName, boolean debug, boolean permissions) {
		super(modName, debug, permissions);
	}

	public static String name = "paint";

	private boolean range = false;

	private Integer dist = 25;

	private HashMap<String, HashMap<Integer, MaterialData>> pPalette = new HashMap<String, HashMap<Integer, MaterialData>>();

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		if(!pPalette.containsKey(subject.getName())) {
			pPalette.put(subject.getName(), new HashMap<Integer,MaterialData>());
		}
		if(event.getAction().equals(Action.LEFT_CLICK_AIR)			||
				event.getAction().equals(Action.LEFT_CLICK_BLOCK)	){
			//Acquire paint
			MaterialData target = null;
			if(event.getAction().equals(Action.LEFT_CLICK_BLOCK))
				target = event.getClickedBlock().getState().getData();
			else
				target = subject.getTargetBlock(null, 200).getState().getData();
			if(!stopCopy.contains(target.getItemType()) && ( onlyAllow.isEmpty() ||
					onlyAllow.contains(target.getItemType()) ) ){
				pPalette.get(subject.getName()).put(subject.getInventory().getHeldItemSlot(), target );
				paintPrint("Paint is now ",subject,target);
			} else {
				subject.sendMessage(ChatColor.RED + "Was not able to grab a block to paint.");
				MaterialData old = pPalette.get(subject.getName()).get(subject.getInventory().getHeldItemSlot());
				paintPrint("Paint is still ",subject,old);
			}
		} else if(event.getAction().equals(Action.RIGHT_CLICK_AIR)	||
				event.getAction().equals(Action.RIGHT_CLICK_BLOCK)	){
			//Draw paint
			MaterialData set = pPalette.get(subject.getName()).get(subject.getInventory().getHeldItemSlot());
			if(set != null) {
				Block target = null;
				if(range && hasRangePerm(subject) && event.getAction().equals(Action.RIGHT_CLICK_AIR) ){
					target = subject.getTargetBlock(null, dist);
				}else if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
					target = event.getClickedBlock();
				if(target != null && !stopOverwrite.contains(target.getType())       &&
						(onlyAllow.isEmpty() || onlyAllow.contains(target.getType())) ){
					target.setTypeIdAndData(set.getItemTypeId(), set.getData(), false);
				}else if(target != null) {
					if(target.getType().equals(Material.AIR))
						subject.sendMessage(ChatColor.RED + "Target is out of range");
					else
						subject.sendMessage(ChatColor.RED + "You can't overwrite "+ChatColor.GOLD+target.getType());
				}
			}
		}
	}

	private void paintPrint(String prefix, CommandSender subject, MaterialData m) {
		if(m == null)
			subject.sendMessage(ChatColor.RED + prefix + ChatColor.GOLD + "empty");
		else if(printData.contains(m.getItemType())||(m.getData() != 0))
			subject.sendMessage(ChatColor.GREEN + prefix + ChatColor.GOLD +
					m.getItemType().toString() + ChatColor.WHITE + ":" +
					ChatColor.BLUE + data2Str(m));
		else
			subject.sendMessage(ChatColor.GREEN + prefix + ChatColor.GOLD +
						m.getItemType().toString());
	}

	private boolean hasRangePerm(CommandSender subject) {
		if(isPermissions())
			return subject.hasPermission(getPermStr()+".range");
		else
			return true;
	}

	@Override
	public void handleItemChange(PlayerItemHeldEvent event) {
		Player subject = event.getPlayer();
		if(pPalette.containsKey(subject.getName())				&&
				(pPalette.get(subject.getName()).size() > 1)	){
			MaterialData c = pPalette.get(subject.getName()).get(event.getNewSlot());
			paintPrint("Paint in this slot is ",subject,c);
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Left-click with the "+getType()+
					" to load a block into your paintbrush");
			sender.sendMessage("Right-click with the "+getType()+
					" to paint that block");
			if(range)
				sender.sendMessage("Be careful, you can paint at a range of up to "+
						dist+" blocks.");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {
		range = conf.getBoolean(tSet+"."+name+".range", false);
		if(isDebug()) log.info("["+modName+"][loadConf] Painting at range is set to "+range);

		dist = conf.getInt(tSet+"."+name+".distance", 25);
		if(isDebug()) {
			log.info("["+modName+"][loadConf] Painting range distance is set to "+dist);
			if(!range) log.info("["+modName+"][loadConf] Painting range "+
						"distance is set even if it is not enabled");
		}

		//Load the default restriction configuration
		if(!loadGlobalRestrictions(tSet,conf))
			return false;

		List<Integer> intL = conf.getIntegerList(tSet+"."+name+".onlyAllow");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items, it overwrites the global");

			onlyAllow = loadMatList(intL,new HashSet<Material>(),tSet+"."+name+".onlyAllow");
			if(onlyAllow == null)
				return false;

			if(isDebug()) {
				logMatSet(onlyAllow,"loadGlobalRestrictions",name+".onlyAllow:");
				log.info( "["+modName+"][loadConf] As "+name+".onlyAllow has items, only those materials are usable");
			}
		} else if(isDebug()&& !onlyAllow.isEmpty()) {
			log.info( "["+modName+"][loadConf] As global.onlyAllow has items, only those materials are usable");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopCopy");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".stopCopy has items, it overwrites the global");

			stopCopy = loadMatList(intL,defStop(),tSet+"."+name+".stopCopy");
			if(stopCopy == null)
				return false;

			if(isDebug()) logMatSet(stopCopy,"loadConf",name+".stopCopy:");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopOverwrite");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+modName+"][loadConf] As "+name+".stopOverwrite has items, it overwrites the global");

			stopOverwrite = loadMatList(intL,defStop(),tSet+"."+name+".stopOverwrite");
			if(stopOverwrite == null)
				return false;

			if(isDebug()) logMatSet(stopOverwrite,"loadGlobalRestrictions",name+".stopOverwrite:");
		}
		return true;
	}

}
