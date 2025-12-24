package com.accountmanager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

public class TotpService
{
	private static final int TIME_STEP_SECONDS = 30;
	private static final int CODE_DIGITS = 6;
	private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	public String generateCode(String secretBase32)
	{
		try
		{
			byte[] key = base32Decode(secretBase32);
			long counter = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

			byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

			Mac hmac = Mac.getInstance("HmacSHA1");
			hmac.init(new SecretKeySpec(key, "HmacSHA1"));
			byte[] hash = hmac.doFinal(counterBytes);

			int offset = hash[hash.length - 1] & 0x0F;
			int binary = ((hash[offset] & 0x7F) << 24) |
				((hash[offset + 1] & 0xFF) << 16) |
				((hash[offset + 2] & 0xFF) << 8) |
				(hash[offset + 3] & 0xFF);

			int otp = binary % (int) Math.pow(10, CODE_DIGITS);
			return String.format("%0" + CODE_DIGITS + "d", otp);
		}
		catch (Exception e)
		{
			throw new RuntimeException("TOTP generation failed", e);
		}
	}

	public int getSecondsRemaining()
	{
		return TIME_STEP_SECONDS - (int) (System.currentTimeMillis() / 1000 % TIME_STEP_SECONDS);
	}

	private byte[] base32Decode(String input)
	{
		input = input.toUpperCase().replaceAll("[^A-Z2-7]", "");

		int outputLength = input.length() * 5 / 8;
		byte[] output = new byte[outputLength];

		int buffer = 0;
		int bitsLeft = 0;
		int outputIndex = 0;

		for (char c : input.toCharArray())
		{
			buffer = (buffer << 5) | BASE32_CHARS.indexOf(c);
			bitsLeft += 5;
			if (bitsLeft >= 8)
			{
				output[outputIndex++] = (byte) (buffer >> (bitsLeft - 8));
				bitsLeft -= 8;
			}
		}

		return output;
	}
}
