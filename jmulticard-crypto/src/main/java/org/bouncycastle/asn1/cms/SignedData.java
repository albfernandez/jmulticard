package org.bouncycastle.asn1.cms;

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.BERTaggedObject;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSequence;

/**
 * <a href="https://tools.ietf.org/html/rfc5652#section-5.1">RFC 5652</a>:
 * <p>
 * A signed data object containing multitude of {@link SignerInfo}s.
 * <pre>
 * SignedData ::= SEQUENCE {
 *     version CMSVersion,
 *     digestAlgorithms DigestAlgorithmIdentifiers,
 *     encapContentInfo EncapsulatedContentInfo,
 *     certificates [0] IMPLICIT CertificateSet OPTIONAL,
 *     crls [1] IMPLICIT CertificateRevocationLists OPTIONAL,
 *     signerInfos SignerInfos
 *   }
 *
 * DigestAlgorithmIdentifiers ::= SET OF DigestAlgorithmIdentifier
 *
 * SignerInfos ::= SET OF SignerInfo
 * </pre>
 * <p>
 * The version calculation uses following ruleset from RFC 5652 section 5.1:
 * <pre>
 * IF ((certificates is present) AND
 *    (any certificates with a type of other are present)) OR
 *    ((crls is present) AND
 *    (any crls with a type of other are present))
 * THEN version MUST be 5
 * ELSE
 *    IF (certificates is present) AND
 *       (any version 2 attribute certificates are present)
 *    THEN version MUST be 4
 *    ELSE
 *       IF ((certificates is present) AND
 *          (any version 1 attribute certificates are present)) OR
 *          (any SignerInfo structures are version 3) OR
 *          (encapContentInfo eContentType is other than id-data)
 *       THEN version MUST be 3
 *       ELSE version MUST be 1
 * </pre>
 */
