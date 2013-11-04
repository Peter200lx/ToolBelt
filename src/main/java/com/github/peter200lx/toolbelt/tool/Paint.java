package com.github.peter200lx.toolbelt.tool;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.material.MaterialData;

import com.github.peter200lx.toolbelt.GlobalConf;
import com.github.peter200lx.toolbelt.PrintEnum;
import com.github.peter200lx.toolbelt.AbstractTool;

public class Paint extends AbstractTool {

	public Paint(GlobalConf gc) {
		super(gc);
	}

	public static final String NAME = "paint";

	/**
	 * Delay between how often a given user can acquire a paint. This is in
	 *     milliseconds, and is used to prevent multiple actions on a single
	 *     click.
	 */
	private int acquRepeatDelay = 300;

	/**
	 * Storage for how recently a given user has acquired a paint, used to
	 *     limit performing multiple actions for a single click.
	 */
	private final Map<String, Long> pAcquCooldown = new HashMap<String, Long>();

	private Integer rangeDef = 0;
	private Integer rangeCrouch = 25;

	private final Map<String, HashMap<Integer, MaterialData>> pPalette =
			new HashMap<String, HashMap<Integer, MaterialData>>();

	@Override
	public String getToolName() {
		return NAME;
	}

	@Override
	public void handleInteract(PlayerInteractEvent event) {
		final Player subject = event.getPlayer();
		if (!delayElapsed(subject.getName())) {
			return;
		}
		if (!pPalette.containsKey(subject.getName())) {
			pPalette.put(subject.getName(),
					new HashMap<Integer, MaterialData>());
		}

		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
		case LEFT_CLICK_AIR:
			// Acquire paint
			if (!acquireDelayElapsed(subject.getName())) {
				return;
			}
			MaterialData mdTarget = null;
			if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				mdTarget = event.getClickedBlock().getState().getData();

				if (subject.getGameMode().equals(GameMode.CREATIVE)
						&& (mdTarget.getItemType().equals(Material.SIGN_POST)
								|| mdTarget.getItemType().equals(
										Material.WALL_SIGN))) {
					uPrint(PrintEnum.WARN, subject, "The sign is not erased on"
									+ " the server, it is just client side");
				}
			} else {
				mdTarget = subject.getTargetBlock(null,
						200).getState().getData();
			}

			if (!noCopy(subject, mdTarget.getItemType())) {
				pPalette.get(subject.getName()).put(
						subject.getInventory().getHeldItemSlot(), mdTarget);
				uPrint(PrintEnum.IMPORT, subject,
						paintFormat("Paint is now ", mdTarget));
			} else {
				final MaterialData old = pPalette.get(subject.getName()).get(
						subject.getInventory().getHeldItemSlot());
				uPrint(PrintEnum.IMPORT, subject, ChatColor.RED
						+ "No paint aquired, "
						+ paintFormat("paint is still ", old));
			}
			break;
		case RIGHT_CLICK_BLOCK:
		case RIGHT_CLICK_AIR:
			// Draw paint
			final MaterialData set = pPalette.get(subject.getName()).get(
					subject.getInventory().getHeldItemSlot());
			if (set != null) {
				Block bTarget = null;
				if (hasRangePerm(subject)
						&& event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
					if ((rangeDef > 0) && !subject.isSneaking()) {
						bTarget = subject.getTargetBlock(null, rangeDef);
					} else if ((rangeCrouch > 0) && subject.isSneaking()) {
						bTarget = subject.getTargetBlock(null, rangeCrouch);
					}
				} else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
					bTarget = event.getClickedBlock();
				}

