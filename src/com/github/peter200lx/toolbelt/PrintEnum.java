package com.github.peter200lx.toolbelt;

import org.bukkit.command.CommandSender;

public enum PrintEnum {

	CMD(0),
	IMPORT(1),
	WARN(2),
	INFO(3),
	HINT(4),
	DEBUG(5);

	private PrintEnum(int priority) {
		this.priority = priority;
	}

	private int priority;

	public int getPri() {
		return priority;
	}

	public void print(PrintEnum pri, CommandSender subject, String message) {
		if(this.shouldPrint(pri)) {
			subject.sendMessage(message);
		}
	}

	public boolean shouldPrint(PrintEnum pri) {
		return (pri.getPri() <= this.priority);
	}

}
