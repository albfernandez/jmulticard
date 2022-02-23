/**
 *  Copyright 2011, Tobias Senger
 *
 *  This file is part of animamea.
 *
 *  Animamea is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Animamea is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with animamea.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.gob.jmulticard.de.tsenger.androsmex.iso7816;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

import org.spongycastle.asn1.ASN1InputStream;

import es.gob.jmulticard.CryptoHelper;
import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.apdu.CommandApdu;
import es.gob.jmulticard.apdu.ResponseApdu;
import es.gob.jmulticard.asn1.Tlv;
import es.gob.jmulticard.asn1.TlvException;
import es.gob.jmulticard.de.tsenger.androsmex.crypto.AmAESCrypto;
import es.gob.jmulticard.de.tsenger.androsmex.crypto.AmCryptoException;

/** Empaquetado de env&iacute;o y recepci&oacute;n de APDUs
 * para establecer una mensajer&iacute;a segura.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s.
 * @author Tobias Senger (tobias@t-senger.de). */
public final class SecureMessaging {

	private final byte[] kenc;
	private final byte[] kmac;
	private final byte[] ssc;
	private final CryptoHelper cryptoHelper;

	/** Constructor.
	 * @param ksenc Clave de sesi&oacute;n para encriptar.
	 * @param ksmac Clave de sesi&oacute;n para el <i>checksum</i>.
	 * @param initialSSC Contador de sequencia de env&iacute;o.
	 * @param ch Utilidad para operaciones criptogr&aacute;ficas. */
	public SecureMessaging(final byte[] ksenc,
						   final byte[] ksmac,
						   final byte[] initialSSC,
						   final CryptoHelper ch) {
		this.cryptoHelper = ch;
		this.kenc = ksenc.clone();
		this.kmac = ksmac.clone();
		this.ssc = initialSSC.clone();
	}

	/** Transforma un Comando APDU en claro a Comando APDU protegido.
	 * @param capdu APDU en claro.
	 * @return CommandApdu APDU protegida.
	 * @throws SecureMessagingException En cualquier error. */
	public CommandApdu wrap(final CommandApdu capdu) throws SecureMessagingException {

		byte lc = 0;
		DO97 do97 = null;
		DO87 do87 = null;

		incrementAtIndex(this.ssc);

		// Enmascara el byte de la clase y hace un padding del comando de cabecera
		final byte[] header = new byte[4];

		// Los primeros 4 bytes de la cabecera son los del Comando APDU
		System.arraycopy(capdu.getBytes(), 0, header, 0, 4);

		// Marca la mensajeria segura con el CLA-Byte
		header[0] = (byte) (header[0] | (byte) 0x0C);

		// Construye el DO87 (parametros de comando)
		if (getAPDUStructure(capdu) == 3 || getAPDUStructure(capdu) == 4) {
			do87 = buildDO87(capdu.getData().clone());
			lc += do87.getEncoded().length;
		}

		// Construye el DO97 (payload de respuesta esperado)
		if (getAPDUStructure(capdu) == 2 || getAPDUStructure(capdu) == 4) {
			do97 = buildDO97(capdu.getLe().intValue());
			lc += do97.getEncoded().length;
		}

		// Construye el DO8E (checksum (MAC))
		final DO8E do8E = buildDO8E(header, do87, do97);
		lc += do8E.getEncoded().length;

		// Construye y devuelve la APDU protegida
		final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		try {
			bOut.write(header);
			bOut.write(lc);
			if (do87 != null) {
				bOut.write(do87.getEncoded());
			}
			if (do97 != null) {
				bOut.write(do97.getEncoded());
			}
			bOut.write(do8E.getEncoded());
			bOut.write(0);
		}
		catch (final IOException e) {
			throw new SecureMessagingException(e);
		}

		return new CommandApdu(bOut.toByteArray());
	}

