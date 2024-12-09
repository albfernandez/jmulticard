package org.bouncycastle.asn1;

import java.io.IOException;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

/**
 * ASN.1 VisibleString object encoding ISO 646 (ASCII) character code points 32 to 126.
 * <p>
 * Explicit character set escape sequences are not allowed.
 * </p>
 */
public abstract class ASN1VisibleString
    extends ASN1Primitive
    implements ASN1String
{
    static final ASN1UniversalType TYPE = new ASN1UniversalType(ASN1VisibleString.class, BERTags.VISIBLE_STRING)
    {
        @Override
		ASN1Primitive fromImplicitPrimitive(final DEROctetString octetString)
        {
            return createPrimitive(octetString.getOctets());
        }
    };

    /**
     * Return a Visible String from the passed in object.
     *
     * @param obj an ASN1VisibleString or an object that can be converted into one.
     * @exception IllegalArgumentException if the object cannot be converted.
     * @return an ASN1VisibleString instance, or null
     */
    public static ASN1VisibleString getInstance(
        final Object  obj)
    {
        if (obj == null || obj instanceof ASN1VisibleString)
        {
            return (ASN1VisibleString)obj;
        }
        if (obj instanceof ASN1Encodable)
        {
            final ASN1Primitive primitive = ((ASN1Encodable)obj).toASN1Primitive();
            if (primitive instanceof ASN1VisibleString)
            {
                return (ASN1VisibleString)primitive;
            }
        }
        if (obj instanceof byte[])
        {
            try
            {
                return (ASN1VisibleString)TYPE.fromByteArray((byte[])obj);
            }
            catch (final Exception e)
            {
                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
            }
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * Return a Visible String from a tagged object.
     *
     * @param taggedObject the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     * @return an ASN1VisibleString instance, or null
     */
    public static ASN1VisibleString getInstance(final ASN1TaggedObject taggedObject, final boolean explicit)
    {
        return (ASN1VisibleString)TYPE.getContextInstance(taggedObject, explicit);
    }

    final byte[] contents;

    ASN1VisibleString(final String string)
    {
        this.contents = Strings.toByteArray(string);
    }

    ASN1VisibleString(final byte[] contents, final boolean clone)
    {
        this.contents = clone ? Arrays.clone(contents) : contents;
    }

    @Override
	public final String getString()
    {
        return Strings.fromByteArray(this.contents);
    }

    @Override
	public String toString()
    {
        return getString();
    }

    public final byte[] getOctets()
    {
        return Arrays.clone(this.contents);
    }

    @Override
	final boolean encodeConstructed()
    {
        return false;
    }

    @Override
	final int encodedLength(final boolean withTag)
    {
        return ASN1OutputStream.getLengthOfEncodingDL(withTag, this.contents.length);
    }

    @Override
	final void encode(final ASN1OutputStream out, final boolean withTag) throws IOException
    {
        out.writeEncodingDL(withTag, BERTags.VISIBLE_STRING, this.contents);
    }

    @Override
	final boolean asn1Equals(final ASN1Primitive other)
    {
        if (!(other instanceof ASN1VisibleString))
        {
            return false;
        }

        final ASN1VisibleString that = (ASN1VisibleString)other;

        return Arrays.areEqual(this.contents, that.contents);
    }

    @Override
	public final int hashCode()
    {
        return Arrays.hashCode(this.contents);
    }

    static ASN1VisibleString createPrimitive(final byte[] contents)
    {
        return new DERVisibleString(contents, false);
    }
}
