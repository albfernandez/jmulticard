/*
 * Controlador Java de la Secretaria de Estado de Administraciones Publicas
 * para el DNI electronico.
 *
 * El Controlador Java para el DNI electronico es un proveedor de seguridad de JCA/JCE
 * que permite el acceso y uso del DNI electronico en aplicaciones Java de terceros
 * para la realizacion de procesos de autenticacion, firma electronica y validacion
 * de firma. Para ello, se implementan las funcionalidades KeyStore y Signature para
 * el acceso a los certificados y claves del DNI electronico, asi como la realizacion
 * de operaciones criptograficas de firma con el DNI electronico. El Controlador ha
 * sido disenado para su funcionamiento independiente del sistema operativo final.
 *
 * Copyright (C) 2012 Direccion General de Modernizacion Administrativa, Procedimientos
 * e Impulso de la Administracion Electronica
 *
 * Este programa es software libre y utiliza un licenciamiento dual (LGPL 2.1+
 * o EUPL 1.1+), lo cual significa que los usuarios podran elegir bajo cual de las
 * licencias desean utilizar el codigo fuente. Su eleccion debera reflejarse
 * en las aplicaciones que integren o distribuyan el Controlador, ya que determinara
 * su compatibilidad con otros componentes.
 *
 * El Controlador puede ser redistribuido y/o modificado bajo los terminos de la
 * Lesser GNU General Public License publicada por la Free Software Foundation,
 * tanto en la version 2.1 de la Licencia, o en una version posterior.
 *
 * El Controlador puede ser redistribuido y/o modificado bajo los terminos de la
 * European Union Public License publicada por la Comision Europea,
 * tanto en la version 1.1 de la Licencia, o en una version posterior.
 *
 * Deberia recibir una copia de la GNU Lesser General Public License, si aplica, junto
 * con este programa. Si no, consultelo en <http://www.gnu.org/licenses/>.
 *
 * Deberia recibir una copia de la European Union Public License, si aplica, junto
 * con este programa. Si no, consultelo en <http://joinup.ec.europa.eu/software/page/eupl>.
 *
 * Este programa es distribuido con la esperanza de que sea util, pero
 * SIN NINGUNA GARANTIA; incluso sin la garantia implicita de comercializacion
 * o idoneidad para un proposito particular.
 */
package es.gob.jmulticard.asn1.der.pkcs1;

import java.io.IOException;

import es.gob.jmulticard.CryptoHelper;
import es.gob.jmulticard.DigestAlgorithm;
import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.asn1.OptionalDecoderObjectElement;
import es.gob.jmulticard.asn1.der.OctectString;
import es.gob.jmulticard.asn1.der.Sequence;

