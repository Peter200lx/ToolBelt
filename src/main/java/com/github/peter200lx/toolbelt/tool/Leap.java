package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

public class Leap extends AbstractTool {

	public Leap(GlobalConf gc) {
		super(gc);
	}

	public static final String NAME = "leap";

	private boolean leapTeleport;

	private boolean leapFly;

	private int leapThrust;

	private int leapCruise;

	private double leapInvuln;

	private int leapCost;

	private final Set<String> pLept = new HashSet<String>();

	private final Set<String> pFlight = new HashSet<String>();

	private final Map<String, Integer> pFlap = new HashMap<String, Integer>();

	@Override
	public String getToolName() {
		return NAME;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event) {
		final Action act = event.getAction();
		final Player subject = event.getPlayer();
		final String name = subject.getName();
		if (!delayElapsed(name)) {
			return;
		}
		if (act.equals(Action.RIGHT_CLICK_AIR)
				|| act.equals(Action.RIGHT_CLICK_BLOCK)) {
			// This code block is copied from VoxelAir from FlyRidgeFly
			// Further modifications by peter200lx
			double cProt = subject.getLocation().getYaw() % 360.0F;
			if (cProt > 0.0D) {
				cProt -= 720.0D;
			}
			final double pRot = Math.abs(cProt % 360.0D);
			double pX = 0.0D;
			double pZ = 0.0D;
			double pY = 0.0D;
			final double pPit = subject.getLocation().getPitch();
			double pyY = 0.0D;
			if ((pPit < 21.0D) && (pPit > -21.0D)) {
				pX = Math.sin(Math.toRadians(pRot)) * 10.0D;
				pZ = Math.cos(Math.toRadians(pRot)) * 10.0D;
				if (subject.getLocation().getY() < leapCruise) {
					pY = 2.5D;
				} else if (subject.getLocation().getY() <= leapCruise + 5) {
					pY = 1.0D;
				} else {
					pY = 0.0D;
				}
			} else {
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
			if (subject.isSneaking() && leapTeleport
					&& (hasTeleportPerm(subject))) {
				subject.teleport(new Location(subject.getWorld(),
						subject.getLocation().getX() + pX,
						subject.getLocation().getY() + pY,
						subject.getLocation().getZ() + pZ,
						subject.getLocation().getYaw(),
						subject.getLocation().getPitch()));
			} else {
				subject.setVelocity(new Vector(pX, pY / 2.5D, pZ));
			}
			if (leapCost > 0 && !hasFreePerm(subject)) {
				int cost = 1;
				if (pFlap.containsKey(name)) {
					cost = pFlap.get(name);
				}
				if (cost < leapCost) {
					pFlap.put(name, cost + 1);
				} else {
					ItemStack stack = subject.getItemInHand();
					if (stack.getAmount() > 1) {
						stack.setAmount(stack.getAmount() - 1);
					} else {
						subject.getInventory().clear(
								subject.getInventory().getHeldItemSlot());
					}
					uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
							+ "One " + ChatColor.GOLD + this.getType()
							+ ChatColor.DARK_PURPLE
							+ " has been removed from your inventory.");
					pFlap.remove(name);
				}
			}
			if (leapInvuln > 0) {
				pLept.add(name);
			}
		} else if ((act.equals(Action.LEFT_CLICK_AIR)
				|| act.equals(Action.LEFT_CLICK_BLOCK))
				&& leapFly && hasCFlyPerm(subject)) {
			if (subject.isFlying()) {
				subject.setFlying(false);
				if (leapInvuln > 0) {
					pLept.add(name);
				}
				if (pFlight.contains(name)) {
					subject.setAllowFlight(false);
					uPrint(PrintEnum.INFO, subject,
							"Creative mode flying disabled");
					pFlight.remove(name);
				}
			} else {
				if (!subject.getAllowFlight()) {
					pFlight.add(name);
					subject.setAllowFlight(true);
					uPrint(PrintEnum.INFO, subject,
							"Creative mode flying enabled");
				}
				subject.setFlying(true);
			}
		}
	}

	private boolean hasTeleportPerm(CommandSender subject) {
		if (gc.perm) {
			return subject.hasPermission(getPermStr() + ".tel");
		} else {
			return true;
		}
	}

	private boolean hasCFlyPerm(CommandSender subject) {
		if (gc.perm) {
			return subject.hasPermission(getPermStr() + ".fly");
		} else {
			return true;
		}
	}

	private boolean hasFreePerm(CommandSender subject) {
		if (gc.perm) {
			return subject.hasPermission(getPermStr() + ".free");
		} else {
			// false so that cost can apply when permissions are disabled.
			return false;
		}
	}

	@Override
	public void handleDamage(EntityDamageEvent event) {
		if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
			// Safe to cast to Player because catchDamage() in ToolListener has
			// already verified that the entity in question is a Player
			if ((leapInvuln < 0)
					|| pLept.contains(((Player) event.getEntity()).getName())) {
				event.setCancelled(true);
				pLept.remove(((Player) event.getEntity()).getName());
			}
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if (hasPerm(sender)) {
			uPrint(PrintEnum.CMD, sender, "Right-Click with the "
					+ ChatColor.GOLD + getType() + ChatColor.WHITE
					+ " to make magnificent leaps");
			if (hasTeleportPerm(sender) && leapTeleport) {
				uPrint(PrintEnum.CMD, sender,
						"Crouch while leaping to teleport");
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, ConfigurationSection conf) {

		// Load the repeat delay
		if (!loadRepeatDelay(tSet, conf, 0)) {
			return false;
		}

		leapTeleport = conf.getBoolean(tSet + "." + NAME + ".teleport", false);
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Teleport leaping"
					+ " is set to " + leapTeleport);
		}
		leapFly = conf.getBoolean(tSet + "." + NAME + ".fly", true);
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Creative flying"
					+ " is set to " + leapFly);
		}
		leapThrust = conf.getInt(tSet + "." + NAME + ".thrust", 8);
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Flap thrust"
					+ " is set to " + leapThrust);
		}
		leapCruise = conf.getInt(tSet + "." + NAME + ".cruise", 110);
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Cruising altitude"
					+ " is set to " + leapCruise);
		}
		leapInvuln = conf.getDouble(tSet + "." + NAME + ".invuln", -1);
		if (isDebug()) {
			if (leapInvuln < 0) {
				log.info("[" + gc.modName + "][loadConf] Fall damage"
						+ " is disabled");
			} else if (leapInvuln == 0) {
				log.info("[" + gc.modName + "][loadConf]"
						+ " Fall damage not disabled at all by leap tool");
			} else {
				log.info("[" + gc.modName + "][loadConf]"
						+ " Next fall damage after leaping is disabled");
			}
		}
		leapCost = conf.getInt(tSet + "." + NAME + ".cost", 0);
		if (isDebug()) {
			if (leapCost == 0) {
				log.info("[" + gc.modName
						+ "][loadConf] No cost for using the leap tool");
			} else {
				log.info("[" + gc.modName + "][loadConf] Leaping will remove "
						+ "one " + this.getType() + " every " + leapCost
						+ " uses");
			}
		}
		return true;
	}

}