public class SignedData
    extends ASN1Object
{
    private static final ASN1Integer VERSION_1 = new ASN1Integer(1);
    private static final ASN1Integer VERSION_3 = new ASN1Integer(3);
    private static final ASN1Integer VERSION_4 = new ASN1Integer(4);
    private static final ASN1Integer VERSION_5 = new ASN1Integer(5);

    private final ASN1Integer version;
    private final ASN1Set     digestAlgorithms;
    private final ContentInfo contentInfo;
    private final ASN1Set     signerInfos;
    private final boolean     digsBer;
    private final boolean     sigsBer;

    private ASN1Set     certificates;
    private ASN1Set     crls;
    private boolean     certsBer;
    private boolean     crlsBer;

    /**
     * Return a SignedData object from the given object.
     * <p>
     * Accepted inputs:
     * <ul>
     * <li> null &rarr; null
     * <li> {@link SignedData} object
     * <li> {@link org.bouncycastle.asn1.ASN1Sequence#getInstance(java.lang.Object) ASN1Sequence} input formats with SignedData structure inside
     * </ul>
     *
     * @param o the object we want converted.
     * @return a reference that can be assigned to SignedData (may be null)
     * @throws IllegalArgumentException if the object cannot be converted.
     */
    public static SignedData getInstance(
        final Object  o)
    {
        if (o instanceof SignedData)
        {
            return (SignedData)o;
        }
        else if (o != null)
        {
            return new SignedData(ASN1Sequence.getInstance(o));
        }

        return null;
    }

    public SignedData(
        final ASN1Set     digestAlgorithms,
        final ContentInfo contentInfo,
        final ASN1Set     certificates,
        final ASN1Set     crls,
        final ASN1Set     signerInfos)
    {
        this.version = calculateVersion(contentInfo.getContentType(), certificates, crls, signerInfos);
        this.digestAlgorithms = digestAlgorithms;
        this.contentInfo = contentInfo;
        this.certificates = certificates;
        this.crls = crls;
        this.signerInfos = signerInfos;
        this.digsBer = digestAlgorithms instanceof BERSet;
        this.crlsBer = crls instanceof BERSet;
        this.certsBer = certificates instanceof BERSet;
        this.sigsBer = signerInfos instanceof BERSet;
    }


    private ASN1Integer calculateVersion(
        final ASN1ObjectIdentifier contentOid,
        final ASN1Set certs,
        final ASN1Set crls,
        final ASN1Set signerInfs)
    {
        boolean otherCert = false;
        boolean otherCrl = false;
        boolean attrCertV1Found = false;
        boolean attrCertV2Found = false;

        if (certs != null)
        {
            for (final Object obj : certs) {
                if (obj instanceof ASN1TaggedObject)
                {
                    final ASN1TaggedObject tagged = ASN1TaggedObject.getInstance(obj);

                    if (tagged.getTagNo() == 1)
                    {
                        attrCertV1Found = true;
                    }
                    else if (tagged.getTagNo() == 2)
                    {
                        attrCertV2Found = true;
                    }
                    else if (tagged.getTagNo() == 3)
                    {
                        otherCert = true;
                    }
                }
            }
        }

        if (otherCert)
        {
            return new ASN1Integer(5);
        }

        if (crls != null)         // no need to check if otherCert is true
        {
            for (final Object obj : crls) {
                if (obj instanceof ASN1TaggedObject)
                {
                    otherCrl = true;
                }
            }
        }

        if (otherCrl)
        {
            return VERSION_5;
        }

        if (attrCertV2Found)
        {
            return VERSION_4;
        }

        if (attrCertV1Found || checkForVersion3(signerInfs) || !CMSObjectIdentifiers.data.equals(contentOid))
        {
            return VERSION_3;
        }

        return VERSION_1;
    }

    private boolean checkForVersion3(final ASN1Set signerInfs)
    {
        for (final Object signerInf : signerInfs) {
            final SignerInfo s = SignerInfo.getInstance(signerInf);

            if (s.getVersion().hasValue(3))
            {
                return true;
            }
        }

        return false;
    }

    private SignedData(
        final ASN1Sequence seq)
    {
        final Enumeration     e = seq.getObjects();

        this.version = ASN1Integer.getInstance(e.nextElement());
        this.digestAlgorithms = (ASN1Set)e.nextElement();
        this.contentInfo = ContentInfo.getInstance(e.nextElement());

        ASN1Set sigInfs = null;
        while (e.hasMoreElements())
        {
            final ASN1Primitive o = (ASN1Primitive)e.nextElement();

            //
            // an interesting feature of SignedData is that there appear
            // to be varying implementations...
            // for the moment we ignore anything which doesn't fit.
            //
            if (o instanceof ASN1TaggedObject)
            {
                final ASN1TaggedObject tagged = (ASN1TaggedObject)o;

                switch (tagged.getTagNo())
                {
                case 0:
                    this.certsBer = tagged instanceof BERTaggedObject;
                    this.certificates = ASN1Set.getInstance(tagged, false);
                    break;
                case 1:
                    this.crlsBer = tagged instanceof BERTaggedObject;
                    this.crls = ASN1Set.getInstance(tagged, false);
                    break;
                default:
                    throw new IllegalArgumentException("unknown tag value " + tagged.getTagNo());
                }
            }
            else
            {
                if (!(o instanceof ASN1Set))
                {
                    throw new IllegalArgumentException("SET expected, not encountered");
                }
                sigInfs = (ASN1Set)o;
            }
        }

        if (sigInfs == null)
        {
            throw new IllegalArgumentException("signerInfos not set");
        }

        this.signerInfos = sigInfs;
        this.digsBer = this.digestAlgorithms instanceof BERSet;
        this.sigsBer = this.signerInfos instanceof BERSet;
    }

    public ASN1Integer getVersion()
    {
        return this.version;
    }

    public ASN1Set getDigestAlgorithms()
    {
        return this.digestAlgorithms;
    }

    public ContentInfo getEncapContentInfo()
    {
        return this.contentInfo;
    }

    public ASN1Set getCertificates()
    {
        return this.certificates;
    }

    public ASN1Set getCRLs()
    {
        return this.crls;
    }

    public ASN1Set getSignerInfos()
    {
        return this.signerInfos;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     */
    @Override
	public ASN1Primitive toASN1Primitive()
    {
        final ASN1EncodableVector v = new ASN1EncodableVector(6);

        v.add(this.version);
        v.add(this.digestAlgorithms);
        v.add(this.contentInfo);

        if (this.certificates != null)
        {
            if (this.certsBer)
            {
                v.add(new BERTaggedObject(false, 0, this.certificates));
            }
            else
            {
                v.add(new DERTaggedObject(false, 0, this.certificates));
            }
        }

        if (this.crls != null)
        {
            if (this.crlsBer)
            {
                v.add(new BERTaggedObject(false, 1, this.crls));
            }
            else
            {
                v.add(new DERTaggedObject(false, 1, this.crls));
            }
        }

        v.add(this.signerInfos);

        if (!this.contentInfo.isDefiniteLength() || this.digsBer || this.sigsBer || this.crlsBer || this.certsBer)
        {
            return new BERSequence(v);
        }
        else
        {
            return new DLSequence(v);
        }
    }
}
