package com.github.peter200lx.toolbelt;

public enum PrintEnum {

	CMD(0,"cmd"),
	IMPORT(1,"important"),
	WARN(2,"warning"),
	INFO(3,"info"),
	HINT(4,"hint"),
	DEBUG(5,"debug");

	private PrintEnum(int priority, String permName) {
		this.priority = priority;
		this.permName = permName;
	}

	private int priority;
	private String permName;

	public int getPri() {
		return priority;
	}

	public String getPermName() {
		return permName;
	}

	public boolean shouldPrint(PrintEnum pri) {
		return (pri.getPri() <= this.priority);
	}

}
