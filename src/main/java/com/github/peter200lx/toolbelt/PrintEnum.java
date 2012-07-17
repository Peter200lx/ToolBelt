package com.github.peter200lx.toolbelt;

/**
 * @author peter200lx
 *
 * Enumeration of allowed user message verbosity levels.
 */
public enum PrintEnum {

	/**
	 * These messages should only be in response to typed user commands.
	 */
	CMD(0, "cmd"),
	/**
	 * These messages are critical to the use of tools and should be shown.
	 */
	IMPORT(1, "important"),
	/**
	 * These messages show why a tool isn't working as expected.
	 */
	WARN(2, "warning"),
	/**
	 * These messages are helpful to display data about what was just done.
	 */
	INFO(3, "info"),
	/**
	 * These messages will give a user hints about how to use a tool.
	 */
	HINT(4, "hint"),
	/**
	 * These messages are available to help admins and programmers.
	 *     Not needed for regular users.
	 */
	DEBUG(5, "debug");

	/**
	 * Initialize the enum objects with int priority and name.
	 *
	 * @param priority used for ranking messages. 0-n prints for rank n
	 * @param permName user friendly name for print level. Used for permissions
	 */
	private PrintEnum(int priority, String permName) {
		this.priority = priority;
		this.permName = permName;
	}

	/**
	 * Used for ranking messages to user. All levels less then the current
	 *     verbosity will print for a given level. Priority info (3) will print
	 *     warning (2), important (1), and cmd (0).
	 */
	private int priority;
	/**
	 * User friendly name of the print enum level. This is used for
	 *     permissions when setting per-user/group print levels (such as
	 *     toolbelt.print.hint or toolbelt.print.warning)
	 */
	private String permName;

	/**
	 * Get the numerical priority level.
	 *
	 * @return level of this user print enum's priority.
	 */
	public int getPri() {
		return priority;
	}

	/**
	 * Get the user friendly name for priority level.
	 *
	 * @return name of this user print enum's priority.
	 */
	public String getPermName() {
		return permName;
	}

	/**
	 * Test if new priority should print based on current priority.
	 *
	 * @param pri priority of command to test
	 * @return true if passed in priority should print, false otherwise
	 */
	public boolean shouldPrint(PrintEnum pri) {
		return (pri.getPri() <= this.priority);
	}

}
