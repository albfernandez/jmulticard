package test.es.gob.jmulticard.jse.provider.rsacipher;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.smartcardio.CommandAPDU;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.apdu.CommandApdu;
import es.gob.jmulticard.apdu.iso7816eight.PsoSignHashApduCommand;
import es.gob.jmulticard.card.dnie.DniePrivateKeyReference;
import es.gob.jmulticard.jse.provider.DniePrivateKey;
import es.gob.jmulticard.jse.provider.DnieProvider;
import test.es.gob.jmulticard.TestingDnieCallbackHandler;

/** Pruebas de cifrado RSA.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
final class TestCipher {

	private static final String RSA_ECB_PKCS1PADDING = "RSA/ECB/PKCS1Padding"; //$NON-NLS-1$

	private static final String CAN = "630208"; //$NON-NLS-1$
	private static final String PIN = "WJ8d6EzzxDkz"; //$NON-NLS-1$

	/** Alias del certificado de autenticaci&oacute;n del DNIe (siempre el mismo en el DNIe y tarjetas derivadas). */
    private static final String CERT_ALIAS_AUTH = "CertAutenticacion"; //$NON-NLS-1$

	/** Main para pruebas.
	 * @param args No se usa.
	 * @throws Throwable En cualquier error. */
	public static void main(final String[] args) throws Throwable {

		System.out.println("java.vendor: " + System.getProperty("java.vendor")); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println("java.version: " + System.getProperty("java.version")); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println("java.vendor.url: " + System.getProperty("java.vendor.url")); //$NON-NLS-1$ //$NON-NLS-2$

		final Provider provider = new DnieProvider();
		Security.insertProviderAt(provider, 1);

		final Cipher cipher = Cipher.getInstance(RSA_ECB_PKCS1PADDING, provider);

		final KeyStore.Builder builder = KeyStore.Builder.newInstance(
			"DNI", //$NON-NLS-1$
			new DnieProvider(),
			new KeyStore.CallbackHandlerProtection(
				new TestingDnieCallbackHandler(CAN, PIN)
			)
		);
		final KeyStore ks = builder.getKeyStore();

		final Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) {
			System.out.println(aliases.nextElement());
		}

		final DniePrivateKey prK = (DniePrivateKey) ks.getKey(CERT_ALIAS_AUTH, PIN.toCharArray());
		final DniePrivateKeyReference dpkr = prK.getDniePrivateKeyReference();

		System.out.println(dpkr);

		final String data ="HolaManola"; //$NON-NLS-1$

		cipher.init(Cipher.ENCRYPT_MODE, prK);
		final byte[] out = cipher.doFinal(data.getBytes());
		System.out.println(
			HexUtils.hexify(
				out,
				false
			)
		);

		final PublicKey puK = ks.getCertificate(CERT_ALIAS_AUTH).getPublicKey();
		final Cipher cipherDec = Cipher.getInstance(RSA_ECB_PKCS1PADDING);
		cipherDec.init(Cipher.DECRYPT_MODE, puK);
		final byte[] descifrado = cipherDec.doFinal(out);

		System.out.println(
			HexUtils.hexify(
				descifrado,
				false
			)
		);

		System.out.println();
		System.out.println(
			new String(
				descifrado
			)
		);
	}

	private static final char[] KEYSTORE_PWD = "12341234".toCharArray(); //$NON-NLS-1$
	private static final String KEYSTORE_FILE = "/ANF_PF_Activo.pfx"; //$NON-NLS-1$
	private static final String KEYSTORE_TYPE = "PKCS12"; //$NON-NLS-1$
	static final char[] KEYSTORE_FIRST_ENTRY_PWD = "12341234".toCharArray(); //$NON-NLS-1$

	/** Prueba de cifrado usando un PKCS#12 y el proveedor por defecto.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	void testP12KeyCipher() throws Exception {
		final KeyStore ks;
		final PrivateKeyEntry pke;
		final String alias;
        try (
    		InputStream is = TestCipher.class.getResourceAsStream(KEYSTORE_FILE)
		) {
    		ks = KeyStore.getInstance(KEYSTORE_TYPE);
        	ks.load(is, KEYSTORE_PWD);
        	Assertions.assertTrue(ks.aliases().hasMoreElements(), "No se ha cargado el KeyStore o esta vacio"); //$NON-NLS-1$
            alias = ks.aliases().nextElement();
            pke = (PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(KEYSTORE_FIRST_ENTRY_PWD));
        }
        final Cipher cipher = Cipher.getInstance(RSA_ECB_PKCS1PADDING);
		final String data ="HolaManola"; //$NON-NLS-1$
		cipher.init(Cipher.ENCRYPT_MODE, pke.getPrivateKey());
		final byte[] out = cipher.doFinal(data.getBytes());

		System.out.println(
			HexUtils.hexify(
				out,
				false
			)
		);

		final PublicKey puK = ks.getCertificate(alias).getPublicKey();
		final Cipher cipherDec = Cipher.getInstance(RSA_ECB_PKCS1PADDING);
		cipherDec.init(Cipher.DECRYPT_MODE, puK);
		final byte[] descifrado = cipherDec.doFinal(out);

		System.out.println(
			HexUtils.hexify(
				descifrado,
				false
			)
		);

		System.out.println(new String(descifrado));
	}

	/** Prueba de conformaci&oacute;n de APDU PSO Sign Hash. */
	@SuppressWarnings("static-method")
	@Test
	@Disabled("Resultado invariable")
	void testCommandAdpu() {
		final byte[] data = new byte[256];
		new SecureRandom().nextBytes(data);

		final CommandAPDU apu = new CommandAPDU(0x00, (byte) 0x2a, (byte) 0x9e, (byte) 0x9a, data);
		System.out.println(HexUtils.hexify(apu.getBytes(), false));

		final CommandApdu apu2 = new PsoSignHashApduCommand((byte)0x00, data);
		System.out.println(HexUtils.hexify(apu2.getBytes(), false));
	}

	/** Lista los servicios soportados por cada proveedor instalado. */
	@SuppressWarnings("static-method")
	@Test
	@Disabled("Solo para comprobacion manual")
	void testProviderSupp() {
		try {
			for (final Provider provider: Security.getProviders()) {
			  System.out.println(provider.getName());
			  for (final String key: provider.stringPropertyNames()) {
				System.out.println('\t' + key + '\t' + provider.getProperty(key));
			  }
			}
		}
		catch(final Exception e) {
			e.printStackTrace();
			Assertions.fail();
		}
	}
}
