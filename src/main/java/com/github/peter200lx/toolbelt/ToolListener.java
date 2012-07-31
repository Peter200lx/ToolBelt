package com.github.peter200lx.toolbelt;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author peter200lx
 *
 * Event Listener class for hooking into gameplay.
 */
public class ToolListener implements Listener {

	/**
	 * Reference to host bukkit plugin. This is used to access variables and
	 *     functions from the root plugin class.
	 */
	private final ToolBelt master;

	/**
	 * Initialize ToolListener with host plugin reference.
	 *
	 * @param caller reference to host bukkit plugin
	 */
	public ToolListener(ToolBelt caller) {
		super();
		master = caller;
	}

	/**
	 * Catch and pass valid mouse clicks into available tools. Valid means that
	 *     the user clicked with a ToolBelt tool in hand and that the player
	 *     has permission to use that tool. Also we know it was a mouse click
	 *     because we block PHYSICAL actions (stepping on a pressure plate).
	 *
	 * When the event is valid for a ToolBelt tool to handle, the event is
	 *     canceled so that we don't have interfering behavior.
	 *
	 * @param event event when an Action is performed by a user
	 */
	@EventHandler
	public void catchInteract(PlayerInteractEvent event) {
		if (master.getTbDisabled().contains(event.getPlayer().getName())) {
			return;
		}
		//Call tool listing
		for (ToolInterface tool:master.getTools()) {
			if (!event.getAction().equals(Action.PHYSICAL)
					&& tool.hasPerm(event.getPlayer())
					&& tool.getType().equals(
							event.getPlayer().getItemInHand().getType())) {
				event.setCancelled(true);
				tool.handleInteract(event);
			}
		}
	}

	/**
	 * Catch and pass valid item bar scrolls to available tools. Valid means
	 *     the user scrolled onto (or picked up into hand) a ToolBelt tool and
	 *     has permission to use the tool now selected.
	 *
	 * @param event event when user picks up or scrolls item in item bar
	 */
	@EventHandler
	public void catchItemChange(PlayerItemHeldEvent event) {
		if (master.getTbDisabled().contains(event.getPlayer().getName())) {
			return;
		}
		ItemStack cur = event.getPlayer().getInventory().getItem(
				event.getNewSlot());
		if (cur != null) {
			for (ToolInterface tool:master.getTools()) {
				if (tool.getType().equals(cur.getType())
						&& tool.hasPerm(event.getPlayer())) {
					tool.handleItemChange(event);
				}
			}
		}
	}

	/**
	 * Catch and pass valid player damage to available tools. Valid means that
	 *     the entity in question is a Player, with a ToolBelt tool in hand,
	 *     and has permission to use that tool.
	 *
	 * @param event event when an entity takes damage
	 */
	@EventHandler
	public void catchDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			if (master.getTbDisabled().contains(
					((Player) event.getEntity()).getName())) {
				return;
			}
			for (ToolInterface tool:master.getTools()) {
				if (tool.getType().equals(((Player) event.getEntity()
										 ).getItemInHand().getType())
						&& tool.hasPerm((Player) event.getEntity())) {
					tool.handleDamage(event);
				}
			}
		}
	}

}
