package org.bouncycastle.pqc.jcajce.provider.sphincsplus;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;

public class SPHINCSPlusKeyFactorySpi
    extends KeyFactorySpi
    implements AsymmetricKeyInfoConverter
{
    @Override
	public PrivateKey engineGeneratePrivate(KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof PKCS8EncodedKeySpec)
        {
            // get the DER-encoded Key according to PKCS#8 from the spec
            byte[] encKey = ((PKCS8EncodedKeySpec)keySpec).getEncoded();

            try
            {
                return generatePrivate(PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(encKey)));
            }
            catch (Exception e)
            {
                throw new InvalidKeySpecException(e.toString());
            }
        }

        throw new InvalidKeySpecException("Unsupported key specification: "
            + keySpec.getClass() + ".");
    }

    @Override
	public PublicKey engineGeneratePublic(KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof X509EncodedKeySpec)
        {
            // get the DER-encoded Key according to X.509 from the spec
            byte[] encKey = ((X509EncodedKeySpec)keySpec).getEncoded();

            // decode the SubjectPublicKeyInfo data structure to the pki object
            try
            {
                return generatePublic(SubjectPublicKeyInfo.getInstance(encKey));
            }
            catch (Exception e)
            {
                throw new InvalidKeySpecException(e.toString());
            }
        }

        throw new InvalidKeySpecException("Unknown key specification: " + keySpec + ".");
    }

    @Override
	public final KeySpec engineGetKeySpec(Key key, Class keySpec)
        throws InvalidKeySpecException
    {
        if (key instanceof BCSPHINCSPlusPrivateKey)
        {
            if (PKCS8EncodedKeySpec.class.isAssignableFrom(keySpec))
            {
                return new PKCS8EncodedKeySpec(key.getEncoded());
            }
        }
        else if (key instanceof BCSPHINCSPlusPublicKey)
        {
            if (X509EncodedKeySpec.class.isAssignableFrom(keySpec))
            {
                return new X509EncodedKeySpec(key.getEncoded());
            }
        }
        else
        {
            throw new InvalidKeySpecException("Unsupported key type: "
                + key.getClass() + ".");
        }

        throw new InvalidKeySpecException("Unknown key specification: "
            + keySpec + ".");
    }

    @Override
	public final Key engineTranslateKey(Key key)
        throws InvalidKeyException
    {
        if (key instanceof BCSPHINCSPlusPrivateKey || key instanceof BCSPHINCSPlusPublicKey)
        {
            return key;
        }

        throw new InvalidKeyException("Unsupported key type");
    }

    @Override
	public PrivateKey generatePrivate(PrivateKeyInfo keyInfo)
        throws IOException
    {
        return new BCSPHINCSPlusPrivateKey(keyInfo);
    }

    @Override
	public PublicKey generatePublic(SubjectPublicKeyInfo keyInfo)
        throws IOException
    {
        return new BCSPHINCSPlusPublicKey(keyInfo);
    }
}
