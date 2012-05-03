package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.material.MaterialData;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.Tool;

public class Paint extends Tool  {

	public Paint(GlobalConf gc) {
		super(gc);
	}

	public static String name = "paint";

	private Integer rangeDef = 0;
	private Integer rangeCrouch = 25;

	private HashMap<String, HashMap<Integer, MaterialData>> pPalette =
			new HashMap<String, HashMap<Integer, MaterialData>>();

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		if(!delayElapsed(subject.getName()))
			return;
		if(!pPalette.containsKey(subject.getName())) {
			pPalette.put(subject.getName(), new HashMap<Integer,MaterialData>());
		}

		switch(event.getAction()) {
		case LEFT_CLICK_BLOCK:
		case LEFT_CLICK_AIR:
			//Acquire paint
			MaterialData mdTarget = null;
			if(event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				mdTarget = event.getClickedBlock().getState().getData();
				if(subject.getGameMode().equals(GameMode.CREATIVE)      &&(
						mdTarget.getItemType().equals(Material.SIGN_POST) ||
						mdTarget.getItemType().equals(Material.WALL_SIGN))){
					subject.sendMessage("The sign is not erased on the server, "+
								"it is just client side");
				}
			}else
				mdTarget = subject.getTargetBlock(null, 200).getState().getData();
			if(!stopCopy.contains(mdTarget.getItemType()) && ( onlyAllow.isEmpty() ||
					onlyAllow.contains(mdTarget.getItemType()) ) ){
				pPalette.get(subject.getName()).put(
						subject.getInventory().getHeldItemSlot(), mdTarget );
				paintPrint("Paint is now ",subject,mdTarget);
			} else {
				subject.sendMessage(ChatColor.RED + "Was not able to grab a block to paint.");
				MaterialData old = pPalette.get(subject.getName()).get(
						subject.getInventory().getHeldItemSlot());
				paintPrint("Paint is still ",subject,old);
			}
			break;
		case RIGHT_CLICK_BLOCK:
		case RIGHT_CLICK_AIR:
			//Draw paint
			MaterialData set = pPalette.get(subject.getName()).get(
					subject.getInventory().getHeldItemSlot());
			if(set != null) {
				Block bTarget = null;
				if(hasRangePerm(subject) && event.getAction().equals(Action.RIGHT_CLICK_AIR) ){
					if((rangeDef > 0) && !subject.isSneaking())
						bTarget = subject.getTargetBlock(null, rangeDef);
					else if((rangeCrouch > 0)&& subject.isSneaking())
						bTarget = subject.getTargetBlock(null, rangeCrouch);
				}else if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
					bTarget = event.getClickedBlock();
				if(bTarget != null && !stopOverwrite.contains(bTarget.getType())       &&
						(onlyAllow.isEmpty() || onlyAllow.contains(bTarget.getType())) ){
					if((bTarget.getType()==set.getItemType())&&(bTarget.getData()==set.getData())) {
						//Don't replace blocks with the same type and data
						return;
					}
					if(spawnBuild(bTarget,subject)) {
						if(isUseEvent()) {
							if(safeReplace(set,bTarget,subject,true)) {
								subject.sendBlockChange(bTarget.getLocation(), set.getItemType(),set.getData());
							}
						}else {
							bTarget.setTypeIdAndData(set.getItemTypeId(), set.getData(), false);
							subject.sendBlockChange(bTarget.getLocation(), set.getItemType(), set.getData());
						}
					}
				}else if(bTarget != null) {
					if(bTarget.getType().equals(Material.AIR))
						subject.sendMessage(ChatColor.RED + "Target is out of range");
					else
						subject.sendMessage(ChatColor.RED + "You can't overwrite "+
								ChatColor.GOLD+bTarget.getType());
				}
			}
			break;
		default:
			return;
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
		if(gc.perm)
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
			sender.sendMessage("Left-click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to load a block");
			sender.sendMessage("Right-click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to paint the loaded block");
			if(hasRangePerm(sender)) {
				if(rangeDef > 0)
					sender.sendMessage("Be careful, you can paint at a range of up to "+
						rangeDef+" blocks.");
				if(rangeCrouch > 0)
					sender.sendMessage("If you crouch, you can paint at a range of "+
							rangeCrouch);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the repeat delay
		if(!loadRepeatDelay(tSet,conf,0))
			return false;

		rangeDef = conf.getInt(tSet+"."+name+".rangeDefault", 0);
		rangeCrouch = conf.getInt(tSet+"."+name+".rangeCrouch", 25);
		if(isDebug()) {
			log.info("["+gc.modName+"][loadConf] Default painting range distance is set to "+
					rangeDef);
			log.info("["+gc.modName+"][loadConf] Crouched painting range distance is set to "+
					rangeCrouch);
		}

		List<Integer> intL = conf.getIntegerList(tSet+"."+name+".onlyAllow");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+gc.modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" it overwrites the global");

			if(!onlyAllow.loadMatList(intL,false,tSet+"."+name+".onlyAllow"))
				return false;

			if(isDebug()) {
				onlyAllow.logMatSet("loadConf",name+".onlyAllow:");
				log.info( "["+gc.modName+"][loadConf] As "+name+".onlyAllow has items,"+
						" only those materials are usable");
			}
		} else if(isDebug()&& !onlyAllow.isEmpty()) {
			log.info( "["+gc.modName+"][loadConf] As global.onlyAllow has items,"+
					" only those materials are usable");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopCopy");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+gc.modName+"][loadConf] As "+name+".stopCopy has items,"+
						" it overwrites the global");

			if(!stopCopy.loadMatList(intL,true,tSet+"."+name+".stopCopy"))
				return false;

			if(isDebug()) stopCopy.logMatSet("loadConf",name+".stopCopy:");
		}

		intL = conf.getIntegerList(tSet+"."+name+".stopOverwrite");

		if(!intL.isEmpty())
		{
			if(isDebug())
				log.info( "["+gc.modName+"][loadConf] As "+name+".stopOverwrite has items,"+
						" it overwrites the global");

			if(!stopOverwrite.loadMatList(intL,true,tSet+"."+name+".stopOverwrite"))
				return false;

			if(isDebug()) stopOverwrite.logMatSet("loadConf",
					name+".stopOverwrite:");
		}
		return true;
	}

}
