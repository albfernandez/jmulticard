package es.gob.jmulticard.jse.provider.ceres;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;

import es.gob.jmulticard.card.CryptoCard;
import es.gob.jmulticard.card.fnmt.ceres.Ceres;
import es.gob.jmulticard.card.fnmt.ceres.CeresPrivateKeyReference;

/** Clave privada de una tarjeta FNMT-RCM-CERES. La clase no contiene la clave privada en s&iacute;, sino
 * una referencia a ella y una referencia a la propia tarjeta.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CeresPrivateKey implements RSAPrivateKey {

	private static final long serialVersionUID = 4403051294889801855L;

	/** Instancia de la tarjeta donde reside la clave. */
	private final transient Ceres ceres;

	/** Referencia a la clave dentro de la tarjeta. */
	private final transient CeresPrivateKeyReference keyRef;

	/** M&oacute;dulo de la clave privada.
	 * Al ser la clave privada interna a la tarjeta, este dato se obtiene de la p&uacute;blica (es igual). */
	private final BigInteger modulus;

	/** Crea una clave privada de tarjeta FNMT-RCM-CERES.
	 * @param keyReference Referencia a la clave privada de tarjeta FNMT-RCM-CERES.
	 * @param card Tarjeta a la cual pertenece esta clave.
	 * @param mod M&oacute;dulo de la clave privada. */
	CeresPrivateKey(final CeresPrivateKeyReference keyReference, final Ceres card, final BigInteger mod) {
		keyRef = keyReference;
		ceres = card;
		modulus = mod;
	}

	@Override
	public String getAlgorithm() {
		return "RSA"; //$NON-NLS-1$
	}

	/** Obtiene la tarjeta capaz de operar con esta clave.
	 * @return Tarjeta capaz de operar con esta clave. */
	CryptoCard getCryptoCard() {
		return ceres;
	}

	@Override
	public byte[] getEncoded() {
		return null;
	}

	@Override
	public String getFormat() {
		return null;
	}

	/** Recupera la referencia de la clave.
	 * @return Referencia de la clave. */
	CeresPrivateKeyReference getReference() {
		return keyRef;
	}

	/** No soportado. */
	@Override
	public BigInteger getModulus() {
		return modulus;
	}

	/** No soportado. */
	@Override
	public BigInteger getPrivateExponent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return keyRef.toString();
	}

	/** Serializaci&oacute;n no soportada, lanza un <code>NotSerializableException</code>.
	 * @param outStream No se usa.
	 * @throws IOException No se lanza, siempre lanza un <code>NotSerializableException</code>. */
	@SuppressWarnings({ "static-method" })
	private void writeObject(final ObjectOutputStream outStream) throws IOException {
		throw new NotSerializableException();
	}
}