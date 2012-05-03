package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.Tool;

public class Leap extends Tool {

	public Leap(GlobalConf gc) {
		super(gc);
	}

	public static String name = "leap";

	private boolean leapTeleport;

	private int leapThrust;

	private int leapCruise;

	private double leapInvuln;

	private HashMap<String, Long> pInvuln = new HashMap<String, Long>();

	@Override
	public String getToolName() {
		return name;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event){
		Action act = event.getAction();
		Player subject = event.getPlayer();
		if(!delayElapsed(subject.getName()))
			return;
		if(isDebug()) log.info("["+gc.modName+"][leapTool] "+subject.getName()+
				" " + act.toString()+" with the leap tool");
		if(act.equals(Action.RIGHT_CLICK_AIR)||act.equals(Action.RIGHT_CLICK_BLOCK)) {
			//This code block is copied from VoxelAir from FlyRidgeFly
			// Further modifications by peter200lx
			double cProt = subject.getLocation().getYaw() % 360.0F;
			if (cProt > 0.0D) {
				cProt -= 720.0D;
			}
			double pRot = Math.abs(cProt % 360.0D);
			double pX = 0.0D;
			double pZ = 0.0D;
			double pY = 0.0D;
			double pPit = subject.getLocation().getPitch();
			double pyY = 0.0D;
			if ((pPit < 21.0D) && (pPit > -21.0D)) {
				pX = Math.sin(Math.toRadians(pRot)) * 10.0D;
				pZ = Math.cos(Math.toRadians(pRot)) * 10.0D;
				if (subject.getLocation().getY() < leapCruise)
					pY = 2.5D;
				else if (subject.getLocation().getY() <= leapCruise + 5)
					pY = 1.0D;
				else
					pY = 0.0D;
			}
			else {
				if (pPit < 0.0D) {
					pY = Math.sin(Math.toRadians(Math.abs(pPit))) * 10.0D;
					pyY = Math.cos(Math.toRadians(Math.abs(pPit))) * 10.0D;
					pX = Math.sin(Math.toRadians(pRot)) * pyY;
					pZ = Math.cos(Math.toRadians(pRot)) * pyY;
				} else if (pPit < 30.0D) {
					pY = 4.0D;
					pX = Math.sin(Math.toRadians(pRot)) * 6.0D;
					pZ = Math.cos(Math.toRadians(pRot)) * 6.0D;
				} else if (pPit < 60.0D) {
					pY = 5.0D;
					pX = Math.sin(Math.toRadians(pRot)) * 3.0D;
					pZ = Math.cos(Math.toRadians(pRot)) * 3.0D;
				} else if (pPit < 75.0D) {
					pY = 6.0D;
					pX = Math.sin(Math.toRadians(pRot)) * 1.5D;
					pZ = Math.cos(Math.toRadians(pRot)) * 1.5D;
				} else {
					pY = leapThrust;
					pX = 0.0D;
					pZ = 0.0D;
				}
			}
			if (subject.isSneaking() && leapTeleport && (hasTeleportPerm(subject)))
				subject.teleport(new Location(subject.getWorld(),
						subject.getLocation().getX() + pX, subject.getLocation().getY() + pY,
						subject.getLocation().getZ() + pZ,
						subject.getLocation().getYaw(), subject.getLocation().getPitch()));
			else
				subject.setVelocity(new Vector(pX, pY / 2.5D, pZ));
			if(leapInvuln > 0)
				pInvuln.put(name, System.currentTimeMillis());
		}
	}

	private boolean hasTeleportPerm (CommandSender subject) {
		if(gc.perm)
			return subject.hasPermission(getPermStr()+".tel");
		else
			return true;
	}

	@Override
	public void handleDamage(EntityDamageEvent event) {
		if(event.getCause().equals(EntityDamageEvent.DamageCause.FALL)){
			if((leapInvuln < 0) || pInvuln.containsKey(name) &&
					(System.currentTimeMillis() <= (pInvuln.get(name)+leapInvuln*1000)))
				event.setCancelled(true);
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if(hasPerm(sender)) {
			sender.sendMessage("Right-Click with the "+ChatColor.GOLD+getType()+
					ChatColor.WHITE+" to make magnificent leaps");
			if(hasTeleportPerm(sender)&&leapTeleport)
				sender.sendMessage("Crouch while leaping to teleport");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		//Load the repeat delay
		if(!loadRepeatDelay(tSet,conf,0))
			return false;

		leapTeleport = conf.getBoolean("tools.leap.teleport", false);
		if(isDebug())
			log.info("["+gc.modName+"][loadConf] Teleport leaping is set to "+leapTeleport);
		leapThrust = conf.getInt("tools.leap.thrust", 8);
		if(isDebug())
			log.info("["+gc.modName+"][loadConf] Flap thrust is set to "+leapThrust);
		leapCruise = conf.getInt("tools.leap.cruise", 110);
		if(isDebug())
			log.info("["+gc.modName+"][loadConf] Cruising altitude is set to "+leapCruise);
		leapInvuln = conf.getDouble("tools.leap.invuln", -1);
		if(isDebug()) {
			if(leapInvuln < 0)
				log.info("["+gc.modName+"][loadConf] Fall damage is disabled");
			else
				log.info("["+gc.modName+"][loadConf] Fall damage is disabled for "+
						leapInvuln+" seconds after using leap tool");
		}
		return true;
	}

}
