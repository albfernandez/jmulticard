package org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.security.cert.CRLException;

class ExtCRLException
    extends CRLException
{
    Throwable cause;

    ExtCRLException(String message, Throwable cause)
    {
        super(message);
        this.cause = cause;
    }

    @Override
	public Throwable getCause()
    {
        return cause;
    }
}
