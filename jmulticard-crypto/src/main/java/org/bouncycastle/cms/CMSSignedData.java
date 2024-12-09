package org.bouncycastle.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.DLSet;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Encodable;
import org.bouncycastle.util.Store;

/**
 * general class for handling a pkcs7-signature message.
 *
 * A simple example of usage - note, in the example below the validity of
 * the certificate isn't verified, just the fact that one of the certs
 * matches the given signer...
 *
 * <pre>
 *  Store                   certStore = s.getCertificates();
 *  SignerInformationStore  signers = s.getSignerInfos();
 *  Collection              c = signers.getSigners();
 *  Iterator                it = c.iterator();
 *
 *  while (it.hasNext())
 *  {
 *      SignerInformation   signer = (SignerInformation)it.next();
 *      Collection          certCollection = certStore.getMatches(signer.getSID());
 *
 *      Iterator              certIt = certCollection.iterator();
 *      X509CertificateHolder cert = (X509CertificateHolder)certIt.next();
 *
 *      if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert)))
 *      {
 *          verified++;
 *      }
 *  }
 * </pre>
 */
public class CMSSignedData
    implements Encodable
{
    private static final CMSSignedHelper HELPER = CMSSignedHelper.INSTANCE;
    private static final DefaultDigestAlgorithmIdentifierFinder dgstAlgFinder = new DefaultDigestAlgorithmIdentifierFinder();

    SignedData              signedData;
    ContentInfo             contentInfo;
    CMSTypedData            signedContent;
    SignerInformationStore  signerInfoStore;

    private Map             hashes;

    private CMSSignedData(
        final CMSSignedData   c)
    {
        this.signedData = c.signedData;
        this.contentInfo = c.contentInfo;
        this.signedContent = c.signedContent;
        this.signerInfoStore = c.signerInfoStore;
    }

    public CMSSignedData(
        final byte[]      sigBlock)
        throws CMSException
    {
        this(CMSUtils.readContentInfo(sigBlock));
    }

    public CMSSignedData(
        final CMSProcessable  signedContent,
        final byte[]          sigBlock)
        throws CMSException
    {
        this(signedContent, CMSUtils.readContentInfo(sigBlock));
    }

    /**
     * Content with detached signature, digests precomputed
     *
     * @param hashes a map of precomputed digests for content indexed by name of hash.
     * @param sigBlock the signature object.
     * @throws CMSException Invalid data.
     */
    public CMSSignedData(
        final Map     hashes,
        final byte[]  sigBlock)
        throws CMSException
    {
        this(hashes, CMSUtils.readContentInfo(sigBlock));
    }

    /**
     * base constructor - content with detached signature.
     *
     * @param signedContent the content that was signed.
     * @param sigData the signature object.
     * @throws CMSException Invalid data.
     */
    public CMSSignedData(
        final CMSProcessable  signedContent,
        final InputStream     sigData)
        throws CMSException
    {
        this(signedContent, CMSUtils.readContentInfo(new ASN1InputStream(sigData)));
    }

    /**
     * base constructor - with encapsulated content
     * @param sigData the signature object.
     * @throws CMSException Invalid data.
     */
    public CMSSignedData(
        final InputStream sigData)
        throws CMSException
    {
        this(CMSUtils.readContentInfo(sigData));
    }

    /**
     * Constructor
     * @param signedContent Content.
     * @param sigData the signature object.
     * @throws CMSException Invalid data.
     */
    public CMSSignedData(
        final CMSProcessable  signedContent,
        final ContentInfo     sigData)
        throws CMSException
    {
        if (signedContent instanceof CMSTypedData)
        {
            this.signedContent = (CMSTypedData)signedContent;
        }
        else
        {
            this.signedContent = new CMSTypedData()
            {
                @Override
				public ASN1ObjectIdentifier getContentType()
                {
                    return CMSSignedData.this.signedData.getEncapContentInfo().getContentType();
                }

                @Override
				public void write(final OutputStream out)
                    throws IOException, CMSException
                {
                    signedContent.write(out);
                }

                @Override
				public Object getContent()
                {
                    return signedContent.getContent();
                }
            };
        }

        this.contentInfo = sigData;
        this.signedData = getSignedData();
    }

    public CMSSignedData(
        final Map             hashes,
        final ContentInfo     sigData)
        throws CMSException
    {
        this.hashes = hashes;
        this.contentInfo = sigData;
        this.signedData = getSignedData();
    }

    public CMSSignedData(
        final ContentInfo sigData)
        throws CMSException
    {
        this.contentInfo = sigData;
        this.signedData = getSignedData();

        //
        // this can happen if the signed message is sent simply to send a
        // certificate chain.
        //
        final ASN1Encodable content = this.signedData.getEncapContentInfo().getContent();
        if (content != null)
        {
            if (content instanceof ASN1OctetString)
            {
                this.signedContent = new CMSProcessableByteArray(this.signedData.getEncapContentInfo().getContentType(),
                    ((ASN1OctetString)content).getOctets());
            }
            else
            {
                this.signedContent = new PKCS7ProcessableObject(this.signedData.getEncapContentInfo().getContentType(), content);
            }
        }
        else
        {
            this.signedContent = null;
        }
    }

    private SignedData getSignedData()
        throws CMSException
    {
        try
        {
            return SignedData.getInstance(this.contentInfo.getContent());
        }
        catch (final ClassCastException | IllegalArgumentException e)
        {
            throw new CMSException("Malformed content.", e);
        }
    }

    /**
     * Return the version number for this object
     * @return version.
     */
    public int getVersion()
    {
        return this.signedData.getVersion().intValueExact();
    }

    /**
     *
     * Return the collection of signers that are associated with the
     * signatures for the message.
     * @return signers.
     */
    public SignerInformationStore getSignerInfos()
    {
        if (this.signerInfoStore == null)
        {
            final ASN1Set         s = this.signedData.getSignerInfos();
            final List            signerInfos = new ArrayList();

            for (int i = 0; i != s.size(); i++)
            {
                final SignerInfo info = SignerInfo.getInstance(s.getObjectAt(i));
                final ASN1ObjectIdentifier contentType = this.signedData.getEncapContentInfo().getContentType();

                if (this.hashes == null)
                {
                    signerInfos.add(new SignerInformation(info, contentType, this.signedContent, null));
                }
                else
                {
                    final Object obj = this.hashes.keySet().iterator().next();
                    final byte[] hash = obj instanceof String ? (byte[])this.hashes.get(info.getDigestAlgorithm().getAlgorithm().getId()) : (byte[])this.hashes.get(info.getDigestAlgorithm().getAlgorithm());

                    signerInfos.add(new SignerInformation(info, contentType, null, hash));
                }
            }

            this.signerInfoStore = new SignerInformationStore(signerInfos);
        }

        return this.signerInfoStore;
    }

    /**
     * Return if this is object represents a detached signature.
     *
     * @return true if this message represents a detached signature, false otherwise.
     */
    public boolean isDetachedSignature()
    {
        return this.signedData.getEncapContentInfo().getContent() == null && this.signedData.getSignerInfos().size() > 0;
    }

    /**
     * Return if this is object represents a certificate management message.
     *
     * @return true if the message has no signers or content, false otherwise.
     */
    public boolean isCertificateManagementMessage()
    {
        return this.signedData.getEncapContentInfo().getContent() == null && this.signedData.getSignerInfos().size() == 0;
    }

    /**
     * Return any X.509 certificate objects in this SignedData structure as a Store of X509CertificateHolder objects.
     *
     * @return a Store of X509CertificateHolder objects.
     */
    public Store<X509CertificateHolder> getCertificates()
    {
        return HELPER.getCertificates(this.signedData.getCertificates());
    }

    /**
     * Return any X.509 CRL objects in this SignedData structure as a Store of X509CRLHolder objects.
     *
     * @return a Store of X509CRLHolder objects.
     */
    public Store<X509CRLHolder> getCRLs()
    {
        return HELPER.getCRLs(this.signedData.getCRLs());
    }

    /**
     * Return any X.509 attribute certificate objects in this SignedData structure as a Store of X509AttributeCertificateHolder objects.
     *
     * @return a Store of X509AttributeCertificateHolder objects.
     */
    public Store<X509AttributeCertificateHolder> getAttributeCertificates()
    {
        return HELPER.getAttributeCertificates(this.signedData.getCertificates());
    }

    /**
     * Return any OtherRevocationInfo OtherRevInfo objects of the type indicated by otherRevocationInfoFormat in
     * this SignedData structure.
     *
     * @param otherRevocationInfoFormat OID of the format type been looked for.
     *
     * @return a Store of ASN1Encodable objects representing any objects of otherRevocationInfoFormat found.
     */
    public Store getOtherRevocationInfo(final ASN1ObjectIdentifier otherRevocationInfoFormat)
    {
        return HELPER.getOtherRevocationInfo(otherRevocationInfoFormat, this.signedData.getCRLs());
    }

    /**
     * Return the digest algorithm identifiers for the SignedData object
     *
     * @return the set of digest algorithm identifiers
     */
    public Set<AlgorithmIdentifier> getDigestAlgorithmIDs()
    {
        final Set<AlgorithmIdentifier> digests = new HashSet<>(this.signedData.getDigestAlgorithms().size());

        for (final Object element : this.signedData.getDigestAlgorithms()) {
            digests.add(AlgorithmIdentifier.getInstance(element));
        }

        return Collections.unmodifiableSet(digests);
    }

    /**
     * Return the a string representation of the OID associated with the
     * encapsulated content info structure carried in the signed data.
     *
     * @return the OID for the content type.
     */
    public String getSignedContentTypeOID()
    {
        return this.signedData.getEncapContentInfo().getContentType().getId();
    }

    public CMSTypedData getSignedContent()
    {
        return this.signedContent;
    }

    /**
     * return the ContentInfo
     * @return ContentInfo.
     */
    public ContentInfo toASN1Structure()
    {
        return this.contentInfo;
    }

    /**
     * return the ASN.1 encoded representation of this object.
     */
    @Override
	public byte[] getEncoded()
        throws IOException
    {
        return this.contentInfo.getEncoded();
    }

    /**
     * return the ASN.1 encoded representation of this object using the specified encoding.
     *
     * @param encoding the ASN.1 encoding format to use ("BER", "DL", or "DER").
     * @return Signed data encoded.
     * @throws IOException If IO error occurs.
     */
    public byte[] getEncoded(final String encoding)
        throws IOException
    {
        return this.contentInfo.getEncoded(encoding);
    }

    /**
     * Verify all the SignerInformation objects and their associated counter signatures attached
     * to this CMS SignedData object.
     *
     * @param verifierProvider  a provider of SignerInformationVerifier objects.
     * @return true if all verify, false otherwise.
     * @throws CMSException  if an exception occurs during the verification process.
     */
    public boolean verifySignatures(final SignerInformationVerifierProvider verifierProvider)
        throws CMSException
    {
        return verifySignatures(verifierProvider, false);
    }

    /**
     * Verify all the SignerInformation objects and optionally their associated counter signatures attached
     * to this CMS SignedData object.
     *
     * @param verifierProvider  a provider of SignerInformationVerifier objects.
     * @param ignoreCounterSignatures if true don't check counter signatures. If false check counter signatures as well.
     * @return true if all verify, false otherwise.
     * @throws CMSException  if an exception occurs during the verification process.
     */
    public boolean verifySignatures(final SignerInformationVerifierProvider verifierProvider, final boolean ignoreCounterSignatures)
        throws CMSException
    {
        final Collection signers = getSignerInfos().getSigners();

        for (final Object signer2 : signers) {
            final SignerInformation signer = (SignerInformation)signer2;

            try
            {
                final SignerInformationVerifier verifier = verifierProvider.get(signer.getSID());

                if (!signer.verify(verifier))
                {
                    return false;
                }

                if (!ignoreCounterSignatures)
                {
                    final Collection counterSigners = signer.getCounterSignatures().getSigners();

                    for (final Object counterSigner : counterSigners) {
                        if (!verifyCounterSignature((SignerInformation)counterSigner, verifierProvider))
                        {
                            return false;
                        }
                    }
                }
            }
            catch (final OperatorCreationException e)
            {
                throw new CMSException("failure in verifier provider: " + e.getMessage(), e);
            }
        }

        return true;
    }

    private boolean verifyCounterSignature(final SignerInformation counterSigner, final SignerInformationVerifierProvider verifierProvider)
        throws OperatorCreationException, CMSException
    {
        final SignerInformationVerifier counterVerifier = verifierProvider.get(counterSigner.getSID());

        if (!counterSigner.verify(counterVerifier))
        {
            return false;
        }

        final Collection counterSigners = counterSigner.getCounterSignatures().getSigners();
        for (final Object counterSigner2 : counterSigners) {
            if (!verifyCounterSignature((SignerInformation)counterSigner2, verifierProvider))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Return a new CMSSignedData which guarantees to have the passed in digestAlgorithm
     * in it.
     *
     * @param signedData the signed data object to be used as a base.
     * @param digestAlgorithm the digest algorithm to be added to the signed data.
     * @return a new signed data object.
     */
    public static CMSSignedData addDigestAlgorithm(
        final CMSSignedData           signedData,
        final AlgorithmIdentifier     digestAlgorithm)
    {
        final Set<AlgorithmIdentifier>   digestAlgorithms = signedData.getDigestAlgorithmIDs();
        final AlgorithmIdentifier        digestAlg = CMSSignedHelper.INSTANCE.fixDigestAlgID(digestAlgorithm, dgstAlgFinder);

        //
        // if the algorithm is already present there is no need to add it.
        //
        if (digestAlgorithms.contains(digestAlg))
        {
            return signedData;
        }

        //
        // copy
        //
        final CMSSignedData   cms = new CMSSignedData(signedData);

        //
        // build up the new set
        //
        final Set<AlgorithmIdentifier> digestAlgs = new HashSet<>();

        final Iterator    it = digestAlgorithms.iterator();
        while (it.hasNext())
        {
            digestAlgs.add(CMSSignedHelper.INSTANCE.fixDigestAlgID((AlgorithmIdentifier)it.next(), dgstAlgFinder));
        }
        digestAlgs.add(digestAlg);

        final ASN1Set             digests = CMSUtils.convertToDlSet(digestAlgs);
        final ASN1Sequence        sD = (ASN1Sequence)signedData.signedData.toASN1Primitive();

        final ASN1EncodableVector vec = new ASN1EncodableVector();

        //
        // signers are the last item in the sequence.
        //
        vec.add(sD.getObjectAt(0)); // version
        vec.add(digests);

        for (int i = 2; i != sD.size(); i++)
        {
            vec.add(sD.getObjectAt(i));
        }

        cms.signedData = SignedData.getInstance(new BERSequence(vec));

        //
        // replace the contentInfo with the new one
        //
        cms.contentInfo = new ContentInfo(cms.contentInfo.getContentType(), cms.signedData);

        return cms;
    }

    /**
     * Replace the SignerInformation store associated with this
     * CMSSignedData object with the new one passed in. You would
     * probably only want to do this if you wanted to change the unsigned
     * attributes associated with a signer, or perhaps delete one.
     *
     * @param signedData the signed data object to be used as a base.
     * @param signerInformationStore the new signer information store to use.
     * @return a new signed data object.
     */
    public static CMSSignedData replaceSigners(
        final CMSSignedData           signedData,
        final SignerInformationStore  signerInformationStore)
    {
        //
        // copy
        //
        final CMSSignedData   cms = new CMSSignedData(signedData);

        //
        // replace the store
        //
        cms.signerInfoStore = signerInformationStore;

        //
        // replace the signers in the SignedData object
        //
        final Set<AlgorithmIdentifier> digestAlgs = new HashSet<>();
        ASN1EncodableVector vec = new ASN1EncodableVector();

        final Iterator    it = signerInformationStore.getSigners().iterator();
        while (it.hasNext())
        {
            final SignerInformation signer = (SignerInformation)it.next();
            CMSUtils.addDigestAlgs(digestAlgs, signer, dgstAlgFinder);
            vec.add(signer.toASN1Structure());
        }

        final ASN1Set             digests = CMSUtils.convertToDlSet(digestAlgs);
        final ASN1Set             signers = new DLSet(vec);
        final ASN1Sequence        sD = (ASN1Sequence)signedData.signedData.toASN1Primitive();

        vec = new ASN1EncodableVector();

        //
        // signers are the last item in the sequence.
        //
        vec.add(sD.getObjectAt(0)); // version
        vec.add(digests);

        for (int i = 2; i != sD.size() - 1; i++)
        {
            vec.add(sD.getObjectAt(i));
        }

        vec.add(signers);

        cms.signedData = SignedData.getInstance(new BERSequence(vec));

        //
        // replace the contentInfo with the new one
        //
        cms.contentInfo = new ContentInfo(cms.contentInfo.getContentType(), cms.signedData);

        return cms;
    }

    /**
     * Replace the certificate and CRL information associated with this
     * CMSSignedData object with the new one passed in.
     *
     * @param signedData the signed data object to be used as a base.
     * @param certificates the new certificates to be used.
     * @param attrCerts the new attribute certificates to be used.
     * @param revocations the new CRLs to be used - a collection of X509CRLHolder objects, OtherRevocationInfoFormat, or both.
     * @return a new signed data object.
     * @exception CMSException if there is an error processing the CertStore
     */
    public static CMSSignedData replaceCertificatesAndCRLs(
        final CMSSignedData   signedData,
        final Store           certificates,
        final Store           attrCerts,
        final Store           revocations)
        throws CMSException
    {
        //
        // copy
        //
        final CMSSignedData   cms = new CMSSignedData(signedData);

        //
        // replace the certs and revocations in the SignedData object
        //
        ASN1Set certSet = null;
        ASN1Set crlSet = null;

        if (certificates != null || attrCerts != null)
        {
            final List certs = new ArrayList();

            if (certificates != null)
            {
                certs.addAll(CMSUtils.getCertificatesFromStore(certificates));
            }
            if (attrCerts != null)
            {
                certs.addAll(CMSUtils.getAttributeCertificatesFromStore(attrCerts));
            }

            final ASN1Set set = CMSUtils.createBerSetFromList(certs);

            if (set.size() != 0)
            {
                certSet = set;
            }
        }

        if (revocations != null)
        {
            final ASN1Set set = CMSUtils.createBerSetFromList(CMSUtils.getCRLsFromStore(revocations));

            if (set.size() != 0)
            {
                crlSet = set;
            }
        }

        //
        // replace the CMS structure.
        //
        cms.signedData = new SignedData(signedData.signedData.getDigestAlgorithms(),
                                   signedData.signedData.getEncapContentInfo(),
                                   certSet,
                                   crlSet,
                                   signedData.signedData.getSignerInfos());

        //
        // replace the contentInfo with the new one
        //
        cms.contentInfo = new ContentInfo(cms.contentInfo.getContentType(), cms.signedData);

        return cms;
    }
}
