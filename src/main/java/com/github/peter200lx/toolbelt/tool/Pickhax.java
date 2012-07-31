package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

public class Pickhax extends AbstractTool {

	public Pickhax(GlobalConf gc) {
		super(gc);
	}

	public static final String NAME = "phax";

	private HashMap<String, Long> pWarned = new HashMap<String, Long>();

	private Integer range;

	@Override
	public String getToolName() {
		return NAME;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event) {
		Player subject = event.getPlayer();
		if (!delayElapsed(subject.getName())) {
			return;
		}
		Boolean physics = false;
		Block target;

		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			physics = true;
		case RIGHT_CLICK_BLOCK:
			target = event.getClickedBlock();
			break;
		case LEFT_CLICK_AIR:
			physics = true;
		case RIGHT_CLICK_AIR:
			if (subject.isSneaking() && hasRangePerm(subject) && (range > 0)) {
				target = subject.getTargetBlock(null, range);
			} else if (!warningElapsed(subject.getName())) {
				return;
			} else if (range <= 0) {
				uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
						+ "Ranged block removal isn't enabled");
				return;
			} else if (!hasRangePerm(subject)) {
				uPrint(PrintEnum.DEBUG, subject, ChatColor.DARK_PURPLE
						+ "You don't have ranged delete permission");
				return;
			} else {
				uPrint(PrintEnum.HINT, subject, ChatColor.RED + "Sorry, you"
						+ " clicked on air, try crouching for ranged removal");
				return;
			}
			break;
		default:
			return;
		}

		if (target != null && !noOverwrite(subject, target.getType())) {
			if (spawnBuild(target, subject)) {
				if (isUseEvent()) {
					if (safeBreak(target, subject, physics)) {
						this.updateUser(subject, target.getLocation(), 0,
								(byte) 0);
					}
				} else {
					target.setTypeId(0, physics);
					this.updateUser(subject, target.getLocation(), 0, (byte) 0);
				}
			}
		} else if ((target != null) && !target.getType().equals(Material.AIR)) {
			uPrint(PrintEnum.WARN, subject,
					ChatColor.RED + "You can't insta-delete "
							+ ChatColor.GOLD + target.getType());
		}
	}

	protected boolean warningElapsed(String playerName) {
		if (pWarned.containsKey(playerName) && (System.currentTimeMillis()
				< (pWarned.get(playerName) + 10 * 1000))) {
			return false;
		}
		pWarned.put(playerName, System.currentTimeMillis());
		return true;
	}

	private boolean hasRangePerm(CommandSender subject) {
		if (gc.perm) {
			return subject.hasPermission(getPermStr() + ".range");
		} else {
			return true;
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if (hasPerm(sender)) {
			uPrint(PrintEnum.CMD, sender, "Click with the "
					+ ChatColor.GOLD + getType() + ChatColor.WHITE
					+ " to delete a block (Right-click for no-physics)");
			return true;
		}
		return false;
	}

	@Override
	public boolean loadConf(String tSet, FileConfiguration conf) {

		// Load the repeat delay
		if (!loadRepeatDelay(tSet, conf, -1)) {
			return false;
		}

		range = conf.getInt(tSet + "." + NAME + ".range", 25);
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Crouched PickHax range"
					+ " distance is set to " + range);
		}

		if (!loadOnlyAllow(tSet, conf)) {
			return false;
		}

		if (!loadStopOverwrite(tSet, conf)) {
			return false;
		}

		return true;
	}
}
