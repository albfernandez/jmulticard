package test.es.gob.jmulticard.icao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import es.gob.jmulticard.card.icao.vdsned.Vdsned;

/** Pruebas de <i>Visible Digital Seals for Non-Electronic Documents</i> de ICAO.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
final class TestVdsned {

	private static final byte[] SAMPLE = {

		// Cabecera
		(byte) 0xdc, // Magic
		(byte) 0x03, // Version 4
		(byte) 0xd9, (byte) 0xc5, // Pais = UTO (Utopia)
		(byte) 0x6d, (byte) 0x15, (byte) 0x22, (byte) 0x4c, (byte) 0x5a, (byte) 0x8c, // Certificate Authority and Certificate Reference (DE01FFAFF)
		(byte) 0x31, (byte) 0x9f, (byte) 0x27, // Document Issue Date (25th of March, 2007)
		(byte) 0x31, (byte) 0xc6, (byte) 0x37, // Signature Creation Date (26th of March, 2007)
		(byte) 0x5d, // Document Feature Definition Reference (93)
		(byte) 0x01, // Document Type Category (1)

		// Mensaje
		(byte) 0x02, (byte) 0x2c, (byte) 0xdd, (byte) 0x52, (byte) 0x13, (byte) 0x4a, (byte) 0x74, (byte) 0xda, // MRZ-B, linea 1
		(byte) 0x13, (byte) 0x47, (byte) 0xc6, (byte) 0xfe, (byte) 0xd9, (byte) 0x5c, (byte) 0xb8, (byte) 0x9f,
		(byte) 0x9f, (byte) 0xce, (byte) 0x13, (byte) 0x3c, (byte) 0x13, (byte) 0x3c, (byte) 0x13, (byte) 0x3c,

		(byte) 0x13, (byte) 0x3c, (byte) 0x20, (byte) 0x38, (byte) 0x33, (byte) 0x73, (byte) 0x4a, (byte) 0xaf, // MRZ-B, linea 2
		(byte) 0x47, (byte) 0xf0, (byte) 0xc3, (byte) 0x2f, (byte) 0x1a, (byte) 0x1e, (byte) 0x20, (byte) 0xeb,
		(byte) 0x26, (byte) 0x25, (byte) 0x39, (byte) 0x3a, (byte) 0xfe, (byte) 0x31,

		(byte) 0x03, (byte) 0x01, (byte) 0x02, // Numero de entradas (2)

		(byte) 0x04, (byte) 0x03, (byte) 0x5a, (byte) 0x00, (byte) 0x00, // Duracion (90 dias)

		(byte) 0x05, (byte) 0x06, (byte) 0x59, (byte) 0xe9, (byte) 0x32, (byte) 0xf9, (byte) 0x26, (byte) 0xc7, // Numero de pasaporte

		// Firma
		(byte) 0xff, (byte) 0x40,
		(byte) 0x56, (byte) 0xbc, (byte) 0xbf, (byte) 0xed, (byte) 0xfd, (byte) 0x2d, (byte) 0xc8, (byte) 0x84,
		(byte) 0x24, (byte) 0x74, (byte) 0x26, (byte) 0xa2, (byte) 0x40, (byte) 0xa7, (byte) 0x06, (byte) 0x8d,
		(byte) 0x32, (byte) 0xb3, (byte) 0x7c, (byte) 0x6c, (byte) 0xe3, (byte) 0x70, (byte) 0xae, (byte) 0xea,
		(byte) 0xb6, (byte) 0x2b, (byte) 0x54, (byte) 0x8b, (byte) 0x5f, (byte) 0xcc, (byte) 0x16, (byte) 0xfa,
		(byte) 0x6a, (byte) 0x09, (byte) 0x8c, (byte) 0xa7, (byte) 0x4c, (byte) 0xb2, (byte) 0x25, (byte) 0x59,
		(byte) 0x43, (byte) 0x5f, (byte) 0xd4, (byte) 0xdb, (byte) 0xde, (byte) 0x70, (byte) 0x9b, (byte) 0x45,
		(byte) 0xf6, (byte) 0xfc, (byte) 0x4c, (byte) 0x85, (byte) 0x0d, (byte) 0xa4, (byte) 0x21, (byte) 0xa6,
		(byte) 0xe7, (byte) 0x5c, (byte) 0xd0, (byte) 0x5a, (byte) 0x88, (byte) 0x70, (byte) 0x7c, (byte) 0xbb
	};

	/** Prueba simple de creaci&oacute;n de un <i>Visible Digital Seals for Non-Electronic Documents</i> de ICAO.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	void testVdsned() throws Exception {
		final Vdsned vdsned = new Vdsned(SAMPLE);
		Assertions.assertNotNull(vdsned);
		System.out.println(vdsned);
	}
}
