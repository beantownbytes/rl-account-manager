package com.accountmanager;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

@Slf4j
class AccountManagerPanel extends PluginPanel
{
	private final JLabel addAccount = new JLabel(Icons.ADD_ICON);
	private final JLabel title = new JLabel("Account Manager");
	private final JPanel accountListPanel = new JPanel();
	private final JPanel lockedPanel = new JPanel();
	private final JPanel unlockedPanel = new JPanel();

	private AccountManagerPlugin plugin;

	@Inject
	AccountManagerPanel()
	{
		super(false);
	}

	void init(AccountManagerPlugin plugin)
	{
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		buildLockedPanel();
		buildUnlockedPanel();
		rebuild();
	}

	private void buildLockedPanel()
	{
		lockedPanel.setLayout(new BorderLayout());
		lockedPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		lockedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
	}

	private void rebuildLockedPanel()
	{
		lockedPanel.removeAll();

		boolean isFirstTime = !plugin.hasExistingVault();

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Lock icon
		JLabel lockLabel = new JLabel(Icons.LOCK_ICON);
		lockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		// Spacing
		centerPanel.add(Box.createVerticalGlue());
		centerPanel.add(lockLabel);
		centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

		// Title - different for first time vs returning
		JLabel titleLabel = new JLabel(isFirstTime ? "First Time Setup" : "Vault Locked");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		centerPanel.add(titleLabel);
		centerPanel.add(Box.createRigidArea(new Dimension(0, 5)));

		// Prompt
		JLabel promptLabel = new JLabel(isFirstTime ? "Create a master password" : "Enter master password");
		promptLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		centerPanel.add(promptLabel);
		centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

		// Password field with label
		JPanel passwordPanel = new JPanel();
		passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.Y_AXIS));
		passwordPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		passwordPanel.setMaximumSize(new Dimension(200, 100));

		JLabel passwordLabel = new JLabel(isFirstTime ? "Password:" : "Password:");
		passwordLabel.setForeground(Color.WHITE);
		passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		passwordPanel.add(passwordLabel);
		passwordPanel.add(Box.createRigidArea(new Dimension(0, 3)));

		JPasswordField passwordField = new JPasswordField();
		passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
		passwordPanel.add(passwordField);

		// Confirm field - only for first time setup
		JPasswordField confirmField = new JPasswordField();
		if (isFirstTime)
		{
			passwordPanel.add(Box.createRigidArea(new Dimension(0, 10)));

			JLabel confirmLabel = new JLabel("Confirm password:");
			confirmLabel.setForeground(Color.WHITE);
			confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			passwordPanel.add(confirmLabel);
			passwordPanel.add(Box.createRigidArea(new Dimension(0, 3)));

			confirmField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
			confirmField.setAlignmentX(Component.LEFT_ALIGNMENT);
			passwordPanel.add(confirmField);
		}

		centerPanel.add(passwordPanel);
		centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

		// Button - different text for first time
		JButton actionButton = new JButton(isFirstTime ? "Create Vault" : "Unlock");
		actionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		centerPanel.add(actionButton);
		centerPanel.add(Box.createVerticalGlue());

		// Action handlers
		Runnable unlockAction = () ->
		{
			String password = new String(passwordField.getPassword());
			if (password.isEmpty())
			{
				JOptionPane.showMessageDialog(this, "Please enter a password",
					"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if (isFirstTime)
			{
				String confirm = new String(confirmField.getPassword());
				if (!password.equals(confirm))
				{
					JOptionPane.showMessageDialog(this, "Passwords do not match",
						"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (password.length() < 4)
				{
					JOptionPane.showMessageDialog(this, "Password must be at least 4 characters",
						"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			if (!plugin.unlock(password))
			{
				JOptionPane.showMessageDialog(this, "Incorrect password",
					"Error", JOptionPane.ERROR_MESSAGE);
			}
			passwordField.setText("");
			confirmField.setText("");
		};

		actionButton.addActionListener(e -> unlockAction.run());

		KeyAdapter enterKeyListener = new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					unlockAction.run();
				}
			}
		};
		passwordField.addKeyListener(enterKeyListener);
		confirmField.addKeyListener(enterKeyListener);

		lockedPanel.add(centerPanel, BorderLayout.CENTER);
		lockedPanel.revalidate();
		lockedPanel.repaint();
	}

	private void buildUnlockedPanel()
	{
		unlockedPanel.setLayout(new BorderLayout());
		unlockedPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		unlockedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		// Header
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(0, 0, 10, 0));

		title.setForeground(Color.WHITE);
		header.add(title, BorderLayout.WEST);

		addAccount.setToolTipText("Add account");
		addAccount.setCursor(new Cursor(Cursor.HAND_CURSOR));
		addAccount.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				openAddAccountDialog(null);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				addAccount.setIcon(Icons.ADD_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				addAccount.setIcon(Icons.ADD_ICON);
			}
		});
		header.add(addAccount, BorderLayout.EAST);

		unlockedPanel.add(header, BorderLayout.NORTH);

		// Account list
		accountListPanel.setLayout(new BoxLayout(accountListPanel, BoxLayout.Y_AXIS));
		accountListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scrollPane = new JScrollPane(accountListPanel);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setBorder(null);
		scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

		unlockedPanel.add(scrollPane, BorderLayout.CENTER);
	}

	void rebuild()
	{
		removeAll();

		if (!plugin.isUnlocked())
		{
			rebuildLockedPanel();
			add(lockedPanel, BorderLayout.CENTER);
		}
		else
		{
			rebuildAccountList();
			add(unlockedPanel, BorderLayout.CENTER);
		}

		revalidate();
		repaint();
	}

	private void rebuildAccountList()
	{
		accountListPanel.removeAll();

		List<Account> accounts = plugin.getAccounts();
		if (accounts.isEmpty())
		{
			JLabel emptyLabel = new JLabel("No accounts added yet");
			emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			emptyLabel.setBorder(new EmptyBorder(20, 0, 0, 0));
			accountListPanel.add(emptyLabel);

			JLabel hintLabel = new JLabel("Click + to add an account");
			hintLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
			hintLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
			accountListPanel.add(hintLabel);
		}
		else
		{
			for (Account account : accounts)
			{
				AccountEntryPanel entryPanel = new AccountEntryPanel(plugin, this, account);
				entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, entryPanel.getPreferredSize().height));
				accountListPanel.add(entryPanel);
				accountListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
			}
		}

		accountListPanel.revalidate();
		accountListPanel.repaint();
	}

	void openAddAccountDialog(Account existingAccount)
	{
		AddAccountDialog dialog = new AddAccountDialog(
			SwingUtilities.getWindowAncestor(this),
			plugin,
			existingAccount
		);
		dialog.setVisible(true);

		if (dialog.isSaved())
		{
			rebuild();
		}
	}
}
