package com.farmatodo.order.services;

import com.farmatodo.order.config.TokenizationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CardCipher {

	private static final int IV_LENGTH = 12;
	private static final int TAG_BITS = 128;

	private final SecretKeySpec key;
	private final SecureRandom random = new SecureRandom();

	public CardCipher(TokenizationProperties properties) {
		if (properties.encryptionKey() == null || properties.encryptionKey().isBlank()) {
			throw new IllegalStateException("Tokenization encryption key is required");
		}
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(properties.encryptionKey().getBytes(StandardCharsets.UTF_8));
			this.key = new SecretKeySpec(digest, "AES");
		} catch (Exception exception) {
			throw new IllegalStateException("Tokenization encryption key could not be loaded", exception);
		}
	}

	public String encrypt(String plainText) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			byte[] payload = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, payload, 0, iv.length);
			System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
			return Base64.getEncoder().encodeToString(payload);
		} catch (Exception exception) {
			throw new IllegalStateException("Card number could not be encrypted", exception);
		}
	}
}
