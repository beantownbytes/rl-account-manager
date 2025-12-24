package com.accountmanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account
{
	private String id;
	private String nickname;
	private String encryptedUsername;
	private String encryptedPassword;
	private String encryptedTotpSecret;

	public boolean hasTotpSecret()
	{
		return encryptedTotpSecret != null && !encryptedTotpSecret.isEmpty();
	}
}
