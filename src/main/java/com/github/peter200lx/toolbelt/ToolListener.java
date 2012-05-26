package com.github.peter200lx.toolbelt;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
		if(master.tbDisabled.contains(event.getPlayer().getName()))
			return;
		//Call tool listing
		for(ToolInterface tool:master.tools) {
			if(!event.getAction().equals(Action.PHYSICAL) && tool.hasPerm(event.getPlayer()) &&
					tool.getType().equals(event.getPlayer().getItemInHand().getType())       ){
				event.setCancelled(true);
				tool.handleInteract(event);
			}
		}
	}

	@EventHandler
	public void catchItemChange(PlayerItemHeldEvent event) {
		if(master.tbDisabled.contains(event.getPlayer().getName()))
			return;
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
			if(master.tbDisabled.contains(((Player)event.getEntity()).getName()))
				return;
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
