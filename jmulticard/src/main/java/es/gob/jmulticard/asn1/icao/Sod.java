package es.gob.jmulticard.asn1.icao;

import java.io.IOException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import es.gob.jmulticard.CryptoHelper;
import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.JmcLogger;
import es.gob.jmulticard.asn1.Asn1Exception;
import es.gob.jmulticard.asn1.DecoderObject;
import es.gob.jmulticard.asn1.Tlv;
import es.gob.jmulticard.asn1.TlvException;

/** SOD de ICAO 9303.
 * La implementaci&oacute;n interna se apoya en BouncyCastle.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class Sod extends DecoderObject {

	private final CryptoHelper cryptoHelper;

	private static final byte TAG = 0x77;

	private byte[] ldsSecurityObjectBytes = null;
	private LdsSecurityObject ldsSecurityObject = null;
	private X509Certificate[] certificateChain = null;

	/** Constructor.
	 * @param ch Clase de utilidad para operaciones criptogr&aacute;ficas. */
	public Sod(final CryptoHelper ch) {
		cryptoHelper = ch;
	}

	@Override
	protected void decodeValue() throws Asn1Exception, TlvException {
		final Tlv tlv = new Tlv(getBytes());
		checkTag(tlv.getTag());
	}

	/** Valida la firma electr&oacute;nica del SOD.
	 * @throws TlvException Si el SOD no es un TLV correctamente formado.
	 * @throws SignatureException Si la firma no es v&aacute;lida.
	 * @throws CertificateException Si hay problemas relacionados con los certificados de firma.
	 * @throws IOException Si los datos proporcionados no son una firma CMS/PKCS#7 bien formada.
	 * @throws Asn1Exception Si el SOD no contiene un <code>LDSSecurityObject</code> v&aacute;lido. */
	public void validateSignature() throws TlvException,
	                                       SignatureException,
	                                       CertificateException,
	                                       IOException,
	                                       Asn1Exception {

		final Tlv tlv = new Tlv(getBytes());

		certificateChain = cryptoHelper.validateCmsSignature(tlv.getValue());

		ldsSecurityObjectBytes = cryptoHelper.getCmsSignatureSignedContent(tlv.getValue());
		ldsSecurityObject = new LdsSecurityObject();
		ldsSecurityObject.setDerValue(ldsSecurityObjectBytes);
	}

	@Override
	protected byte getDefaultTag() {
		return TAG;
	}

	/** Obtiene la codificaci&oacute;n binaria del LDSSecurityObject.
	 * La obtenci&oacute;n desencadena una validaci&oacute;n de la firma
	 * electr&oacute;nica del SOD.
	 * @return Codificaci&oacute;n binaria del LDSSecurityObject.
	 * @throws IOException Si se encunetra alguna estructura ASN&#46;1 mal formada.
	 * @throws CertificateException Si los certificados de firma del SOD presentan problemas.
	 * @throws SignatureException Si la firma del SOD es inv&aacute;lida o presenta problemas.
	 * @throws TlvException Si el SOD del documento no es un TLV v&aacute;lido.
	 * @throws Asn1Exception Si el SOD no contiene un <code>LDSSecurityObject</code> v&aacute;lido. */
	public byte[] getLdsSecurityObjectBytes() throws SignatureException,
	                                                 CertificateException,
	                                                 TlvException,
	                                                 IOException,
	                                                 Asn1Exception {
		if (ldsSecurityObjectBytes == null) {
			validateSignature();
		}
		return ldsSecurityObjectBytes;
	}

	/** Obtiene el <code>LDSSecurityObject</code>.
	 * La obtenci&oacute;n desencadena una validaci&oacute;n de la firma
	 * electr&oacute;nica del SOD.
	 * @return LDSSecurityObject.
	 * @throws TlvException Si el SOD del documento no es un TLV v&aacute;lido.
	 * @throws IOException Si no se puede construir el <code>LDSSecurityObject</code>.
	 * @throws Asn1Exception Si los datos encontrados no conforman un
	 *         <code>LDSSecurityObject</code> v&aacute;lido.
	 * @throws CertificateException Si los certificados de firma del SOD presentan problemas.
	 * @throws SignatureException Si la firma del SOD es inv&aacute;lida o presenta problemas. */
	public LdsSecurityObject getLdsSecurityObject() throws TlvException, Asn1Exception, IOException, SignatureException, CertificateException {
		if (ldsSecurityObject == null) {
			validateSignature();
		}
		return ldsSecurityObject;
	}

	/** Obtiene la cadena de certificados del firmante del LDSSecurityObject.
	 * La obtenci&oacute;n desencadena una validaci&oacute;n de la firma
	 * electr&oacute;nica del SOD.
	 * @return Cadena de certificados del firmante del LDSSecurityObject.
	 * @throws TlvException Si el SOD del documento no es un TLV v&aacute;lido.
	 * @throws Asn1Exception Si el SOD del documento no es un tipo ASN&#46;1 v&aacute;lido.
	 * @throws IOException Si se encunetra alguna estructura ASN&#46;1 mal formada.
	 * @throws CertificateException Si los certificados de firma del SOD presentan problemas.
	 * @throws SignatureException Si la firma del SOD es inv&aacute;lida o presenta problemas. */
	public X509Certificate[] getCertificateChain() throws TlvException, Asn1Exception, SignatureException, CertificateException, IOException {
		if (certificateChain == null) {
			validateSignature();
		}
		return certificateChain.clone();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("SOD ICAO"); //$NON-NLS-1$
		try {
			sb.append(
				"\nFirmado por: " + getCertificateChain()[0].getSubjectX500Principal() //$NON-NLS-1$
			);
		}
		catch (final Exception e) {
			JmcLogger.warning(
				"No se ha podido obtener la cadena de certificados de firma del SOD: " + e //$NON-NLS-1$
			);
			return sb.toString();
		}
		sb.append("\n  Con huellas para los siguientes grupos de datos\n"); //$NON-NLS-1$

		for (final DataGroupHash dgh : ldsSecurityObject.getDataGroupHashes()) {
			sb.append("    DG"); //$NON-NLS-1$
			sb.append(dgh.getDataGroupNumber());
			sb.append(" = "); //$NON-NLS-1$
			sb.append(HexUtils.hexify(dgh.getDataGroupHashValue(), false));
			sb.append('\n');
		}

		return sb.toString();
	}
}