	/** Obtiene la APDU de respuesta en claro a partir de una APDU protegida.
	 * @param responseApduEncrypted APDU protegida.
	 * @return APDU en claro.
	 * @throws SecureMessagingException En cualquier error. */
	public ResponseApdu unwrap(final ResponseApdu responseApduEncrypted) throws SecureMessagingException {

		DO87 do87 = null;
		DO99 do99 = null;
		DO8E do8E = null;

		incrementAtIndex(this.ssc);

		int pointer = 0;
		final byte[] rapduBytes = responseApduEncrypted.getData();
		final byte[] subArray = new byte[rapduBytes.length];

		while (pointer < rapduBytes.length) {
			System.arraycopy(
				rapduBytes,
				pointer,
				subArray,
				0,
				rapduBytes.length - pointer
			);

			final byte[] encodedBytes;
			try {
				encodedBytes = new Tlv(subArray).getBytes();
			}
			catch (final TlvException e1) {
				throw new SecureMessagingException(
					"Los datos de la APDU protegida no forman un TLV valido: " + e1, e1 //$NON-NLS-1$
				);
			}

			try (
				final ASN1InputStream asn1in = new ASN1InputStream(encodedBytes)
			) {
				switch (encodedBytes[0]) {
					case (byte) 0x87:
						do87 = new DO87();
						do87.fromByteArray(asn1in.readObject().getEncoded());
						break;
					case (byte) 0x99:
						do99 = new DO99();
						do99.fromByteArray(asn1in.readObject().getEncoded());
						break;
					case (byte) 0x8E:
						do8E = new DO8E(asn1in.readObject().getEncoded());
						break;
					default:
						Logger.getLogger("es.gob.jmulticard").warning( //$NON-NLS-1$
							"Encontrada estructura desconocida en la APDU protegida: " + HexUtils.hexify(encodedBytes, false) //$NON-NLS-1$
						);
						break;
				}
			}
			catch (final IOException e) {
				throw new SecureMessagingException(e);
			}
			pointer += encodedBytes.length;
		}

		if (do99 == null || do8E == null) {
			throw new SecureMessagingException(
				"Error desempaquetando el mensaje seguro: DO99 o DO8E no encontrados" // DO99 es obligatorio //$NON-NLS-1$
			);
		}

		// Calcula K (SSC||DO87||DO99)
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			if (do87 != null) {
				bout.write(do87.getEncoded());
			}
			bout.write(do99.getEncoded());
		}
		catch (final IOException e) {
			throw new SecureMessagingException(e);
		}

		final byte[] cc;
		try {
			cc = AmAESCrypto.getMac(bout.toByteArray(), this.ssc, this.kmac, this.cryptoHelper);
		}
		catch (final InvalidKeyException | NoSuchAlgorithmException e1) {
			throw new SecureMessagingException(
				"Error calculando el CMAC: " + e1, e1 //$NON-NLS-1$
			);
		}

		final byte[] do8eData = do8E.getData();

		if (!java.util.Arrays.equals(cc, do8eData)) {
			throw new SecureMessagingException(
				"Checksum incorrecto (CC Calculado = " //$NON-NLS-1$
					+ HexUtils.hexify(cc, false) + ", CC en DO8E = " //$NON-NLS-1$
						+ HexUtils.hexify(do8eData, false) + ")" //$NON-NLS-1$
			);
		}

		// Desencriptar DO87
		final byte[] unwrappedAPDUBytes;
		if (do87 != null) {
			final byte[] do87Data = do87.getData();
			final byte[] data;
			try {
				data = AmAESCrypto.decrypt(do87Data, this.kenc, this.ssc, this.cryptoHelper);
			}
			catch (final AmCryptoException e) {
				throw new SecureMessagingException(e);
			}
			// Construir la respuesta APDU desencriptada
			unwrappedAPDUBytes = new byte[data.length + 2];
			System.arraycopy(data, 0, unwrappedAPDUBytes, 0, data.length);
			final byte[] do99Data = do99.getData();
			System.arraycopy(
				do99Data,
				0,
				unwrappedAPDUBytes,
				data.length,
				do99Data.length
			);
		}
		else {
			unwrappedAPDUBytes = do99.getData().clone();
		}