/** Tipo ASN&#46;1 PKCS#1 <i>DigestInfo</i>.
 * <pre>
 *  DigestInfo::=SEQUENCE {
 *    digestAlgorithm  AlgorithmIdentifier,
 *    digest OCTET STRING
 *  }
 * </pre>
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class DigestInfo extends Sequence {

    private static final String SHA1WITHRSA_NORMALIZED_ALGO_NAME   = "SHA1withRSA"; //$NON-NLS-1$
    private static final String SHA256WITHRSA_NORMALIZED_ALGO_NAME = "SHA256withRSA"; //$NON-NLS-1$
    private static final String SHA384WITHRSA_NORMALIZED_ALGO_NAME = "SHA384withRSA"; //$NON-NLS-1$
    private static final String SHA512WITHRSA_NORMALIZED_ALGO_NAME = "SHA512withRSA"; //$NON-NLS-1$

    private static final byte[] SHA1_DIGESTINFO_HEADER = {
        (byte) 0x30, (byte) 0x21, (byte) 0x30, (byte) 0x09, (byte) 0x06, (byte) 0x05, (byte) 0x2B, (byte) 0x0E,
        (byte) 0x03, (byte) 0x02, (byte) 0x1A, (byte) 0x05, (byte) 0x00, (byte) 0x04, (byte) 0x14
    };

    private static final byte[] SHA256_DIGESTINFO_HEADER = {
        (byte) 0x30, (byte) 0x31, (byte) 0x30, (byte) 0x0D, (byte) 0x06, (byte) 0x09, (byte) 0x60, (byte) 0x86,
        (byte) 0x48, (byte) 0x01, (byte) 0x65, (byte) 0x03, (byte) 0x04, (byte) 0x02, (byte) 0x01, (byte) 0x05,
        (byte) 0x00, (byte) 0x04, (byte) 0x20
    };

    private static final byte[] SHA384_DIGESTINFO_HEADER = {
        (byte) 0x30, (byte) 0x41, (byte) 0x30, (byte) 0x0D, (byte) 0x06, (byte) 0x09, (byte) 0x60, (byte) 0x86,
        (byte) 0x48, (byte) 0x01, (byte) 0x65, (byte) 0x03, (byte) 0x04, (byte) 0x02, (byte) 0x02, (byte) 0x05,
        (byte) 0x00, (byte) 0x04, (byte) 0x30
    };

    private static final byte[] SHA512_DIGESTINFO_HEADER = {
        (byte) 0x30, (byte) 0x51, (byte) 0x30, (byte) 0x0D, (byte) 0x06, (byte) 0x09, (byte) 0x60, (byte) 0x86,
        (byte) 0x48, (byte) 0x01, (byte) 0x65, (byte) 0x03, (byte) 0x04, (byte) 0x02, (byte) 0x03, (byte) 0x05,
        (byte) 0x00, (byte) 0x04, (byte) 0x40
    };

    /** @return the sha1DigestinfoHeader */
    private static byte[] getSha1DigestinfoHeader() {
        final byte[] out = new byte[SHA1_DIGESTINFO_HEADER.length];
        System.arraycopy(SHA1_DIGESTINFO_HEADER, 0, out, 0, SHA1_DIGESTINFO_HEADER.length);
        return out;
    }

    /** @return the sha256DigestinfoHeader */
    private static byte[] getSha256DigestinfoHeader() {
        final byte[] out = new byte[SHA256_DIGESTINFO_HEADER.length];
        System.arraycopy(SHA256_DIGESTINFO_HEADER, 0, out, 0, SHA256_DIGESTINFO_HEADER.length);
        return out;
    }

    /** @return the sha384DigestinfoHeader */
    private static byte[] getSha384DigestinfoHeader() {
        final byte[] out = new byte[SHA384_DIGESTINFO_HEADER.length];
        System.arraycopy(SHA384_DIGESTINFO_HEADER, 0, out, 0, SHA384_DIGESTINFO_HEADER.length);
        return out;
    }

    /** @return the sha512DigestinfoHeader */
    private static byte[] getSha512DigestinfoHeader() {
        final byte[] out = new byte[SHA512_DIGESTINFO_HEADER.length];
        System.arraycopy(SHA512_DIGESTINFO_HEADER, 0, out, 0, SHA512_DIGESTINFO_HEADER.length);
        return out;
    }

    /** Construye un objeto ASN&#46;1 PKCS#1 <i>DigestInfo</i>. */
	public DigestInfo() {
        super(
			new OptionalDecoderObjectElement(
				AlgorithmIdentifer.class,
				false
			),
			new OptionalDecoderObjectElement(
				OctectString.class,
				false
			)
		);
    }

	@Override
	public String toString() {
		return "DigestInfo:\n  Datos=" + //$NON-NLS-1$
			HexUtils.hexify(((OctectString)getElementAt(1)).getOctectStringByteValue(), false) +
				"\n  Algoritmo=" + ((AlgorithmIdentifer)getElementAt(0)).toString(); //$NON-NLS-1$
	}

    /** Codifica una estructura <code>DigestInfo</code>.
     * @param signingAlgorithm Algoritmo de huella digital o de firma electr&oacute;nica.
     * @param data Datos de los que obtener la estructura.
     * @param cryptoHelper Manejador de operaciones criptogr&aacute;ficas.
     * @return Estructura <code>DigestInfo</code>.
     * @throws IOException Cuando se produce algun error en la estrucura de la estructura. */
    public static byte[] encode(final String signingAlgorithm,
    		                    final byte[] data,
    		                    final CryptoHelper cryptoHelper) throws IOException {

        final String normalizedSignningAlgorithm = getNormalizedSigningAlgorithm(signingAlgorithm);
        final DigestAlgorithm digestAlgorithm = getDigestAlgorithm(normalizedSignningAlgorithm);
        final byte[] header = selectHeaderTemplate(digestAlgorithm);
        final byte[] md = cryptoHelper.digest(digestAlgorithm, data);

        final byte[] digestInfo = new byte[header.length + md.length];
        System.arraycopy(header, 0, digestInfo, 0, header.length);
        System.arraycopy(md, 0, digestInfo, header.length, md.length);

        return digestInfo;
    }

    /** Normaliza los nombres de algorimo de firma.
     * @param algorithm Nombre de algoritmo.
     * @return Nombre de algoritmo normalizado. */
    private static String getNormalizedSigningAlgorithm(final String algorithm) {
        if (
        	"SHA1".equalsIgnoreCase(algorithm)                   || //$NON-NLS-1$
        	"SHA-1".equalsIgnoreCase(algorithm)                  || //$NON-NLS-1$
        	"SHA".equalsIgnoreCase(algorithm)                    || //$NON-NLS-1$
            "SHAwithRSA".equalsIgnoreCase(algorithm)             || //$NON-NLS-1$
            "SHA-1withRSA".equalsIgnoreCase(algorithm)           || //$NON-NLS-1$
            "SHA1withRSAEncryption".equalsIgnoreCase(algorithm)  || //$NON-NLS-1$
            "SHA-1withRSAEncryption".equalsIgnoreCase(algorithm) || //$NON-NLS-1$
        	SHA1WITHRSA_NORMALIZED_ALGO_NAME.equalsIgnoreCase(algorithm)
        ) {
            return SHA1WITHRSA_NORMALIZED_ALGO_NAME;
        }
		if (
    		"SHA256".equalsIgnoreCase(algorithm)                   || //$NON-NLS-1$
    		"SHA-256".equalsIgnoreCase(algorithm)                  || //$NON-NLS-1$
            "SHA-256withRSA".equalsIgnoreCase(algorithm)           || //$NON-NLS-1$
            "SHA-256withRSAEncryption".equalsIgnoreCase(algorithm) || //$NON-NLS-1$
            "SHA256withRSAEncryption".equalsIgnoreCase(algorithm)  || //$NON-NLS-1$
    		SHA256WITHRSA_NORMALIZED_ALGO_NAME.equalsIgnoreCase(algorithm)
        ) {
            return SHA256WITHRSA_NORMALIZED_ALGO_NAME;
        }
		if (
    		"SHA384".equalsIgnoreCase(algorithm)                   || //$NON-NLS-1$
    		"SHA-384".equalsIgnoreCase(algorithm)                  || //$NON-NLS-1$
            "SHA-384withRSA".equalsIgnoreCase(algorithm)           || //$NON-NLS-1$
            "SHA-384withRSAEncryption".equalsIgnoreCase(algorithm) || //$NON-NLS-1$
            "SHA384withRSAEncryption".equalsIgnoreCase(algorithm)  || //$NON-NLS-1$
    		SHA384WITHRSA_NORMALIZED_ALGO_NAME.equalsIgnoreCase(algorithm)
        ) {
            return SHA384WITHRSA_NORMALIZED_ALGO_NAME;
        }
		if (
    		"SHA512".equalsIgnoreCase(algorithm)                   || //$NON-NLS-1$
    		"SHA-512".equalsIgnoreCase(algorithm)                  || //$NON-NLS-1$
            "SHA-512withRSA".equalsIgnoreCase(algorithm)           || //$NON-NLS-1$
            "SHA-512withRSAEncryption".equalsIgnoreCase(algorithm) || //$NON-NLS-1$
            "SHA512withRSAEncryption".equalsIgnoreCase(algorithm)  || //$NON-NLS-1$
    		SHA512WITHRSA_NORMALIZED_ALGO_NAME.equalsIgnoreCase(algorithm)
        ) {
            return SHA512WITHRSA_NORMALIZED_ALGO_NAME;
        }
        return algorithm;
    }

    /** Selecciona una plantilla con la cabecera del
     * <code>DigestInfo</code> para un algoritmo concreto.
     * @param algorithm Algoritmo del que obtener la plantilla de cabecera.
     * @return Cabecera. */
    private static byte[] selectHeaderTemplate(final DigestAlgorithm algorithm) {
    	switch(algorithm) {
    		case SHA1:
    			return getSha1DigestinfoHeader();
    		case SHA256:
    			return getSha256DigestinfoHeader();
    		case SHA384:
    			return getSha384DigestinfoHeader();
    		case SHA512:
    			return getSha512DigestinfoHeader();
			default:
				throw new IllegalStateException(
		    		"Algoritmo de huella digital no soportado: " + algorithm //$NON-NLS-1$
				);
    	}
    }

    /** Obtiene el algoritmo de huella digital correspondiente a
     * un algoritmo de firma concreto.
     * @param signatureAlgorithm Algoritmo de firma.
     * @return Algoritmo de huella digital o la propia entrada si no se identific&oacute;. */
    private static DigestAlgorithm getDigestAlgorithm(final String signatureAlgorithm) {
        if (SHA1WITHRSA_NORMALIZED_ALGO_NAME.equals(signatureAlgorithm)) {
            return DigestAlgorithm.SHA1;
        }
		if (SHA256WITHRSA_NORMALIZED_ALGO_NAME.equals(signatureAlgorithm)) {
            return DigestAlgorithm.SHA256;
        }
		if (SHA384WITHRSA_NORMALIZED_ALGO_NAME.equals(signatureAlgorithm)) {
            return DigestAlgorithm.SHA384;
        }
		if (SHA512WITHRSA_NORMALIZED_ALGO_NAME.equals(signatureAlgorithm)) {
            return DigestAlgorithm.SHA512;
        }
        throw new IllegalStateException(
    		"Algoritmo de huella digital no soportado para: " + signatureAlgorithm //$NON-NLS-1$
		);
    }
}