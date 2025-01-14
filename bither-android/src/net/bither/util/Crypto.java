/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.util;

import android.util.Base64;

import net.bither.BitherSetting;

import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.ParametersWithIV;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.SecureRandom;

import javax.annotation.Nonnull;

public class Crypto {
	/**
	 * number of times the password & salt are hashed during key creation.
	 */
	private static final int NUMBER_OF_ITERATIONS = 1024;

	/**
	 * Key length.
	 */
	private static final int KEY_LENGTH = 256;

	/**
	 * Initialization vector length.
	 */
	private static final int IV_LENGTH = 128;

	/**
	 * The length of the salt.
	 */
	private static final int SALT_LENGTH = 8;

	/**
	 * OpenSSL salted prefix text.
	 */
	private static final String OPENSSL_SALTED_TEXT = "Salted__";

	/**
	 * OpenSSL salted prefix bytes - also used as magic number for encrypted key
	 * file.
	 */
	private static final byte[] OPENSSL_SALTED_BYTES = OPENSSL_SALTED_TEXT
			.getBytes(BitherSetting.UTF_8);

	/**
	 * Magic text that appears at the beginning of every OpenSSL encrypted file.
	 * Used in identifying encrypted key files.
	 */
	private static final String OPENSSL_MAGIC_TEXT = new String(
			encodeBase64(Crypto.OPENSSL_SALTED_BYTES), BitherSetting.UTF_8)
			.substring(0,
					Crypto.NUMBER_OF_CHARACTERS_TO_MATCH_IN_OPENSSL_MAGIC_TEXT);

	private static final int NUMBER_OF_CHARACTERS_TO_MATCH_IN_OPENSSL_MAGIC_TEXT = 10;

	private static final SecureRandom secureRandom = new SecureRandom();

	/**
	 * Get password and generate key and iv.
	 * 
	 * @param password
	 *            The password to use in key generation
	 * @param salt
	 *            The salt to use in key generation
	 * @return The CipherParameters containing the created key
	 */
	private static CipherParameters getAESPasswordKey(final char[] password,
			final byte[] salt) {
		final PBEParametersGenerator generator = new OpenSSLPBEParametersGenerator();
		generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(password),
				salt, NUMBER_OF_ITERATIONS);

		final ParametersWithIV key = (ParametersWithIV) generator
				.generateDerivedParameters(KEY_LENGTH, IV_LENGTH);