				if (bTarget != null
						&& !noOverwrite(subject, bTarget.getType())) {
					if ((bTarget.getType() == set.getItemType())
							&& (bTarget.getData() == set.getData())) {
						// Don't replace blocks with the same type and data
						return;
					}
					if (spawnBuild(bTarget, subject)) {
						if (isUseEvent()) {
							if (safeReplace(set, bTarget, subject, true)) {
								this.updateUser(subject, bTarget.getLocation(),
										set);
							}
						} else {
							bTarget.setTypeIdAndData(set.getItemTypeId(),
									set.getData(), false);
							this.updateUser(subject, bTarget.getLocation(),
									set);
						}
					}
				} else if (bTarget != null) {
					if (bTarget.getType().equals(Material.AIR)) {
						uPrint(PrintEnum.HINT, subject, ChatColor.RED
								+ "Target out of range");
					} else {
						uPrint(PrintEnum.WARN, subject, ChatColor.RED
								+ "You can't overwrite "
								+ ChatColor.GOLD + bTarget.getType());
					}
				}
			}
			break;
		default:
			return;
		}
	}

	private String paintFormat(String prefix, MaterialData m) {
		if (m == null) {
			return ChatColor.RED + prefix + ChatColor.GOLD + "empty";
		} else if (printData.contains(m.getItemType()) || (m.getData() != 0)) {
			return ChatColor.GREEN + prefix + ChatColor.GOLD
					+ m.getItemType().toString() + ChatColor.WHITE + ":"
					+ ChatColor.BLUE + data2Str(m);
		} else {
			return ChatColor.GREEN + prefix + ChatColor.GOLD
					+ m.getItemType().toString();
		}
	}

	private boolean hasRangePerm(CommandSender subject) {
		if (gc.perm) {
			return subject.hasPermission(getPermStr() + ".range");
		} else {
			return true;
		}
	}

	/**
	 * Check to determine if the user has waited longer then acquRepeatDelay
	 *     since the last use.
	 *
	 * @param userName user to check time difference for
	 * @return true if last check > then acquRepeatDelay, false otherwise
	 */
	private boolean acquireDelayElapsed(String userName) {
		if (acquRepeatDelay == 0) {
			return true;
		}
		if (pAcquCooldown.containsKey(userName) && (System.currentTimeMillis()
						< (pAcquCooldown.get(userName) + acquRepeatDelay))) {
			return false;
		}
		pAcquCooldown.put(userName, System.currentTimeMillis());
		return true;
	}

	@Override
	public void handleItemChange(PlayerItemHeldEvent event) {
		final Player subject = event.getPlayer();
		if (pPalette.containsKey(subject.getName())
				&& (pPalette.get(subject.getName()).size() > 1)) {
			final MaterialData slot = pPalette.get(
					subject.getName()).get(event.getNewSlot());
			uPrint(PrintEnum.IMPORT, subject,
					paintFormat("Paint in this slot is ", slot));
		}
	}

	@Override
	public boolean printUse(CommandSender sender) {
		if (hasPerm(sender)) {
			uPrint(PrintEnum.CMD, sender, useFormat(
					"Left click to load a block"));
			uPrint(PrintEnum.CMD, sender, useFormatExtra(
					"Right click to paint the loaded block"));
			if (hasRangePerm(sender)) {
				if (rangeDef > 0) {
					uPrint(PrintEnum.CMD, sender, useFormatExtra(
							"Be careful, you paint at a range of up to "
							+ rangeDef + " blocks."));
				}
				if (rangeCrouch > 0) {
					uPrint(PrintEnum.CMD, sender, useFormatExtra(
							"If you crouch, you paint at a range of "
							+ rangeCrouch + " blocks."));
				}
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

		acquRepeatDelay = conf.getInt(tSet + "." + getToolName()
				+ ".aquireRepeatDelay", 300);
		if (acquRepeatDelay < 0) {
			log.warning("[" + gc.modName + "] " + tSet + "." + getToolName()
					+ ".acquireRepeatDelay has an invalid value of "
					+ acquRepeatDelay);
			log.warning("[" + gc.modName + "] (The delay must be greater"
					+ " than or equal to zero)");
			return false;
		}
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Aquire Paint repeat"
					+ " delay is " + acquRepeatDelay);
		}

		rangeDef = conf.getInt(tSet + "." + NAME + ".rangeDefault", 0);
		rangeCrouch = conf.getInt(tSet + "." + NAME + ".rangeCrouch", 25);
		if (isDebug()) {
			log.info("[" + gc.modName + "][loadConf] Default painting range"
					+ " distance is set to " + rangeDef);
			log.info("[" + gc.modName + "][loadConf] Crouched painting range"
					+ " distance is set to " + rangeCrouch);
		}

		if (!loadOnlyAllow(tSet, conf)) {
			return false;
		}

		if (!loadStopCopy(tSet, conf)) {
			return false;
		}

		if (!loadStopOverwrite(tSet, conf)) {
			return false;
		}

		return true;
	}

}
