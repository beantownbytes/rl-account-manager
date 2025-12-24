package com.accountmanager;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Security tests for EncryptionService.
 * Validates AES-256-GCM encryption and PBKDF2 key derivation.
 */
public class EncryptionServiceTest
{
	private static final String TEST_PASSWORD = "testMasterPassword123";
	private static final String TEST_PLAINTEXT = "sensitiveData123!@#";

	private String salt;
	private EncryptionService encryptionService;

	@Before
	public void setUp()
	{
		salt = EncryptionService.generateSalt();
		encryptionService = new EncryptionService(TEST_PASSWORD, salt);
	}

	// === Salt Generation Tests ===

	@Test
	public void testSaltGeneration_producesBase64String()
	{
		String generatedSalt = EncryptionService.generateSalt();
		assertNotNull("Salt should not be null", generatedSalt);
		assertFalse("Salt should not be empty", generatedSalt.isEmpty());

		// Verify it's valid Base64 (32 bytes = 44 chars in Base64 with padding)
		assertTrue("Salt should be valid Base64 length", generatedSalt.length() >= 40);
	}

	@Test
	public void testSaltGeneration_producesUniqueSalts()
	{
		String salt1 = EncryptionService.generateSalt();
		String salt2 = EncryptionService.generateSalt();
		String salt3 = EncryptionService.generateSalt();

		assertNotEquals("Each salt should be unique", salt1, salt2);
		assertNotEquals("Each salt should be unique", salt2, salt3);
		assertNotEquals("Each salt should be unique", salt1, salt3);
	}

	// === Encryption/Decryption Tests ===

	@Test
	public void testEncryptDecrypt_roundTrip()
	{
		String encrypted = encryptionService.encrypt(TEST_PLAINTEXT);
		String decrypted = encryptionService.decrypt(encrypted);

		assertEquals("Decrypted text should match original", TEST_PLAINTEXT, decrypted);
	}

	@Test
	public void testEncrypt_producesDifferentCiphertextEachTime()
	{
		// Due to random IV, same plaintext should produce different ciphertext
		String encrypted1 = encryptionService.encrypt(TEST_PLAINTEXT);
		String encrypted2 = encryptionService.encrypt(TEST_PLAINTEXT);
		String encrypted3 = encryptionService.encrypt(TEST_PLAINTEXT);

		assertNotEquals("Each encryption should produce unique ciphertext (random IV)", encrypted1, encrypted2);
		assertNotEquals("Each encryption should produce unique ciphertext (random IV)", encrypted2, encrypted3);
		assertNotEquals("Each encryption should produce unique ciphertext (random IV)", encrypted1, encrypted3);

		// But all should decrypt to same plaintext
		assertEquals(TEST_PLAINTEXT, encryptionService.decrypt(encrypted1));
		assertEquals(TEST_PLAINTEXT, encryptionService.decrypt(encrypted2));
		assertEquals(TEST_PLAINTEXT, encryptionService.decrypt(encrypted3));
	}

	@Test
	public void testEncrypt_ciphertextDoesNotContainPlaintext()
	{
		String sensitivePassword = "MySecretPassword123";
		String encrypted = encryptionService.encrypt(sensitivePassword);

		assertFalse("Ciphertext should not contain plaintext",
			encrypted.contains(sensitivePassword));
		assertFalse("Ciphertext should not contain plaintext (case insensitive)",
			encrypted.toLowerCase().contains(sensitivePassword.toLowerCase()));
	}

	@Test
	public void testEncrypt_handlesEmptyString()
	{
		String encrypted = encryptionService.encrypt("");
		String decrypted = encryptionService.decrypt(encrypted);

		assertEquals("Should handle empty string", "", decrypted);
	}

	@Test
	public void testEncrypt_handlesSpecialCharacters()
	{
		String specialChars = "p@$$w0rd!#$%^&*()_+-=[]{}|;':\",./<>?`~";
		String encrypted = encryptionService.encrypt(specialChars);
		String decrypted = encryptionService.decrypt(encrypted);

		assertEquals("Should handle special characters", specialChars, decrypted);
	}

	@Test
	public void testEncrypt_handlesUnicodeCharacters()
	{
		String unicode = "пароль密码パスワード🔐";
		String encrypted = encryptionService.encrypt(unicode);
		String decrypted = encryptionService.decrypt(encrypted);

		assertEquals("Should handle unicode characters", unicode, decrypted);
	}

	@Test
	public void testEncrypt_handlesLongStrings()
	{
		StringBuilder longString = new StringBuilder();
		for (int i = 0; i < 1000; i++)
		{
			longString.append("LongPassword123!");
		}
		String original = longString.toString();

		String encrypted = encryptionService.encrypt(original);
		String decrypted = encryptionService.decrypt(encrypted);

		assertEquals("Should handle long strings", original, decrypted);
	}

	// === Key Derivation Tests ===

