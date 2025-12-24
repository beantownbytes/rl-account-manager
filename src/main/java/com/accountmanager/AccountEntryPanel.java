package com.accountmanager;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class AccountEntryPanel extends JPanel
{
	private final AccountManagerPlugin plugin;
	private final AccountManagerPanel parentPanel;
	private final Account account;

	AccountEntryPanel(AccountManagerPlugin plugin, AccountManagerPanel parentPanel, Account account)
	{
		this.plugin = plugin;
		this.parentPanel = parentPanel;
		this.account = account;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel infoPanel = new JPanel(new BorderLayout());
		infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nicknameLabel = new JLabel(account.getNickname());
		nicknameLabel.setForeground(Color.WHITE);
		nicknameLabel.setFont(FontManager.getRunescapeSmallFont());
		infoPanel.add(nicknameLabel, BorderLayout.CENTER);

		if (account.hasTotpSecret())
		{
			JLabel totpLabel = new JLabel(Icons.TOTP_ICON);
			totpLabel.setToolTipText("Has 2FA configured");
			totpLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
			infoPanel.add(totpLabel, BorderLayout.EAST);
		}

		JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		actionsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel loginLabel = new JLabel(Icons.LOGIN_ICON);
		loginLabel.setToolTipText("Fill credentials");
		loginLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		loginLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				plugin.fillCredentials(account);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				loginLabel.setIcon(Icons.LOGIN_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				loginLabel.setIcon(Icons.LOGIN_ICON);
			}
		});

		JLabel editLabel = new JLabel(Icons.EDIT_ICON);
		editLabel.setToolTipText("Edit account");
		editLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		editLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				parentPanel.openAddAccountDialog(account);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				editLabel.setIcon(Icons.EDIT_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				editLabel.setIcon(Icons.EDIT_ICON);
			}
		});

		JLabel deleteLabel = new JLabel(Icons.DELETE_ICON);
		deleteLabel.setToolTipText("Delete account");
		deleteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		deleteLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				int confirm = JOptionPane.showConfirmDialog(
					AccountEntryPanel.this,
					"Delete account '" + account.getNickname() + "'?",
					"Confirm Delete",
					JOptionPane.YES_NO_OPTION
				);
				if (confirm == JOptionPane.YES_OPTION)
				{
					plugin.deleteAccount(account);
					parentPanel.rebuild();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				deleteLabel.setIcon(Icons.DELETE_HOVER_ICON);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				deleteLabel.setIcon(Icons.DELETE_ICON);
			}
		});

		actionsPanel.add(loginLabel);
		actionsPanel.add(editLabel);
		actionsPanel.add(deleteLabel);

		add(infoPanel, BorderLayout.CENTER);
		add(actionsPanel, BorderLayout.EAST);
	}
}
