package org.bouncycastle.asn1.cms;

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;

/**
 * <a href="https://tools.ietf.org/html/rfc5652#section-10.2.4">RFC 5652</a>: IssuerAndSerialNumber object.
 * <pre>
 * IssuerAndSerialNumber ::= SEQUENCE {
 *     issuer Name,
 *     serialNumber CertificateSerialNumber
 * }
 *
 * CertificateSerialNumber ::= INTEGER  -- See RFC 5280
 * </pre>
 */
public class IssuerAndSerialNumber
    extends ASN1Object
{
    private final X500Name    name;
    private final ASN1Integer  serialNumber;

    /**
     * Return an IssuerAndSerialNumber object from the given object.
     * <p>
     * Accepted inputs:
     * <ul>
     * <li> null &rarr; null
     * <li> {@link IssuerAndSerialNumber} object
     * <li> {@link org.bouncycastle.asn1.ASN1Sequence#getInstance(java.lang.Object) ASN1Sequence} input formats with IssuerAndSerialNumber structure inside
     * </ul>
     *
     * @param obj the object we want converted.
     * @return IssuerAndSerialNumber.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static IssuerAndSerialNumber getInstance(
        final Object  obj)
    {
        if (obj instanceof IssuerAndSerialNumber)
        {
            return (IssuerAndSerialNumber)obj;
        }
        else if (obj != null)
        {
            return new IssuerAndSerialNumber(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    /**
     * @param seq Encoded sequence.
     * @deprecated  use getInstance() method.
     */
    @Deprecated
	public IssuerAndSerialNumber(
        final ASN1Sequence    seq)
    {
        this.name = X500Name.getInstance(seq.getObjectAt(0));
        this.serialNumber = (ASN1Integer)seq.getObjectAt(1);
    }

    public IssuerAndSerialNumber(
        final Certificate certificate)
    {
        this.name = certificate.getIssuer();
        this.serialNumber = certificate.getSerialNumber();
    }

    /**
     * @param certificate Certificate.
     * @deprecated use constructor taking Certificate
     */
    @Deprecated
	public IssuerAndSerialNumber(
        final X509CertificateStructure certificate)
    {
        this.name = certificate.getIssuer();
        this.serialNumber = certificate.getSerialNumber();
    }

    public IssuerAndSerialNumber(
        final X500Name name,
        final BigInteger  serialNumber)
    {
        this.name = name;
        this.serialNumber = new ASN1Integer(serialNumber);
    }

    /**
     * @param name Name
     * @param serialNumber Serial number
     * @deprecated use X500Name constructor
     */
    @Deprecated
	public IssuerAndSerialNumber(
        final X509Name    name,
        final BigInteger  serialNumber)
    {
        this.name = X500Name.getInstance(name);
        this.serialNumber = new ASN1Integer(serialNumber);
    }

    /**
     * @param name Name
     * @param serialNumber Serial number
     * @deprecated use X500Name constructor
     */
    @Deprecated
	public IssuerAndSerialNumber(
        final X509Name    name,
        final ASN1Integer  serialNumber)
    {
        this.name = X500Name.getInstance(name);
        this.serialNumber = serialNumber;
    }

    public X500Name getName()
    {
        return this.name;
    }

	public ASN1Integer getSerialNumber()
    {
        return this.serialNumber;
    }

    @Override
	public ASN1Primitive toASN1Primitive()
    {
        final ASN1EncodableVector v = new ASN1EncodableVector(2);

        v.add(this.name);
        v.add(this.serialNumber);

        return new DERSequence(v);
    }
}