	@Test
	public void testDifferentPasswords_produceDifferentKeys()
	{
		EncryptionService service1 = new EncryptionService("password1", salt);
		EncryptionService service2 = new EncryptionService("password2", salt);

		String encrypted1 = service1.encrypt(TEST_PLAINTEXT);
		String encrypted2 = service2.encrypt(TEST_PLAINTEXT);

		// Different passwords should produce different ciphertext
		// (More importantly, cross-decryption should fail)
		assertNotEquals("Different passwords should produce different results", encrypted1, encrypted2);
	}

	@Test
	public void testDifferentSalts_produceDifferentKeys()
	{
		String salt1 = EncryptionService.generateSalt();
		String salt2 = EncryptionService.generateSalt();

		EncryptionService service1 = new EncryptionService(TEST_PASSWORD, salt1);
		EncryptionService service2 = new EncryptionService(TEST_PASSWORD, salt2);

		String encrypted1 = service1.encrypt(TEST_PLAINTEXT);

		// Attempting to decrypt with different salt should fail
		try
		{
			service2.decrypt(encrypted1);
			fail("Decryption with different salt should fail");
		}
		catch (RuntimeException e)
		{
			// Expected - GCM authentication should fail
			assertTrue("Should fail due to authentication",
				e.getMessage().contains("Decryption failed"));
		}
	}

	@Test
	public void testSamePasswordAndSalt_producesSameKey()
	{
		EncryptionService service1 = new EncryptionService(TEST_PASSWORD, salt);
		EncryptionService service2 = new EncryptionService(TEST_PASSWORD, salt);

		String encrypted = service1.encrypt(TEST_PLAINTEXT);
		String decrypted = service2.decrypt(encrypted);

		assertEquals("Same password and salt should allow decryption", TEST_PLAINTEXT, decrypted);
	}

	// === Password Verification Tests ===

	@Test
	public void testVerifyPassword_correctPassword()
	{
		String testCiphertext = encryptionService.encrypt("verification");

		assertTrue("Correct password should verify",
			encryptionService.verifyPassword(testCiphertext));
	}

	@Test
	public void testVerifyPassword_wrongPassword()
	{
		String testCiphertext = encryptionService.encrypt("verification");

		EncryptionService wrongService = new EncryptionService("wrongPassword", salt);

		assertFalse("Wrong password should not verify",
			wrongService.verifyPassword(testCiphertext));
	}

	@Test
	public void testVerifyPassword_tamperedCiphertext()
	{
		String testCiphertext = encryptionService.encrypt("verification");

		// Tamper with the ciphertext
		String tampered = testCiphertext.substring(0, testCiphertext.length() - 5) + "XXXXX";

		assertFalse("Tampered ciphertext should not verify",
			encryptionService.verifyPassword(tampered));
	}

	// === Integrity Tests (GCM Authentication) ===

	@Test
	public void testDecrypt_detectsTampering()
	{
		String encrypted = encryptionService.encrypt(TEST_PLAINTEXT);

		// Tamper with the ciphertext (modify a character)
		char[] chars = encrypted.toCharArray();
		chars[20] = (chars[20] == 'A') ? 'B' : 'A';
		String tampered = new String(chars);

		try
		{
			encryptionService.decrypt(tampered);
			fail("Tampered ciphertext should fail authentication");
		}
		catch (RuntimeException e)
		{
			// Expected - GCM should detect tampering
			assertTrue("Should fail due to tampering detection",
				e.getMessage().contains("Decryption failed"));
		}
	}

	@Test
	public void testDecrypt_detectsTruncation()
	{
		String encrypted = encryptionService.encrypt(TEST_PLAINTEXT);

		// Truncate the ciphertext
		String truncated = encrypted.substring(0, encrypted.length() - 10);

		try
		{
			encryptionService.decrypt(truncated);
			fail("Truncated ciphertext should fail");
		}
		catch (RuntimeException e)
		{
			// Expected
			assertTrue("Should fail due to truncation",
				e.getMessage().contains("Decryption failed"));
		}
	}

	// === Edge Cases ===

	@Test(expected = RuntimeException.class)
	public void testDecrypt_invalidBase64()
	{
		encryptionService.decrypt("not-valid-base64!!!");
	}

	@Test
	public void testPasswordWithSpaces()
	{
		EncryptionService service = new EncryptionService("password with spaces", salt);
		String encrypted = service.encrypt(TEST_PLAINTEXT);
		String decrypted = service.decrypt(encrypted);

		assertEquals("Should handle passwords with spaces", TEST_PLAINTEXT, decrypted);
	}

	@Test
	public void testMinimalPassword()
	{
		EncryptionService service = new EncryptionService("1234", salt);
		String encrypted = service.encrypt(TEST_PLAINTEXT);
		String decrypted = service.decrypt(encrypted);

		assertEquals("Should handle minimal passwords", TEST_PLAINTEXT, decrypted);
	}
}
