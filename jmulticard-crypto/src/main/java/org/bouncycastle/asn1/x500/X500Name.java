package org.bouncycastle.asn1.x500;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.style.BCStyle;

/**
 * The X.500 Name object.
 * <pre>
 *     Name ::= CHOICE {
 *                       RDNSequence }
 *
 *     RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
 *
 *     RelativeDistinguishedName ::= SET SIZE (1..MAX) OF AttributeTypeAndValue
 *
 *     AttributeTypeAndValue ::= SEQUENCE {
 *                                   type  OBJECT IDENTIFIER,
 *                                   value ANY }
 * </pre>
 */
public class X500Name
    extends ASN1Object
    implements ASN1Choice
{
    private static X500NameStyle    defaultStyle = BCStyle.INSTANCE;

    private boolean                 isHashCodeCalculated;
    private int                     hashCodeValue;

    private X500NameStyle style;
    private final RDN[] rdns;
    private DERSequence rdnSeq;

    /**
     * @param style Style.
     * @param name Name.
     * @deprecated use the getInstance() method that takes a style.
     */
    @Deprecated
	public X500Name(final X500NameStyle style, final X500Name name)
    {
        this.style = style;
        this.rdns = name.rdns;
        this.rdnSeq = name.rdnSeq;
    }

    /**
     * Return a X500Name based on the passed in tagged object.
     *
     * @param obj tag object holding name.
     * @param explicit true if explicitly tagged false otherwise.
     * @return the X500Name
     */
    public static X500Name getInstance(
        final ASN1TaggedObject obj,
        final boolean          explicit)
    {
        // must be true as choice item
        return getInstance(ASN1Sequence.getInstance(obj, true));
    }

    public static X500Name getInstance(
        final Object  obj)
    {
        if (obj instanceof X500Name)
        {
            return (X500Name)obj;
        }
        else if (obj != null)
        {
            return new X500Name(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    public static X500Name getInstance(
        final X500NameStyle style,
        final Object        obj)
    {
        if (obj instanceof X500Name)
        {
            return new X500Name(style, (X500Name)obj);
        }
        else if (obj != null)
        {
            return new X500Name(style, ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    /**
     * Constructor from ASN1Sequence
     *
     * the principal will be a list of constructed sets, each containing an (OID, String) pair.
     */
    private X500Name(
        final ASN1Sequence  seq)
    {
        this(defaultStyle, seq);
    }

    private X500Name(
        final X500NameStyle style,
        final ASN1Sequence  seq)
    {
        this.style = style;
        this.rdns = new RDN[seq.size()];

        boolean inPlace = true;

        int index = 0;
        for (final Object element : seq) {
            final RDN rdn = RDN.getInstance(element);
            inPlace &= rdn == element;
            this.rdns[index++] = rdn;
        }

        if (inPlace)
        {
            this.rdnSeq = DERSequence.convert(seq);
        }
        else
        {
            this.rdnSeq = new DERSequence(this.rdns);
        }
    }

    public X500Name(
        final RDN[] rDNs)
    {
        this(defaultStyle, rDNs);
    }

    public X500Name(
        final X500NameStyle style,
        final RDN[]         rDNs)
    {
        this.style = style;
        this.rdns = rDNs.clone();
        this.rdnSeq = new DERSequence(this.rdns);
    }

    public X500Name(
        final String dirName)
    {
        this(defaultStyle, dirName);
    }

    public X500Name(
        final X500NameStyle style,
        final String        dirName)
    {
        this(style.fromString(dirName));

        this.style = style;
    }

    /**
     * return an array of RDNs in structure order.
     *
     * @return an array of RDN objects.
     */
    public RDN[] getRDNs()
    {
        return this.rdns.clone();
    }

    /**
     * return an array of OIDs contained in the attribute type of each RDN in structure order.
     *
     * @return an array, possibly zero length, of ASN1ObjectIdentifiers objects.
     */
    public ASN1ObjectIdentifier[] getAttributeTypes()
    {
        final int count = this.rdns.length;
		int totalSize = 0;
        for (int i = 0; i < count; ++i)
        {
            final RDN rdn = this.rdns[i];
            totalSize += rdn.size();
        }

        final ASN1ObjectIdentifier[] oids = new ASN1ObjectIdentifier[totalSize];
        int oidsOff = 0;
        for (int i = 0; i < count; ++i)
        {
            final RDN rdn = this.rdns[i];
            oidsOff += rdn.collectAttributeTypes(oids, oidsOff);
        }
        return oids;
    }

    /**
     * return an array of RDNs containing the attribute type given by OID in structure order.
     *
     * @param attributeType the type OID we are looking for.
     * @return an array, possibly zero length, of RDN objects.
     */
    public RDN[] getRDNs(final ASN1ObjectIdentifier attributeType)
    {
        RDN[] res = new RDN[this.rdns.length];
        int count = 0;

        for (final RDN rdn : this.rdns) {
            if (rdn.containsAttributeType(attributeType))
            {
                res[count++] = rdn;
            }
        }

        if (count < res.length)
        {
            final RDN[] tmp = new RDN[count];
            System.arraycopy(res, 0, tmp, 0, tmp.length);
            res = tmp;
        }

        return res;
    }

    @Override
	public ASN1Primitive toASN1Primitive()
    {
        return this.rdnSeq;
    }

    @Override
	public int hashCode()
    {
        if (this.isHashCodeCalculated)
        {
            return this.hashCodeValue;
        }

        this.isHashCodeCalculated = true;

        this.hashCodeValue = this.style.calculateHashCode(this);

        return this.hashCodeValue;
    }

    /**
     * test for equality - note: case is ignored.
     */
    @Override
	public boolean equals(final Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (!(obj instanceof X500Name) && !(obj instanceof ASN1Sequence))
        {
            return false;
        }

        final ASN1Primitive derO = ((ASN1Encodable)obj).toASN1Primitive();

        if (toASN1Primitive().equals(derO))
        {
            return true;
        }

        try
        {
            return this.style.areEqual(this, new X500Name(ASN1Sequence.getInstance(((ASN1Encodable)obj).toASN1Primitive())));
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    @Override
	public String toString()
    {
        return this.style.toString(this);
    }

    /**
     * Set the default style for X500Name construction.
     *
     * @param style  an X500NameStyle
     */
    public static void setDefaultStyle(final X500NameStyle style)
    {
        if (style == null)
        {
            throw new NullPointerException("cannot set style to null");
        }

        defaultStyle = style;
    }

    /**
     * Return the current default style.
     *
     * @return default style for X500Name construction.
     */
    public static X500NameStyle getDefaultStyle()
    {
        return defaultStyle;
    }
}
