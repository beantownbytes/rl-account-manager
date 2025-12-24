package com.accountmanager;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup(AccountManagerConfig.CONFIG_GROUP)
public interface AccountManagerConfig extends Config
{
	String CONFIG_GROUP = "accountmanager";

	@ConfigItem(
		keyName = "autoFillOtp",
		name = "Auto-fill OTP",
		description = "Automatically fill OTP when authenticator screen appears after selecting an account",
		position = 1
	)
	default boolean autoFillOtp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoLockMinutes",
		name = "Auto-lock after",
		description = "Automatically lock the vault after this many minutes (0 = never auto-lock)",
		position = 2
	)
	@Units(Units.MINUTES)
	default int autoLockMinutes()
	{
		return 0;
	}
}