		return new ResponseApdu(unwrappedAPDUBytes);
	}

	/** Encripta los datos con <code>kenc</code> para construir el DO87.
	 * @param data Datos a encriptar.
	 * @return DO87 Par&aacute;metros del comando.
	 * @throws SecureMessagingException En caso de error en el cifrado. */
	private DO87 buildDO87(final byte[] data) throws SecureMessagingException  {
		final byte[] enc_data;
		try {
			enc_data = AmAESCrypto.encrypt(data, this.kenc, this.ssc, this.cryptoHelper);
		}
		catch (final AmCryptoException e) {
			throw new SecureMessagingException(e);
		}
		return new DO87(enc_data);
	}

	private DO8E buildDO8E(final byte[] header, final DO87 do87, final DO97 do97) throws SecureMessagingException {

		final ByteArrayOutputStream m = new ByteArrayOutputStream();

		/* Evita la cabecera doble padding: Solo si do87 o do97
		 * estan presentes se le asigna un padding a la cabecera.
		 * De lo contrario solo se hace padding en el calculo del MAC */
		try {
			if (do87 != null || do97 != null) {
				m.write(AmAESCrypto.addPadding(header));
			}
			else {
				m.write(header);
			}

			if (do87 != null) {
				m.write(do87.getEncoded());
			}
			if (do97 != null) {
				m.write(do97.getEncoded());
			}
		}
		catch (final IOException e) {
			throw new SecureMessagingException(e);
		}

		try {
			return new DO8E(AmAESCrypto.getMac(m.toByteArray(), this.ssc, this.kmac, this.cryptoHelper));
		}
		catch (final InvalidKeyException | NoSuchAlgorithmException e) {
			throw new SecureMessagingException(
				"Error calculando el CMAC: " + e, e //$NON-NLS-1$
			);
		}
	}

	private static DO97 buildDO97(final int le) {
		return new DO97(le);
	}

	/** Determina el equivalente a la APDU (ISO/IEC 7816-3 Cap&iacute;tulo 12&#46;1).
	 * @param capdu Comando APDU.
	 * @return Tipo de estructura (1 = CASE1, etc.). */
	private static byte getAPDUStructure(final CommandApdu capdu) {
		final byte[] cardcmd = capdu.getBytes();

		if (cardcmd.length == 4) {
			return 1;
		}
		if (cardcmd.length == 5) {
			return 2;
		}
		if (cardcmd.length == 5 + (cardcmd[4]&0xff) && cardcmd[4] != 0) {
			return 3;
		}
		if (cardcmd.length == 6 + (cardcmd[4]&0xff) && cardcmd[4] != 0) {
			return 4;
		}
		if (cardcmd.length == 7 && cardcmd[4] == 0) {
			return 5;
		}
		if (cardcmd.length == 7 + (cardcmd[5]&0xff) * 256 + (cardcmd[6]&0xff)
				&& cardcmd[4] == 0 && (cardcmd[5] != 0 || cardcmd[6] != 0)) {
			return 6;
		}
		if (cardcmd.length == 9 + (cardcmd[5]&0xff) * 256 + (cardcmd[6]&0xff)
				&& cardcmd[4] == 0 && (cardcmd[5] != 0 || cardcmd[6] != 0)) {
			return 7;
		}
		return 0;
	}

	private static void incrementAtIndex(final byte[] array) {
		final byte[] result = new BigInteger(array).add(BigInteger.ONE).toByteArray();
		if (result.length > array.length) {
			Arrays.fill(array, (byte)0);
		}
		else {
			final int lengthA = array.length;
			final int lengthR = result.length;
			for (int i = 0; i < lengthR; i++) {
				array[lengthA - 1 - i] = result[lengthR - 1 - i];
			}
		}
	}
}
