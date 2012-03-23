package com.github.peter200lx.toolbelt;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

public class ToolListener implements Listener {

	ToolBelt master;

	public ToolListener(ToolBelt caller) {
		super();
		master = caller;
	}

	@EventHandler
	public void catchInteract(PlayerInteractEvent event) {
		//Call tool listing
		for(ToolInterface tool:master.tools) {
			if(tool.getType().equals(event.getPlayer().getItemInHand().getType()) &&
					tool.hasPerm(event.getPlayer())) {
				event.setCancelled(true);
				tool.handleInteract(event);
			}
		}
	}

	@EventHandler
	public void catchItemChange(PlayerItemHeldEvent event) {
		ItemStack cur = event.getPlayer().getInventory().getItem(event.getNewSlot());
		if(cur != null) {
			for(ToolInterface tool:master.tools) {
				if(tool.getType().equals(cur.getType()) &&
						tool.hasPerm(event.getPlayer())) {
					tool.handleItemChange(event);
				}
			}
		}
	}

	@EventHandler
	public void catchDamage(EntityDamageEvent event) {
		if(event.getEntity() instanceof Player) {
			for(ToolInterface tool:master.tools) {
				if(tool.getType().equals(((Player)event.getEntity()
										 ).getItemInHand().getType()) &&
						tool.hasPerm((Player)event.getEntity())) {
					tool.handleDamage(event);
				}
			}
		}
	}

}
