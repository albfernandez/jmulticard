package test.es.gob.jmulticard;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.util.Enumeration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import es.gob.jmulticard.jse.provider.DnieProvider;
import es.gob.jmulticard.jse.provider.ProviderUtil;

/** Pruebas de firma consecutiva en DNIe 100% Java.
 * @author Sergio Mart&iacute;nez Rico. */
final class TestDoubleSign {

	private static final char[] PASSWORD = "password".toCharArray(); //$NON-NLS-1$

	/** Main para pruebas.
	 * @param args No se usa.
	 * @throws Exception En cualquier error. */
	public static void main(final String[] args) throws Exception {
		new TestDoubleSign().testDoubleSign();
	}

	@SuppressWarnings("static-method")
	@Test
	@Disabled("Necesita DNI")
	void testDoubleSign() throws Exception {

		final Provider p = new DnieProvider(ProviderUtil.getDefaultConnection());
		Security.addProvider(p);
		final KeyStore ks = KeyStore.getInstance("DNI"); //$NON-NLS-1$
		ks.load(null, PASSWORD);
		final Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) {
			System.out.println(aliases.nextElement());
		}

		Signature signature = Signature.getInstance("SHA1withRSA"); //$NON-NLS-1$
		signature.initSign((PrivateKey) ks.getKey("CertFirmaDigital", PASSWORD)); //$NON-NLS-1$
		signature.update("Hola Mundo!!".getBytes()); //$NON-NLS-1$
		signature.sign();
		System.out.println("Primera firma generada correctamente"); //$NON-NLS-1$

		signature = Signature.getInstance("SHA1withRSA"); //$NON-NLS-1$
		signature.initSign((PrivateKey) ks.getKey("CertFirmaDigital", PASSWORD)); //$NON-NLS-1$
		signature.update("Hola Mundo 2!!".getBytes()); //$NON-NLS-1$
		Assertions.assertNotNull(signature.sign());

		System.out.println("Segunda firma generada correctamente"); //$NON-NLS-1$
	}
}
