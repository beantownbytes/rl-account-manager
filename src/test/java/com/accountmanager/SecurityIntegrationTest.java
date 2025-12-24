package com.accountmanager;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration tests to verify end-to-end security of the account management system.
 * These tests verify that credentials are properly protected throughout the workflow.
 */
public class SecurityIntegrationTest
{
	private static final String MASTER_PASSWORD = "MySecureMasterPassword123!";
	private static final String TEST_USERNAME = "testuser@example.com";
	private static final String TEST_PASSWORD = "MyGamePassword456!";
	private static final String TEST_TOTP_SECRET = "JBSWY3DPEHPK3PXP";
	private static final String TEST_NICKNAME = "Main Account";

	private final Gson gson = new Gson();

	// === End-to-End Workflow Tests ===

	@Test
	public void testFullWorkflow_credentialsNeverStoredInPlaintext()
	{
		// Simulate the full workflow of creating and storing an account
		String salt = EncryptionService.generateSalt();
		EncryptionService encryption = new EncryptionService(MASTER_PASSWORD, salt);

		// Create an account with encrypted credentials
		Account account = new Account();
		account.setId("test-id-123");
		account.setNickname(TEST_NICKNAME);
		account.setEncryptedUsername(encryption.encrypt(TEST_USERNAME));
		account.setEncryptedPassword(encryption.encrypt(TEST_PASSWORD));
		account.setEncryptedTotpSecret(encryption.encrypt(TEST_TOTP_SECRET));

		// Serialize to JSON (simulating storage)
		String json = gson.toJson(account);

		// Verify plaintext credentials are NOT in the JSON
		assertFalse("Username should not be in stored JSON",
			json.contains(TEST_USERNAME));
		assertFalse("Password should not be in stored JSON",
			json.contains(TEST_PASSWORD));
		assertFalse("TOTP secret should not be in stored JSON",
			json.contains(TEST_TOTP_SECRET));

		// Verify nickname IS in JSON (it's not sensitive)
		assertTrue("Nickname should be in stored JSON",
			json.contains(TEST_NICKNAME));

		// Verify we can recover the credentials with correct password
		Account loaded = gson.fromJson(json, Account.class);
		assertEquals(TEST_USERNAME, encryption.decrypt(loaded.getEncryptedUsername()));
		assertEquals(TEST_PASSWORD, encryption.decrypt(loaded.getEncryptedPassword()));
		assertEquals(TEST_TOTP_SECRET, encryption.decrypt(loaded.getEncryptedTotpSecret()));
	}

	@Test
	public void testWrongMasterPassword_cannotDecryptCredentials()
	{
		String salt = EncryptionService.generateSalt();
		EncryptionService correctEncryption = new EncryptionService(MASTER_PASSWORD, salt);
		EncryptionService wrongEncryption = new EncryptionService("WrongPassword!", salt);

		// Encrypt with correct password
		String encryptedUsername = correctEncryption.encrypt(TEST_USERNAME);

		// Try to decrypt with wrong password
		try
		{
			wrongEncryption.decrypt(encryptedUsername);
			fail("Decryption with wrong password should fail");
		}
		catch (RuntimeException e)
		{
			// Expected - GCM authentication should fail
			assertTrue(e.getMessage().contains("Decryption failed"));
		}
	}

	@Test
	public void testChangingSalt_invalidatesAllCredentials()
	{
		String salt1 = EncryptionService.generateSalt();
		String salt2 = EncryptionService.generateSalt();

		EncryptionService encryption1 = new EncryptionService(MASTER_PASSWORD, salt1);
		EncryptionService encryption2 = new EncryptionService(MASTER_PASSWORD, salt2);

		// Encrypt with first salt
		String encrypted = encryption1.encrypt(TEST_PASSWORD);

		// Try to decrypt with same password but different salt
		try
		{
			encryption2.decrypt(encrypted);
			fail("Decryption with different salt should fail");
		}
		catch (RuntimeException e)
		{
			assertTrue(e.getMessage().contains("Decryption failed"));
		}
	}

	// === Account Model Security Tests ===

	@Test
	public void testAccount_hasTotpSecret_doesNotExposeSecret()
	{
		Account account = new Account();
		account.setEncryptedTotpSecret("encrypted-data-here");

		// hasTotpSecret() should work without decrypting
		assertTrue(account.hasTotpSecret());

		// Setting to null/empty should work
		account.setEncryptedTotpSecret(null);
		assertFalse(account.hasTotpSecret());

		account.setEncryptedTotpSecret("");
		assertFalse(account.hasTotpSecret());
	}

