package com.accountmanager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
	name = "Account Manager",
	description = "Manage multiple OSRS accounts with encrypted storage",
	tags = {"account", "login", "manager", "2fa", "totp"}
)
public class AccountManagerPlugin extends Plugin
{
	private static final String CONFIG_KEY_SALT = "salt";
	private static final String CONFIG_KEY_ACCOUNTS = "accounts";
	private static final String CONFIG_KEY_VERIFICATION = "verification";
	private static final String VERIFICATION_STRING = "account-manager-verification";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private AccountManagerConfig config;

	@Getter
	private EncryptionService encryptionService;

	@Getter
	private TotpService totpService;

	@Getter
	private List<Account> accounts;

	private AccountManagerPanel panel;
	private NavigationButton navButton;

	@Getter
	private boolean unlocked = false;

	private Account lastSelectedAccount;
	private long unlockTime;
	private ScheduledExecutorService autoLockExecutor;


	@Override
	protected void startUp() throws Exception
	{
		totpService = new TotpService();
		accounts = new ArrayList<>();

		panel = injector.getInstance(AccountManagerPanel.class);
		panel.init(this);

		navButton = NavigationButton.builder()
			.tooltip("Account Manager")
			.icon(Icons.PANEL_ICON_IMG)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		// Start auto-lock checker
		autoLockExecutor = Executors.newSingleThreadScheduledExecutor();
		autoLockExecutor.scheduleAtFixedRate(this::checkAutoLock, 30, 30, TimeUnit.SECONDS);

		log.debug("Account Manager started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		lockVault();

		if (autoLockExecutor != null)
		{
			autoLockExecutor.shutdown();
			autoLockExecutor = null;
		}

		log.debug("Account Manager stopped");
	}

	public boolean hasExistingVault()
	{
		return configManager.getConfiguration(AccountManagerConfig.CONFIG_GROUP, CONFIG_KEY_SALT) != null;
	}

	public boolean unlock(String masterPassword)
	{
		String salt = configManager.getConfiguration(AccountManagerConfig.CONFIG_GROUP, CONFIG_KEY_SALT);

		if (salt == null)
		{
			// First time setup - generate new salt
			salt = EncryptionService.generateSalt();
			configManager.setConfiguration(AccountManagerConfig.CONFIG_GROUP, CONFIG_KEY_SALT, salt);
		}

		encryptionService = new EncryptionService(masterPassword, salt);

		// Check if we have existing data to verify password
		String verification = configManager.getConfiguration(AccountManagerConfig.CONFIG_GROUP, CONFIG_KEY_VERIFICATION);
		if (verification != null)
		{
			// Existing vault - verify password
			if (!encryptionService.verifyPassword(verification))
			{
				encryptionService = null;
				return false;
			}
		}
		else
		{
			// New vault - create verification string
			String encryptedVerification = encryptionService.encrypt(VERIFICATION_STRING);
			configManager.setConfiguration(AccountManagerConfig.CONFIG_GROUP, CONFIG_KEY_VERIFICATION, encryptedVerification);
		}

		// Load accounts
		loadAccounts();
		unlocked = true;
		unlockTime = System.currentTimeMillis();
		panel.rebuild();
		return true;
	}

	public void lockVault()
	{
		unlocked = false;
		accounts.clear();
		encryptionService = null;
		lastSelectedAccount = null;
		if (panel != null)
		{
			panel.rebuild();
		}
	}

	private void loadAccounts()
	{
		String accountsJson = configManager.getConfiguration(AccountManagerConfig.CONFIG_GROUP, CONFIG_KEY_ACCOUNTS);
		if (accountsJson == null || accountsJson.isEmpty())
		{
			accounts = new ArrayList<>();
			return;
		}

		try
		{
			Type listType = new TypeToken<ArrayList<Account>>(){}.getType();
			accounts = gson.fromJson(accountsJson, listType);
			if (accounts == null)
			{
				accounts = new ArrayList<>();
			}
		}
		catch (Exception e)
		{
			log.error("Failed to load accounts", e);
			accounts = new ArrayList<>();
		}
	}

	private void saveAccounts()
	{
		String accountsJson = gson.toJson(accounts);
		configManager.setConfiguration(AccountManagerConfig.CONFIG_GROUP, CONFIG_KEY_ACCOUNTS, accountsJson);
	}

	public void addAccount(Account account)
	{
		if (account.getId() == null)
		{
			account.setId(UUID.randomUUID().toString());
		}
		accounts.add(account);
		saveAccounts();
	}

	public void updateAccount(Account account)
	{
		for (int i = 0; i < accounts.size(); i++)
		{
			if (accounts.get(i).getId().equals(account.getId()))
			{
				accounts.set(i, account);
				break;
			}
		}
		saveAccounts();
	}

	public void deleteAccount(Account account)
	{
		accounts.removeIf(a -> a.getId().equals(account.getId()));
		saveAccounts();
	}

	public void fillCredentials(Account account)
	{
		lastSelectedAccount = account;

		clientThread.invoke(() ->
		{
			GameState gameState = client.getGameState();
			int loginIndex = client.getLoginIndex();

			if (gameState == GameState.LOGIN_SCREEN && loginIndex == 2)
			{
				// At username/password form
				fillLoginForm(account);
			}
			else if (gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR && loginIndex == 4)
			{
				// At authenticator form
				fillOtp(account);
			}
		});
	}

	private void fillLoginForm(Account account)
	{
		String username = encryptionService.decrypt(account.getEncryptedUsername());
		String password = encryptionService.decrypt(account.getEncryptedPassword());
		client.setUsername(username);
		client.setPassword(password);
		log.debug("Filled credentials for account: {}", account.getNickname());
	}

	private void fillOtp(Account account)
	{
		if (account.hasTotpSecret())
		{
			String secret = encryptionService.decrypt(account.getEncryptedTotpSecret());
			String code = totpService.generateCode(secret);
			client.setOtp(code);
			log.debug("Filled OTP for account: {}", account.getNickname());
		}
	}


	private void checkAutoLock()
	{
		if (!unlocked || config.autoLockMinutes() <= 0)
		{
			return;
		}

		long now = System.currentTimeMillis();
		long elapsedMinutes = (now - unlockTime) / 60000;
		if (elapsedMinutes >= config.autoLockMinutes())
		{
			log.debug("Auto-locking vault after {} minutes", elapsedMinutes);
			// Use SwingUtilities to ensure we're on the EDT for UI updates
			javax.swing.SwingUtilities.invokeLater(this::lockVault);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (!unlocked || lastSelectedAccount == null)
		{
			return;
		}

		// Auto-fill OTP when authenticator screen appears
		if (event.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR && config.autoFillOtp())
		{
			clientThread.invokeLater(() ->
			{
				if (client.getLoginIndex() == 4 && lastSelectedAccount.hasTotpSecret())
				{
					fillOtp(lastSelectedAccount);
				}
			});
		}

		// Clear last selected account after successful login
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			lastSelectedAccount = null;
		}
	}

	@Provides
	AccountManagerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AccountManagerConfig.class);
	}
}