		return key;
	}

	/**
	 * Password based encryption using AES - CBC 256 bits.
	 * 
	 * @param plainText
	 *            The text to encrypt
	 * @param password
	 *            The password to use for encryption
	 * @return The encrypted string
	 * @throws IOException
	 */
	public static String encrypt(@Nonnull final String plainText,
			@Nonnull final char[] password) throws IOException {
		final byte[] plainTextAsBytes = plainText.getBytes(BitherSetting.UTF_8);

		final byte[] encryptedBytes = encrypt(plainTextAsBytes, password);

		// OpenSSL prefixes the salt bytes + encryptedBytes with Salted___ and
		// then base64 encodes it
		final byte[] encryptedBytesPlusSaltedText = concat(
				OPENSSL_SALTED_BYTES, encryptedBytes);

		return new String(encodeBase64(encryptedBytesPlusSaltedText),
				BitherSetting.UTF_8);
	}

	/**
	 * Password based encryption using AES - CBC 256 bits.
	 * 
	 * @param plainBytes
	 *            The bytes to encrypt
	 * @param password
	 *            The password to use for encryption
	 * @return SALT_LENGTH bytes of salt followed by the encrypted bytes.
	 * @throws IOException
	 */
	private static byte[] encrypt(final byte[] plainTextAsBytes,
			final char[] password) throws IOException {
		try {
			// Generate salt - each encryption call has a different salt.
			final byte[] salt = new byte[SALT_LENGTH];
			secureRandom.nextBytes(salt);

			final ParametersWithIV key = (ParametersWithIV) getAESPasswordKey(
					password, salt);

			// The following code uses an AES cipher to encrypt the message.
			final BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
					new CBCBlockCipher(new AESFastEngine()));
			cipher.init(true, key);
			final byte[] encryptedBytes = new byte[cipher
					.getOutputSize(plainTextAsBytes.length)];
			final int length = cipher.processBytes(plainTextAsBytes, 0,
					plainTextAsBytes.length, encryptedBytes, 0);

			cipher.doFinal(encryptedBytes, length);

			// The result bytes are the SALT_LENGTH bytes followed by the
			// encrypted bytes.
			return concat(salt, encryptedBytes);
		} catch (final InvalidCipherTextException x) {
			throw new IOException("Could not encrypt bytes", x);
		} catch (final DataLengthException x) {
			throw new IOException("Could not encrypt bytes", x);
		}
	}

	/**
	 * Decrypt text previously encrypted with this class.
	 * 
	 * @param textToDecode
	 *            The code to decrypt
	 * @param password
	 *            password to use for decryption
	 * @return The decrypted text
	 * @throws IOException
	 */
	public static String decrypt(@Nonnull final String textToDecode,
			@Nonnull final char[] password) throws IOException {
		final byte[] decodeTextAsBytes = decodeBase64(textToDecode
				.getBytes(BitherSetting.UTF_8));

		if (decodeTextAsBytes.length < OPENSSL_SALTED_BYTES.length)
			throw new IOException("out of salt");

		final byte[] cipherBytes = new byte[decodeTextAsBytes.length
				- OPENSSL_SALTED_BYTES.length];
		System.arraycopy(decodeTextAsBytes, OPENSSL_SALTED_BYTES.length,
				cipherBytes, 0, decodeTextAsBytes.length
						- OPENSSL_SALTED_BYTES.length);

		final byte[] decryptedBytes = decrypt(cipherBytes, password);

		return new String(decryptedBytes, BitherSetting.UTF_8).trim();
	}

	/**
	 * Decrypt bytes previously encrypted with this class.
	 * 
	 * @param bytesToDecode
	 *            The bytes to decrypt
	 * @param passwordbThe
	 *            password to use for decryption
	 * @return The decrypted bytes
	 * @throws IOException
	 */
	private static byte[] decrypt(final byte[] bytesToDecode,
			final char[] password) throws IOException {
		try {
			// separate the salt and bytes to decrypt
			final byte[] salt = new byte[SALT_LENGTH];

			System.arraycopy(bytesToDecode, 0, salt, 0, SALT_LENGTH);

			final byte[] cipherBytes = new byte[bytesToDecode.length
					- SALT_LENGTH];
			System.arraycopy(bytesToDecode, SALT_LENGTH, cipherBytes, 0,
					bytesToDecode.length - SALT_LENGTH);

			final ParametersWithIV key = (ParametersWithIV) getAESPasswordKey(
					password, salt);

			// decrypt the message
			final BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
					new CBCBlockCipher(new AESFastEngine()));
			cipher.init(false, key);

			final byte[] decryptedBytes = new byte[cipher
					.getOutputSize(cipherBytes.length)];
			final int length = cipher.processBytes(cipherBytes, 0,
					cipherBytes.length, decryptedBytes, 0);

			cipher.doFinal(decryptedBytes, length);

			return decryptedBytes;
		} catch (final InvalidCipherTextException x) {
			throw new IOException("Could not decrypt input string", x);
		} catch (final DataLengthException x) {
			throw new IOException("Could not decrypt input string", x);
		}
	}

	private static byte[] encodeBase64(byte[] decoded) {
		return Base64.encode(decoded, Base64.DEFAULT);
	}

	private static byte[] decodeBase64(byte[] encoded) throws IOException {
		try {
			return Base64.decode(encoded, Base64.DEFAULT);
		} catch (final IllegalArgumentException x) {
			throw new IOException("illegal base64 padding", x);
		}
	}

	/**
	 * Concatenate two byte arrays.
	 */
	private static byte[] concat(final byte[] arrayA, final byte[] arrayB) {
		final byte[] result = new byte[arrayA.length + arrayB.length];
		System.arraycopy(arrayA, 0, result, 0, arrayA.length);
		System.arraycopy(arrayB, 0, result, arrayA.length, arrayB.length);

		return result;
	}

	public final static FileFilter OPENSSL_FILE_FILTER = new FileFilter() {
		private final char[] buf = new char[OPENSSL_MAGIC_TEXT.length()];

		@Override
		public boolean accept(final File file) {
			Reader in = null;
			try {
				in = new InputStreamReader(new FileInputStream(file),
						BitherSetting.UTF_8);
				if (in.read(buf) == -1)
					return false;
				final String str = new String(buf);
				if (!str.toString().equals(OPENSSL_MAGIC_TEXT))
					return false;
				return true;
			} catch (final IOException x) {
				return false;
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (final IOException x2) {
					}
				}
			}
		}
	};
}
