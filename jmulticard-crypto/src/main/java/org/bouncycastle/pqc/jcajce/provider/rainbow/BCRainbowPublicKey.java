package org.bouncycastle.pqc.jcajce.provider.rainbow;

import java.security.PublicKey;

import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.pqc.asn1.PQCObjectIdentifiers;
import org.bouncycastle.pqc.asn1.RainbowPublicKey;
import org.bouncycastle.pqc.crypto.rainbow.RainbowParameters;
import org.bouncycastle.pqc.crypto.rainbow.RainbowPublicKeyParameters;
import org.bouncycastle.pqc.crypto.rainbow.util.RainbowUtil;
import org.bouncycastle.pqc.jcajce.provider.util.KeyUtil;
import org.bouncycastle.pqc.jcajce.spec.RainbowPublicKeySpec;
import org.bouncycastle.util.Arrays;

/**
 * This class implements CipherParameters and PublicKey.
 * <p>
 * The public key in Rainbow consists of n - v1 polynomial components of the
 * private key's F and the field structure of the finite field k.
 * </p><p>
 * The quadratic (or mixed) coefficients of the polynomials from the public key
 * are stored in the 2-dimensional array in lexicographical order, requiring n *
 * (n + 1) / 2 entries for each polynomial. The singular terms are stored in a
 * 2-dimensional array requiring n entries per polynomial, the scalar term of
 * each polynomial is stored in a 1-dimensional array.
 * </p><p>
 * More detailed information on the public key is to be found in the paper of
 * Jintai Ding, Dieter Schmidt: Rainbow, a New Multivariable Polynomial
 * Signature Scheme. ACNS 2005: 164-175 (https://dx.doi.org/10.1007/11496137_12)
 * </p>
 */
public class BCRainbowPublicKey
    implements PublicKey
{
    private static final long serialVersionUID = 1L;

    private final short[][] coeffquadratic;
    private final short[][] coeffsingular;
    private final short[] coeffscalar;
    private final int docLength; // length of possible document to sign

    private RainbowParameters rainbowParams;

    /**
     * Constructor
     *
     * @param docLength Document length
     * @param coeffQuadratic coeffQuadratic
     * @param coeffSingular coeffSingular
     * @param coeffScalar coeffScalar
     */
    public BCRainbowPublicKey(final int docLength,
                              final short[][] coeffQuadratic, final short[][] coeffSingular,
                              final short[] coeffScalar)
    {
        this.docLength = docLength;
        this.coeffquadratic = coeffQuadratic;
        this.coeffsingular = coeffSingular;
        this.coeffscalar = coeffScalar;
    }

    /**
     * Constructor (used by the {@link RainbowKeyFactorySpi}).
     *
     * @param keySpec a {@link RainbowPublicKeySpec}
     */
    public BCRainbowPublicKey(final RainbowPublicKeySpec keySpec)
    {
        this(keySpec.getDocLength(), keySpec.getCoeffQuadratic(), keySpec
            .getCoeffSingular(), keySpec.getCoeffScalar());
    }

    public BCRainbowPublicKey(
        final RainbowPublicKeyParameters params)
    {
        this(params.getDocLength(), params.getCoeffQuadratic(), params.getCoeffSingular(), params.getCoeffScalar());
    }

    /**
     * @return the docLength
     */
    public int getDocLength()
    {
        return this.docLength;
    }

    /**
     * @return the coeffQuadratic
     */
    public short[][] getCoeffQuadratic()
    {
        return this.coeffquadratic;
    }

    /**
     * @return the coeffSingular
     */
    public short[][] getCoeffSingular()
    {
        final short[][] copy = new short[this.coeffsingular.length][];

        for (int i = 0; i != this.coeffsingular.length; i++)
        {
            copy[i] = Arrays.clone(this.coeffsingular[i]);
        }

        return copy;
    }


    /**
     * @return the coeffScalar
     */
    public short[] getCoeffScalar()
    {
        return Arrays.clone(this.coeffscalar);
    }

    /**
     * Compare this Rainbow public key with another object.
     *
     * @param other the other object
     * @return the result of the comparison
     */
    @Override
	public boolean equals(final Object other)
    {
        if (other == null || !(other instanceof BCRainbowPublicKey))
        {
            return false;
        }
        final BCRainbowPublicKey otherKey = (BCRainbowPublicKey)other;

        return this.docLength == otherKey.getDocLength()
            && RainbowUtil.equals(this.coeffquadratic, otherKey.getCoeffQuadratic())
            && RainbowUtil.equals(this.coeffsingular, otherKey.getCoeffSingular())
            && RainbowUtil.equals(this.coeffscalar, otherKey.getCoeffScalar());
    }

    @Override
	public int hashCode()
    {
        int hash = this.docLength;

        hash = hash * 37 + Arrays.hashCode(this.coeffquadratic);
        hash = hash * 37 + Arrays.hashCode(this.coeffsingular);
        hash = hash * 37 + Arrays.hashCode(this.coeffscalar);

        return hash;
    }

    /**
     * @return name of the algorithm - "Rainbow"
     */
    @Override
	public final String getAlgorithm()
    {
        return "Rainbow";
    }

    @Override
	public String getFormat()
    {
        return "X.509";
    }

    @Override
	public byte[] getEncoded()
    {
        final RainbowPublicKey key = new RainbowPublicKey(this.docLength, this.coeffquadratic, this.coeffsingular, this.coeffscalar);
        final AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(PQCObjectIdentifiers.rainbow, DERNull.INSTANCE);

        return KeyUtil.getEncodedSubjectPublicKeyInfo(algorithmIdentifier, key);
    }
}