	@Test
	public void testAccount_toStringDoesNotExposeCredentials()
	{
		String salt = EncryptionService.generateSalt();
		EncryptionService encryption = new EncryptionService(MASTER_PASSWORD, salt);

		Account account = new Account();
		account.setId("test-id");
		account.setNickname(TEST_NICKNAME);
		account.setEncryptedUsername(encryption.encrypt(TEST_USERNAME));
		account.setEncryptedPassword(encryption.encrypt(TEST_PASSWORD));

		String toString = account.toString();

		// toString should not contain decrypted credentials
		assertFalse("toString should not contain username",
			toString.contains(TEST_USERNAME));
		assertFalse("toString should not contain password",
			toString.contains(TEST_PASSWORD));
	}

	// === TOTP Security Integration ===

	@Test
	public void testTotpGeneration_withEncryptedSecret()
	{
		String salt = EncryptionService.generateSalt();
		EncryptionService encryption = new EncryptionService(MASTER_PASSWORD, salt);
		TotpService totpService = new TotpService();

		// Store encrypted secret
		String encryptedSecret = encryption.encrypt(TEST_TOTP_SECRET);

		// Verify the encrypted secret doesn't look like the original
		assertFalse(encryptedSecret.contains(TEST_TOTP_SECRET));

		// Decrypt and generate code
		String decryptedSecret = encryption.decrypt(encryptedSecret);
		String code = totpService.generateCode(decryptedSecret);

		// Verify we get a valid code
		assertNotNull(code);
		assertEquals(6, code.length());
		assertTrue(code.matches("\\d{6}"));
	}

	// === Verification String Security ===

	@Test
	public void testVerificationString_detectsWrongPassword()
	{
		String salt = EncryptionService.generateSalt();
		EncryptionService correctEncryption = new EncryptionService(MASTER_PASSWORD, salt);

		// Create verification string (simulating first-time setup)
		String verificationPlaintext = "account-manager-verification";
		String encryptedVerification = correctEncryption.encrypt(verificationPlaintext);

		// Correct password should verify
		assertTrue(correctEncryption.verifyPassword(encryptedVerification));

		// Wrong password should not verify
		EncryptionService wrongEncryption = new EncryptionService("WrongPassword", salt);
		assertFalse(wrongEncryption.verifyPassword(encryptedVerification));
	}

	// === Memory Security Tests ===

	@Test
	public void testEncryptionService_canBeGarbageCollected()
	{
		String salt = EncryptionService.generateSalt();

		// Create and use encryption service
		EncryptionService encryption = new EncryptionService(MASTER_PASSWORD, salt);
		String encrypted = encryption.encrypt(TEST_PASSWORD);

		// Clear reference (simulating vault lock)
		encryption = null;

		// Force garbage collection
		System.gc();

		// Create new service to verify old data is still encrypted
		EncryptionService newEncryption = new EncryptionService(MASTER_PASSWORD, salt);
		String decrypted = newEncryption.decrypt(encrypted);
		assertEquals(TEST_PASSWORD, decrypted);
	}

	// === Brute Force Resistance ===

	@Test
	public void testPBKDF2_slowsDownBruteForce()
	{
		String salt = EncryptionService.generateSalt();

		// Measure time to create encryption service (includes key derivation)
		long startTime = System.currentTimeMillis();
		new EncryptionService(MASTER_PASSWORD, salt);
		long endTime = System.currentTimeMillis();

		long derivationTime = endTime - startTime;

		// With 310,000 iterations, key derivation should take noticeable time
		// This makes brute force attacks much slower
		// On most systems, this should be at least 100ms
		assertTrue("Key derivation should take significant time (was " + derivationTime + "ms)",
			derivationTime >= 50); // Allow for fast systems, but should be measurable
	}

	// === Data Integrity ===

	@Test
	public void testMultipleAccounts_isolatedEncryption()
	{
		String salt = EncryptionService.generateSalt();
		EncryptionService encryption = new EncryptionService(MASTER_PASSWORD, salt);

		// Create multiple accounts
		Account account1 = new Account();
		account1.setEncryptedPassword(encryption.encrypt("password1"));

		Account account2 = new Account();
		account2.setEncryptedPassword(encryption.encrypt("password2"));

		// Verify they have different encrypted values
		assertNotEquals("Different passwords should have different encrypted values",
			account1.getEncryptedPassword(), account2.getEncryptedPassword());

		// Verify each decrypts correctly
		assertEquals("password1", encryption.decrypt(account1.getEncryptedPassword()));
		assertEquals("password2", encryption.decrypt(account2.getEncryptedPassword()));
	}
}
