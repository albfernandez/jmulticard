package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.MaxBytesExceededException;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Integers;
import org.bouncycastle.util.Pack;
import org.bouncycastle.util.Strings;

/**
 * Implementation of Daniel J. Bernstein's Salsa20 stream cipher, Snuffle 2005
 */
public class Salsa20Engine
    implements SkippingStreamCipher
{
    public final static int DEFAULT_ROUNDS = 20;

    /** Constants */
    private final static int STATE_SIZE = 16; // 16, 32 bit ints = 64 bytes

    private final static int[] TAU_SIGMA = Pack.littleEndianToInt(Strings.toByteArray("expand 16-byte k" + "expand 32-byte k"), 0, 8);

    protected void packTauOrSigma(final int keyLength, final int[] state, final int stateOffset)
    {
        final int tsOff = (keyLength - 16) / 4;
        state[stateOffset    ] = TAU_SIGMA[tsOff    ];
        state[stateOffset + 1] = TAU_SIGMA[tsOff + 1];
        state[stateOffset + 2] = TAU_SIGMA[tsOff + 2];
        state[stateOffset + 3] = TAU_SIGMA[tsOff + 3];
    }

    /** @deprecated */
	@Deprecated
	protected final static byte[]
        sigma = Strings.toByteArray("expand 32-byte k"),
        tau   = Strings.toByteArray("expand 16-byte k");

    protected int rounds;

    /*
     * variables to hold the state of the engine
     * during encryption and decryption
     */
    private int         index = 0;
    protected int[]     engineState = new int[STATE_SIZE]; // state
    protected int[]     x = new int[STATE_SIZE] ; // internal buffer
    private final byte[]      keyStream   = new byte[STATE_SIZE * 4]; // expanded state, 64 bytes
    private boolean     initialised = false;

    /*
     * internal counter
     */
    private int cW0, cW1, cW2;

    /**
     * Creates a 20 round Salsa20 engine.
     */
    public Salsa20Engine()
    {
        this(DEFAULT_ROUNDS);
    }

    /**
     * Creates a Salsa20 engine with a specific number of rounds.
     * @param rounds the number of rounds (must be an even number).
     */
    public Salsa20Engine(final int rounds)
    {
        if (rounds <= 0 || (rounds & 1) != 0)
        {
            throw new IllegalArgumentException("'rounds' must be a positive, even number");
        }

        this.rounds = rounds;
    }

    /**
     * initialise a Salsa20 cipher.
     *
     * @param forEncryption whether or not we are for encryption.
     * @param params the parameters required to set up the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    @Override
	public void init(
        final boolean             forEncryption,
        final CipherParameters     params)
    {
        /*
        * Salsa20 encryption and decryption is completely
        * symmetrical, so the 'forEncryption' is
        * irrelevant. (Like 90% of stream ciphers)
        */

        if (!(params instanceof ParametersWithIV))
        {
            throw new IllegalArgumentException(getAlgorithmName() + " Init parameters must include an IV");
        }

        final ParametersWithIV ivParams = (ParametersWithIV) params;

        final byte[] iv = ivParams.getIV();
        if (iv == null || iv.length != getNonceSize())
        {
            throw new IllegalArgumentException(getAlgorithmName() + " requires exactly " + getNonceSize()
                    + " bytes of IV");
        }

        final CipherParameters keyParam = ivParams.getParameters();
        if (keyParam == null)
        {
            if (!this.initialised)
            {
                throw new IllegalStateException(getAlgorithmName() + " KeyParameter can not be null for first initialisation");
            }

            setKey(null, iv);
        }
        else if (keyParam instanceof KeyParameter)
        {
            setKey(((KeyParameter)keyParam).getKey(), iv);
        }
        else
        {
            throw new IllegalArgumentException(getAlgorithmName() + " Init parameters must contain a KeyParameter (or null for re-init)");
        }

        reset();

        this.initialised = true;
    }

    protected int getNonceSize()
    {
        return 8;
    }

    @Override
	public String getAlgorithmName()
    {
        final StringBuilder name = new StringBuilder("Salsa20");
        if (this.rounds != DEFAULT_ROUNDS)
        {
            name.append("/").append(this.rounds);
        }
        return name.toString();
    }

    @Override
	public byte returnByte(final byte in)
    {
        if (limitExceeded())
        {
            throw new MaxBytesExceededException("2^70 byte limit per IV; Change IV");
        }

        final byte out = (byte)(this.keyStream[this.index]^in);
        this.index = this.index + 1 & 63;

        if (this.index == 0)
        {
            advanceCounter();
            generateKeyStream(this.keyStream);
        }

        return out;
    }

    protected void advanceCounter(final long diff)
    {
        final int hi = (int)(diff >>> 32);
        final int lo = (int)diff;

        if (hi > 0)
        {
            this.engineState[9] += hi;
        }

        final int oldState = this.engineState[8];

        this.engineState[8] += lo;

        if (oldState != 0 && this.engineState[8] < oldState)
        {
            this.engineState[9]++;
        }
    }

    protected void advanceCounter()
    {
        if (++this.engineState[8] == 0)
        {
            ++this.engineState[9];
        }
    }

    protected void retreatCounter(final long diff)
    {
        final int hi = (int)(diff >>> 32);
        final int lo = (int)diff;

        if (hi != 0)
        {
            if ((this.engineState[9] & 0xffffffffL) >= (hi & 0xffffffffL))
            {
                this.engineState[9] -= hi;
            }
            else
            {
                throw new IllegalStateException("attempt to reduce counter past zero.");
            }
        }

        if ((this.engineState[8] & 0xffffffffL) >= (lo & 0xffffffffL))
        {
        } else if (this.engineState[9] != 0)
		{
		    --this.engineState[9];
		}
		else
		{
		    throw new IllegalStateException("attempt to reduce counter past zero.");
		}
		this.engineState[8] -= lo;
    }

    protected void retreatCounter()
    {
        if (this.engineState[8] == 0 && this.engineState[9] == 0)
        {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }

        if (--this.engineState[8] == -1)
        {
            --this.engineState[9];
        }
    }

    @Override
	public int processBytes(
        final byte[]     in,
        final int     inOff,
        final int     len,
        final byte[]     out,
        final int     outOff)
    {
        if (!this.initialised)
        {
            throw new IllegalStateException(getAlgorithmName() + " not initialised");
        }

        if (inOff + len > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        if (outOff + len > out.length)
        {
            throw new OutputLengthException("output buffer too short");
        }

        if (limitExceeded(len))
        {
            throw new MaxBytesExceededException("2^70 byte limit per IV would be exceeded; Change IV");
        }

        for (int i = 0; i < len; i++)
        {
            out[i + outOff] = (byte)(this.keyStream[this.index] ^ in[i + inOff]);
            this.index = this.index + 1 & 63;

            if (this.index == 0)
            {
                advanceCounter();
                generateKeyStream(this.keyStream);
            }
        }

        return len;
    }

    @Override
	public long skip(final long numberOfBytes)
    {
        if (numberOfBytes >= 0)
        {
            long remaining = numberOfBytes;

            if (remaining >= 64)
            {
                final long count = remaining / 64;

                advanceCounter(count);

                remaining -= count * 64;
            }

            final int oldIndex = this.index;

            this.index = this.index + (int)remaining & 63;

            if (this.index < oldIndex)
            {
                advanceCounter();
            }
        }
        else
        {
            long remaining = -numberOfBytes;

            if (remaining >= 64)
            {
                final long count = remaining / 64;

                retreatCounter(count);

                remaining -= count * 64;
            }

            for (long i = 0; i < remaining; i++)
            {
                if (this.index == 0)
                {
                    retreatCounter();
                }

                this.index = this.index - 1 & 63;
            }
        }

        generateKeyStream(this.keyStream);

        return numberOfBytes;
    }

    @Override
	public long seekTo(final long position)
    {
        reset();

        return skip(position);
    }

    @Override
	public long getPosition()
    {
        return getCounter() * 64 + this.index;
    }

    @Override
	public void reset()
    {
        this.index = 0;
        resetLimitCounter();
        resetCounter();

        generateKeyStream(this.keyStream);
    }

    protected long getCounter()
    {
        return (long)this.engineState[9] << 32 | this.engineState[8] & 0xffffffffL;
    }

    protected void resetCounter()
    {
        this.engineState[8] = this.engineState[9] = 0;
    }

    protected void setKey(final byte[] keyBytes, final byte[] ivBytes)
    {
        if (keyBytes != null)
        {
            if (keyBytes.length != 16 && keyBytes.length != 32)
            {
                throw new IllegalArgumentException(getAlgorithmName() + " requires 128 bit or 256 bit key");
            }

            final int tsOff = (keyBytes.length - 16) / 4;
            this.engineState[0 ] = TAU_SIGMA[tsOff    ];
            this.engineState[5 ] = TAU_SIGMA[tsOff + 1];
            this.engineState[10] = TAU_SIGMA[tsOff + 2];
            this.engineState[15] = TAU_SIGMA[tsOff + 3];

            // Key
            Pack.littleEndianToInt(keyBytes, 0, this.engineState, 1, 4);
            Pack.littleEndianToInt(keyBytes, keyBytes.length - 16, this.engineState, 11, 4);
        }

        // IV
        Pack.littleEndianToInt(ivBytes, 0, this.engineState, 6, 2);
    }

    protected void generateKeyStream(final byte[] output)
    {
        salsaCore(this.rounds, this.engineState, this.x);
        Pack.intToLittleEndian(this.x, output, 0);
    }

    /**
     * Salsa20 function
     *
     * @param rounds Rounds number.
     * @param   input   input data
     * @param x result.
     */
    public static void salsaCore(final int rounds, final int[] input, final int[] x)
    {
        if (input.length != 16 || x.length != 16)
        {
            throw new IllegalArgumentException();
        }
        if (rounds % 2 != 0)
        {
            throw new IllegalArgumentException("Number of rounds must be even");
        }

        int x00 = input[ 0];
        int x01 = input[ 1];
        int x02 = input[ 2];
        int x03 = input[ 3];
        int x04 = input[ 4];
        int x05 = input[ 5];
        int x06 = input[ 6];
        int x07 = input[ 7];
        int x08 = input[ 8];
        int x09 = input[ 9];
        int x10 = input[10];
        int x11 = input[11];
        int x12 = input[12];
        int x13 = input[13];
        int x14 = input[14];
        int x15 = input[15];

        for (int i = rounds; i > 0; i -= 2)
        {
            x04 ^= Integers.rotateLeft(x00 + x12, 7);
            x08 ^= Integers.rotateLeft(x04 + x00, 9);
            x12 ^= Integers.rotateLeft(x08 + x04, 13);
            x00 ^= Integers.rotateLeft(x12 + x08, 18);
            x09 ^= Integers.rotateLeft(x05 + x01, 7);
            x13 ^= Integers.rotateLeft(x09 + x05, 9);
            x01 ^= Integers.rotateLeft(x13 + x09, 13);
            x05 ^= Integers.rotateLeft(x01 + x13, 18);
            x14 ^= Integers.rotateLeft(x10 + x06, 7);
            x02 ^= Integers.rotateLeft(x14 + x10, 9);
            x06 ^= Integers.rotateLeft(x02 + x14, 13);
            x10 ^= Integers.rotateLeft(x06 + x02, 18);
            x03 ^= Integers.rotateLeft(x15 + x11, 7);
            x07 ^= Integers.rotateLeft(x03 + x15, 9);
            x11 ^= Integers.rotateLeft(x07 + x03, 13);
            x15 ^= Integers.rotateLeft(x11 + x07, 18);

            x01 ^= Integers.rotateLeft(x00 + x03, 7);
            x02 ^= Integers.rotateLeft(x01 + x00, 9);
            x03 ^= Integers.rotateLeft(x02 + x01, 13);
            x00 ^= Integers.rotateLeft(x03 + x02, 18);
            x06 ^= Integers.rotateLeft(x05 + x04, 7);
            x07 ^= Integers.rotateLeft(x06 + x05, 9);
            x04 ^= Integers.rotateLeft(x07 + x06, 13);
            x05 ^= Integers.rotateLeft(x04 + x07, 18);
            x11 ^= Integers.rotateLeft(x10 + x09, 7);
            x08 ^= Integers.rotateLeft(x11 + x10, 9);
            x09 ^= Integers.rotateLeft(x08 + x11, 13);
            x10 ^= Integers.rotateLeft(x09 + x08, 18);
            x12 ^= Integers.rotateLeft(x15 + x14, 7);
            x13 ^= Integers.rotateLeft(x12 + x15, 9);
            x14 ^= Integers.rotateLeft(x13 + x12, 13);
            x15 ^= Integers.rotateLeft(x14 + x13, 18);
        }

        x[ 0] = x00 + input[ 0];
        x[ 1] = x01 + input[ 1];
        x[ 2] = x02 + input[ 2];
        x[ 3] = x03 + input[ 3];
        x[ 4] = x04 + input[ 4];
        x[ 5] = x05 + input[ 5];
        x[ 6] = x06 + input[ 6];
        x[ 7] = x07 + input[ 7];
        x[ 8] = x08 + input[ 8];
        x[ 9] = x09 + input[ 9];
        x[10] = x10 + input[10];
        x[11] = x11 + input[11];
        x[12] = x12 + input[12];
        x[13] = x13 + input[13];
        x[14] = x14 + input[14];
        x[15] = x15 + input[15];
    }

    private void resetLimitCounter()
    {
        this.cW0 = 0;
        this.cW1 = 0;
        this.cW2 = 0;
    }

    private boolean limitExceeded()
    {
        if (++this.cW0 == 0 && ++this.cW1 == 0)
		{
		    return (++this.cW2 & 0x20) != 0;          // 2^(32 + 32 + 6)
		}

        return false;
    }

    /*
     * this relies on the fact len will always be positive.
     */
    private boolean limitExceeded(final int len)
    {
        this.cW0 += len;
        if (this.cW0 < len && this.cW0 >= 0 && ++this.cW1 == 0)
		{
		    return (++this.cW2 & 0x20) != 0;          // 2^(32 + 32 + 6)
		}

        return false;
    }
}
