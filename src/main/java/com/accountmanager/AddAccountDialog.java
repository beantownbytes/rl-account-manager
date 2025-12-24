package com.accountmanager;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.UUID;

class AddAccountDialog extends JDialog
{
	private final AccountManagerPlugin plugin;
	private final Account existingAccount;

	private final JTextField nicknameField = new JTextField();
	private final JTextField usernameField = new JTextField();
	private final JPasswordField passwordField = new JPasswordField();
	private final JPasswordField totpField = new JPasswordField();
	private final JCheckBox showSecretsCheck = new JCheckBox("Show secrets");

	@Getter
	private boolean saved = false;

	AddAccountDialog(Window owner, AccountManagerPlugin plugin, Account existingAccount)
	{
		super(owner, existingAccount == null ? "Add Account" : "Edit Account",
			ModalityType.APPLICATION_MODAL);
		this.plugin = plugin;
		this.existingAccount = existingAccount;

		buildUI();

		if (existingAccount != null)
		{
			populateFields();
		}

		pack();
		setMinimumSize(new Dimension(300, getHeight()));
		setLocationRelativeTo(owner);
	}

	private void buildUI()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);

		// Nickname
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		panel.add(new JLabel("Nickname:"), c);
		c.gridx = 1;
		c.weightx = 1;
		nicknameField.setToolTipText("A display name for this account");
		panel.add(nicknameField, c);

		// Username
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		panel.add(new JLabel("Username/Email:"), c);
		c.gridx = 1;
		c.weightx = 1;
		panel.add(usernameField, c);

		// Password
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		panel.add(new JLabel("Password:"), c);
		c.gridx = 1;
		c.weightx = 1;
		panel.add(passwordField, c);

		// TOTP Secret (optional)
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 0;
		panel.add(new JLabel("TOTP Secret:"), c);
		c.gridx = 1;
		c.weightx = 1;
		totpField.setToolTipText("Optional - Base32 encoded secret from authenticator setup");
		panel.add(totpField, c);

		// Show secrets checkbox (reveals both password and TOTP)
		c.gridx = 1;
		c.gridy = 4;
		showSecretsCheck.addActionListener(e ->
		{
			char echoChar = showSecretsCheck.isSelected() ? (char) 0 : '*';
			passwordField.setEchoChar(echoChar);
			totpField.setEchoChar(echoChar);
		});
		panel.add(showSecretsCheck, c);

		// TOTP hint
		c.gridx = 1;
		c.gridy = 5;
		JLabel totpHint = new JLabel("<html><i>(Optional - leave blank to enter 2FA manually)</i></html>");
		totpHint.setFont(totpHint.getFont().deriveFont(Font.PLAIN, 10f));
		totpHint.setForeground(Color.GRAY);
		panel.add(totpHint, c);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> dispose());

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(e -> save());

		buttonPanel.add(cancelButton);
		buttonPanel.add(saveButton);

		c.gridx = 0;
		c.gridy = 6;
		c.gridwidth = 2;
		c.insets = new Insets(15, 5, 5, 5);
		panel.add(buttonPanel, c);

		add(panel);
	}

	private void populateFields()
	{
		nicknameField.setText(existingAccount.getNickname());
		usernameField.setText(plugin.getEncryptionService().decrypt(
			existingAccount.getEncryptedUsername()));
		passwordField.setText(plugin.getEncryptionService().decrypt(
			existingAccount.getEncryptedPassword()));
		if (existingAccount.hasTotpSecret())
		{
			String totpSecret = plugin.getEncryptionService().decrypt(
				existingAccount.getEncryptedTotpSecret());
			totpField.setText(totpSecret);
		}
	}

	private void save()
	{
		String nickname = nicknameField.getText().trim();
		String username = usernameField.getText().trim();
		String password = new String(passwordField.getPassword());
		String totpSecret = new String(totpField.getPassword()).trim().toUpperCase().replaceAll("\\s+", "");

		if (nickname.isEmpty() || username.isEmpty() || password.isEmpty())
		{
			JOptionPane.showMessageDialog(this,
				"Nickname, username, and password are required",
				"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Validate TOTP secret if provided
		if (!totpSecret.isEmpty())
		{
			if (!totpSecret.matches("^[A-Z2-7]+=*$"))
			{
				JOptionPane.showMessageDialog(this,
					"TOTP secret must be a valid Base32 string",
					"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Test that we can generate a code
			try
			{
				plugin.getTotpService().generateCode(totpSecret);
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(this,
					"Invalid TOTP secret: " + e.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		EncryptionService encryption = plugin.getEncryptionService();

		Account account;
		if (existingAccount != null)
		{
			account = existingAccount;
		}
		else
		{
			account = new Account();
			account.setId(UUID.randomUUID().toString());
		}

		account.setNickname(nickname);
		account.setEncryptedUsername(encryption.encrypt(username));
		account.setEncryptedPassword(encryption.encrypt(password));
		account.setEncryptedTotpSecret(
			totpSecret.isEmpty() ? null : encryption.encrypt(totpSecret)
		);

		if (existingAccount != null)
		{
			plugin.updateAccount(account);
		}
		else
		{
			plugin.addAccount(account);
		}

		saved = true;
		dispose();
	}
}
