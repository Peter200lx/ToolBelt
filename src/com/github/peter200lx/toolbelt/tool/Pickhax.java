package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.Tool;

public class Pickhax extends Tool  {

	public Pickhax(GlobalConf gc) {
		super(gc);
	}

	public static String name = "phax";

	private HashMap<String, Long> pWarned = new HashMap<String, Long>();

	private Integer range;

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Player subject = event.getPlayer();
		if(!delayElapsed(subject.getName()))
			return;
		Boolean physics = false;
		Block target;
		switch(event.getAction()) {
		case LEFT_CLICK_BLOCK:
			physics = true;
		case RIGHT_CLICK_BLOCK:
			target = event.getClickedBlock();
			break;
		case LEFT_CLICK_AIR:
			physics = true;
		case RIGHT_CLICK_AIR:
			if(subject.isSneaking()&&hasRangePerm(subject)&&(range > 0))
				target = subject.getTargetBlock(null, range);
			else if(!warningElapsed(subject.getName()))
				return;
			else if(range <= 0){
				subject.sendMessage(ChatColor.RED+"Ranged block removal isn't enabled");
				return;
			}else if(!hasRangePerm(subject)) {
				subject.sendMessage(ChatColor.RED+"You don't have ranged delete permission");
				return;
			}else {
				subject.sendMessage(ChatColor.RED+"Sorry, you clicked on air,"+
						" try crouching for ranged removal");
				return;
			}
			break;
		default:
			return;
		}
		if(target != null && !stopOverwrite.contains(target.getType())       &&
				(onlyAllow.isEmpty() || onlyAllow.contains(target.getType())) ){
			if(spawnBuild(target,event.getPlayer())) {
				if(isUseEvent()) {
					if(safeBreak(target,event.getPlayer(),physics))
						subject.sendBlockChange(target.getLocation(), 0, (byte)0);
				}else {
					target.setTypeId(0,physics);
					subject.sendBlockChange(target.getLocation(), 0, (byte)0);
				}
			}
		}else if((target != null)&&!target.getType().equals(Material.AIR)) {
			event.getPlayer().sendMessage(ChatColor.RED + "You can't insta-delete "+
					ChatColor.GOLD+target.getType());
		}
	}

	protected boolean warningElapsed(String name) {
		if(pWarned.containsKey(name) &&
				(System.currentTimeMillis() < (pWarned.get(name)+10*1000)))
			return false;
		pWarned.put(name, System.currentTimeMillis());
		return true;
	}

	private boolean hasRangePerm(CommandSender subject) {
		if(gc.perm)
			return subject.hasPermission(getPermStr()+".range");
		else
			return true;
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to delete a block (Right-click for no-physics)");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the repeat delay
		if(!loadRepeatDelay(tSet,conf,-1))
			return false;

		range = conf.getInt(tSet+"."+name+".range", 25);
		if(isDebug())
			log.info("["+gc.modName+"][loadConf] Crouched PickHax range distance is set to "+range);

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
