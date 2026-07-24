package com.example.crowdsensingwotandroidapp.datafly;

import android.util.Base64;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * Verifica dei token spot (M8, docs/04 §1 livello 4): l'ASK spot trasporta un
 * JWT ES256 firmato dal Core (aud = questo device, jti = ordine, exp breve).
 * Il device lo verifica con la CHIAVE PUBBLICA ricevuta alla registrazione:
 * anche se il broker fosse mal configurato o compromesso, il device risponde
 * solo a richieste emesse davvero dalla piattaforma.
 *
 * Nota implementativa: la firma JWT ES256 e' r||s grezza (64 byte), mentre
 * java.security.Signature "SHA256withECDSA" si aspetta DER — joseToDer fa la
 * conversione. Tutto con java.security standard: nessuna dipendenza JOSE.
 */
public final class SpotTokenVerifier {

	private static final String TAG = "DataFlySpotToken";
	private static final long CLOCK_SKEW_SEC = 30;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private SpotTokenVerifier() {
	}

	/**
	 * Verifica firma e claim del token spot.
	 *
	 * @param token        JWT compatto (header.payload.signature)
	 * @param publicKeyPem chiave pubblica SPKI PEM salvata alla registrazione
	 * @param deviceId     atteso come audience
	 * @param expectedJti  il jti dell'ASK: deve coincidere con quello firmato
	 * @return true se il token e' autentico e valido per questo device/ordine
	 */
	public static boolean verify(String token, String publicKeyPem, String deviceId,
			String expectedJti) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				Log.w(TAG, "token non in forma compatta JWT");
				return false;
			}
			JsonNode header = MAPPER.readTree(Base64.decode(parts[0], Base64.URL_SAFE));
			if (!"ES256".equals(header.path("alg").asText())) {
				Log.w(TAG, "alg inatteso: " + header.path("alg").asText());
				return false;
			}

			// 1) firma: ECDSA P-256/SHA-256 su "header.payload"
			byte[] signedContent =
					(parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
			byte[] joseSignature = Base64.decode(parts[2], Base64.URL_SAFE);
			Signature verifier = Signature.getInstance("SHA256withECDSA");
			verifier.initVerify(parsePublicKey(publicKeyPem));
			verifier.update(signedContent);
			if (!verifier.verify(joseToDer(joseSignature))) {
				Log.w(TAG, "firma del token non valida");
				return false;
			}

			// 2) claim: aud = questo device, jti = quello dell'ASK, non scaduto
			JsonNode claims = MAPPER.readTree(Base64.decode(parts[1], Base64.URL_SAFE));
			if (!deviceId.equals(claims.path("aud").asText())) {
				Log.w(TAG, "audience diversa da questo device: token ignorato");
				return false;
			}
			if (!expectedJti.equals(claims.path("jti").asText())) {
				Log.w(TAG, "jti del token diverso da quello dell'ASK");
				return false;
			}
			long exp = claims.path("exp").asLong(0);
			if (exp <= 0 || exp + CLOCK_SKEW_SEC < System.currentTimeMillis() / 1000) {
				Log.w(TAG, "token scaduto");
				return false;
			}
			return true;
		} catch (Exception e) {
			Log.w(TAG, "verifica token fallita", e);
			return false;
		}
	}

	/**
	 * Claim {@code orderId} del token (M10, pool di Master): riflesso
	 * nell'ANSWER cosi' qualunque Master del gruppo — anche uno diverso da chi
	 * ha inviato l'ASK — puo' instradare la consegna senza stato locale.
	 * Da chiamare solo DOPO {@link #verify}: qui non si riverifica la firma.
	 */
	public static String orderId(String token) {
		try {
			String[] parts = token.split("\\.");
			JsonNode claims = MAPPER.readTree(Base64.decode(parts[1], Base64.URL_SAFE));
			String orderId = claims.path("orderId").asText(null);
			return orderId != null && !orderId.isEmpty() ? orderId : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static PublicKey parsePublicKey(String pem) throws Exception {
		String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
		byte[] der = Base64.decode(base64, Base64.DEFAULT);
		return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
	}

	/**
	 * JOSE (r||s, 32+32 byte) → DER SEQUENCE(INTEGER r, INTEGER s), il formato
	 * che java.security si aspetta per ECDSA.
	 */
	static byte[] joseToDer(byte[] jose) {
		if (jose.length != 64) {
			throw new IllegalArgumentException("firma ES256 attesa di 64 byte");
		}
		byte[] r = toDerInteger(jose, 0);
		byte[] s = toDerInteger(jose, 32);
		int len = 2 + r.length + 2 + s.length;
		byte[] der = new byte[2 + len];
		int i = 0;
		der[i++] = 0x30; // SEQUENCE
		der[i++] = (byte) len;
		der[i++] = 0x02; // INTEGER
		der[i++] = (byte) r.length;
		System.arraycopy(r, 0, der, i, r.length);
		i += r.length;
		der[i++] = 0x02; // INTEGER
		der[i++] = (byte) s.length;
		System.arraycopy(s, 0, der, i, s.length);
		return der;
	}

	/** Componente a 32 byte → INTEGER DER: via gli zeri di testa, padding se il MSB e' 1. */
	private static byte[] toDerInteger(byte[] jose, int offset) {
		int start = offset;
		int end = offset + 32;
		while (start < end - 1 && jose[start] == 0) {
			start++;
		}
		boolean needsPadding = (jose[start] & 0x80) != 0;
		byte[] out = new byte[(end - start) + (needsPadding ? 1 : 0)];
		System.arraycopy(jose, start, out, needsPadding ? 1 : 0, end - start);
		return out;
	}
}
