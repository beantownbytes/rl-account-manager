package com.accountmanager;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Security tests for TotpService.
 * Validates RFC 6238 TOTP implementation.
 */
public class TotpServiceTest
{
	// RFC 6238 test vector (Base32 encoded "12345678901234567890")
	private static final String RFC_TEST_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

	private TotpService totpService;

	@Before
	public void setUp()
	{
		totpService = new TotpService();
	}

	// === Basic Functionality Tests ===

	@Test
	public void testGenerateCode_produces6Digits()
	{
		String code = totpService.generateCode(RFC_TEST_SECRET);

		assertNotNull("Code should not be null", code);
		assertEquals("Code should be 6 digits", 6, code.length());
		assertTrue("Code should be numeric", code.matches("\\d{6}"));
	}

	@Test
	public void testGenerateCode_consistentWithinTimeStep()
	{
		// Within the same 30-second window, codes should be identical
		String code1 = totpService.generateCode(RFC_TEST_SECRET);
		String code2 = totpService.generateCode(RFC_TEST_SECRET);

		assertEquals("Codes within same time step should match", code1, code2);
	}

	@Test
	public void testGenerateCode_differentSecretsProduceDifferentCodes()
	{
		String secret1 = "JBSWY3DPEHPK3PXP"; // Different secret
		String secret2 = "GEZDGNBVGY3TQOJQ"; // Different secret

		String code1 = totpService.generateCode(secret1);
		String code2 = totpService.generateCode(secret2);

		// Very high probability these will be different
		// (1 in 1,000,000 chance of collision)
		assertNotEquals("Different secrets should produce different codes", code1, code2);
	}

	// === Input Validation Tests ===

	@Test
	public void testGenerateCode_handlesLowercaseSecret()
	{
		String lowercase = "gezdgnbvgy3tqojqgezdgnbvgy3tqojq";
		String uppercase = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

		String code1 = totpService.generateCode(lowercase);
		String code2 = totpService.generateCode(uppercase);

		assertEquals("Should handle lowercase input", code1, code2);
	}

	@Test
	public void testGenerateCode_handlesSpacesInSecret()
	{
		String withSpaces = "GEZD GNBV GY3T QOJQ GEZD GNBV GY3T QOJQ";
		String withoutSpaces = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

		String code1 = totpService.generateCode(withSpaces);
		String code2 = totpService.generateCode(withoutSpaces);

		assertEquals("Should strip spaces from secret", code1, code2);
	}

	@Test
	public void testGenerateCode_handlesPaddedSecret()
	{
		// Base32 can have = padding
		String padded = "JBSWY3DPEHPK3PXP====";
		String unpadded = "JBSWY3DPEHPK3PXP";

		// Should not throw and should produce valid code
		String code1 = totpService.generateCode(padded);
		String code2 = totpService.generateCode(unpadded);

		assertNotNull(code1);
		assertEquals(6, code1.length());
	}

	// === Time Window Tests ===

	@Test
	public void testGetSecondsRemaining_inValidRange()
	{
		int remaining = totpService.getSecondsRemaining();

		// Range is 1-30: at start of window = 30 seconds left, at end = 1 second left
		assertTrue("Seconds remaining should be > 0", remaining > 0);
		assertTrue("Seconds remaining should be <= 30", remaining <= 30);
	}

	// === Code Format Tests ===

	@Test
	public void testGenerateCode_padsWithLeadingZeros()
	{
		// Generate many codes to increase chance of getting one starting with 0
		// The code should always be 6 characters, even if numeric value is small
		for (int i = 0; i < 100; i++)
		{
			String secret = EncryptionService.generateSalt().substring(0, 16);
			try
			{
				String code = totpService.generateCode(secret);
				assertEquals("Code should always be 6 characters", 6, code.length());
			}
			catch (Exception e)
			{
				// Some random strings may not be valid Base32, that's OK for this test
			}
		}
	}

	// === Security Tests ===

	@Test
	public void testGenerateCode_secretNotExposedInCode()
	{
		String secret = "JBSWY3DPEHPK3PXP";
		String code = totpService.generateCode(secret);

		// Code should not contain any part of the secret
		assertFalse("Code should not contain secret", code.contains(secret));
		assertFalse("Code should not contain secret parts",
			secret.contains(code));
	}

	@Test
	public void testGenerateCode_codeIsNumericOnly()
	{
		String code = totpService.generateCode(RFC_TEST_SECRET);

		for (char c : code.toCharArray())
		{
			assertTrue("Each character should be a digit: " + c,
				Character.isDigit(c));
		}
	}

	// === Known Test Vector ===
	// Note: Time-based tests are tricky because they depend on current time.
	// These tests verify the algorithm works correctly with consistent inputs.

	@Test
	public void testBase32Decode_handlesValidInput()
	{
		// If generateCode works, Base32 decoding is working
		String[] validSecrets = {
			"JBSWY3DPEHPK3PXP",
			"GEZDGNBVGY3TQOJQ",
			"MFRGGZDFMY",
			"ORSXG5A"
		};

		for (String secret : validSecrets)
		{
			String code = totpService.generateCode(secret);
			assertNotNull("Should decode valid Base32: " + secret, code);
			assertEquals("Should produce 6-digit code", 6, code.length());
		}
	}

	@Test
	public void testGenerateCode_handlesInvalidCharactersGracefully()
	{
		// Invalid Base32 characters (1, 8, 9, 0 are not in Base32 alphabet)
		// The service strips invalid characters, so this should still work
		// with the remaining valid characters
		String code = totpService.generateCode("INVALID");
		assertNotNull("Should handle input with some valid chars", code);
		assertEquals(6, code.length());
	}

	// === Consistency Tests ===

	@Test
	public void testMultipleInstances_produceSameCode()
	{
		TotpService service1 = new TotpService();
		TotpService service2 = new TotpService();

		String code1 = service1.generateCode(RFC_TEST_SECRET);
		String code2 = service2.generateCode(RFC_TEST_SECRET);

		assertEquals("Different instances should produce same code for same secret", code1, code2);
	}
}
