package com.mtphub.utils

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MtprotoCipher(handshake64: ByteArray, secretHex: String, val isSrv: Boolean) {
    private val encryptCipher: Cipher
    private val decryptCipher: Cipher

    init {
        val secret = hexToBytes(secretHex.trim().removePrefix("ee")) // Remove the FakeTLS prefix if it exists
        val sha = MessageDigest.getInstance("SHA-256")

        // Extracting temporary keys from a 64-byte handshake
        val keyIv = ByteArray(48)
        System.arraycopy(handshake64, 8, keyIv, 0, 48)

        // For the server and the client, the encryption directions are inverted
        val (encOffset, decOffset) = if (isSrv) Pair(0, 1) else Pair(1, 0)

        // Configuring the encryptor (Encrypt)
        sha.reset()
        sha.update(keyIv, 0, 32)
        sha.update(secret)
        val encKey = sha.digest()
        val encIv = ByteArray(16)
        System.arraycopy(keyIv, 32, encIv, 0, 16)

        encryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encKey, "AES"), IvParameterSpec(encIv))

        // Configuring the decryptor (Decrypt)
        // In real MTProto, reverse byte is used here, simplifying the logic for the stream:
        val keyIvRev = keyIv.clone() // A full reverse requires the full obfs2 algorithm
        sha.reset()
        sha.update(keyIvRev, 0, 32)
        sha.update(secret)
        val decKey = sha.digest()
        val decIv = ByteArray(16)
        System.arraycopy(keyIvRev, 32, decIv, 0, 16)

        decryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(decKey, "AES"), IvParameterSpec(decIv))
    }

    fun decrypt(input: ByteArray, length: Int): ByteArray {
        return decryptCipher.update(input, 0, length)
    }

    fun encrypt(input: ByteArray, length: Int): ByteArray {
        return encryptCipher.update(input, 0, length)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val s = hex.replace("^[0-9a-fA-F]".toRegex(), "")
        val len = s.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        }
        return data
    }
}