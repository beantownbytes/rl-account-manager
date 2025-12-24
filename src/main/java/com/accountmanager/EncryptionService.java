package com.accountmanager;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

public class EncryptionService
{
	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int KEY_SIZE = 256;
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;
	private static final int PBKDF2_ITERATIONS = 310000;
	private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

	private final SecretKey secretKey;

	public EncryptionService(String masterPassword, String saltBase64)
	{
		byte[] salt = Base64.getDecoder().decode(saltBase64);
		this.secretKey = deriveKey(masterPassword, salt);
	}

	public static String generateSalt()
	{
		byte[] salt = new byte[32];
		new SecureRandom().nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt);
	}

	private SecretKey deriveKey(String password, byte[] salt)
	{
		try
		{
			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE);
			byte[] keyBytes = factory.generateSecret(spec).getEncoded();
			return new SecretKeySpec(keyBytes, "AES");
		}
		catch (Exception e)
		{
			throw new RuntimeException("Key derivation failed", e);
		}
	}

	public String encrypt(String plaintext)
	{
		try
		{
			byte[] iv = new byte[GCM_IV_LENGTH];
			new SecureRandom().nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			byte[] combined = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

			return Base64.getEncoder().encodeToString(combined);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Encryption failed", e);
		}
	}

	public String decrypt(String encryptedBase64)
	{
		try
		{
			byte[] combined = Base64.getDecoder().decode(encryptedBase64);

			byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
			byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

			byte[] plaintext = cipher.doFinal(ciphertext);
			return new String(plaintext, StandardCharsets.UTF_8);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Decryption failed", e);
		}
	}

	public boolean verifyPassword(String testCiphertext)
	{
		try
		{
			decrypt(testCiphertext);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}
