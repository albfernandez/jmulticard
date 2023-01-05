package org.bouncycastle.math.ec.custom.sec;

import java.math.BigInteger;

import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.raw.Nat128;
import org.bouncycastle.util.Arrays;

public class SecT113FieldElement extends ECFieldElement.AbstractF2m
{
    protected long[] x;

    public SecT113FieldElement(final BigInteger x)
    {
        if (x == null || x.signum() < 0 || x.bitLength() > 113)
        {
            throw new IllegalArgumentException("x value invalid for SecT113FieldElement");
        }

        this.x = SecT113Field.fromBigInteger(x);
    }

    public SecT113FieldElement()
    {
        x = Nat128.create64();
    }

    protected SecT113FieldElement(final long[] x)
    {
        this.x = x;
    }

//    public int bitLength()
//    {
//        return x.degree();
//    }

    @Override
	public boolean isOne()
    {
        return Nat128.isOne64(x);
    }

    @Override
	public boolean isZero()
    {
        return Nat128.isZero64(x);
    }

    @Override
	public boolean testBitZero()
    {
        return (x[0] & 1L) != 0L;
    }

    @Override
	public BigInteger toBigInteger()
    {
        return Nat128.toBigInteger64(x);
    }

    @Override
	public String getFieldName()
    {
        return "SecT113Field";
    }

    @Override
	public int getFieldSize()
    {
        return 113;
    }

    @Override
	public ECFieldElement add(final ECFieldElement b)
    {
        final long[] z = Nat128.create64();
        SecT113Field.add(x, ((SecT113FieldElement)b).x, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public ECFieldElement addOne()
    {
        final long[] z = Nat128.create64();
        SecT113Field.addOne(x, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public ECFieldElement subtract(final ECFieldElement b)
    {
        // Addition and subtraction are the same in F2m
        return add(b);
    }

    @Override
	public ECFieldElement multiply(final ECFieldElement b)
    {
        final long[] z = Nat128.create64();
        SecT113Field.multiply(x, ((SecT113FieldElement)b).x, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public ECFieldElement multiplyMinusProduct(final ECFieldElement b, final ECFieldElement x, final ECFieldElement y)
    {
        return multiplyPlusProduct(b, x, y);
    }

    @Override
	public ECFieldElement multiplyPlusProduct(final ECFieldElement b, final ECFieldElement x, final ECFieldElement y)
    {
        final long[] ax = this.x, bx = ((SecT113FieldElement)b).x;
        final long[] xx = ((SecT113FieldElement)x).x, yx = ((SecT113FieldElement)y).x;

        final long[] tt = Nat128.createExt64();
        SecT113Field.multiplyAddToExt(ax, bx, tt);
        SecT113Field.multiplyAddToExt(xx, yx, tt);

        final long[] z = Nat128.create64();
        SecT113Field.reduce(tt, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public ECFieldElement divide(final ECFieldElement b)
    {
        return multiply(b.invert());
    }

    @Override
	public ECFieldElement negate()
    {
        return this;
    }

    @Override
	public ECFieldElement square()
    {
        final long[] z = Nat128.create64();
        SecT113Field.square(x, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public ECFieldElement squareMinusProduct(final ECFieldElement x, final ECFieldElement y)
    {
        return squarePlusProduct(x, y);
    }

    @Override
	public ECFieldElement squarePlusProduct(final ECFieldElement x, final ECFieldElement y)
    {
        final long[] ax = this.x;
        final long[] xx = ((SecT113FieldElement)x).x, yx = ((SecT113FieldElement)y).x;

        final long[] tt = Nat128.createExt64();
        SecT113Field.squareAddToExt(ax, tt);
        SecT113Field.multiplyAddToExt(xx, yx, tt);

        final long[] z = Nat128.create64();
        SecT113Field.reduce(tt, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public ECFieldElement squarePow(final int pow)
    {
        if (pow < 1)
        {
            return this;
        }

        final long[] z = Nat128.create64();
        SecT113Field.squareN(x, pow, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public ECFieldElement halfTrace()
    {
        final long[] z = Nat128.create64();
        SecT113Field.halfTrace(x, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public boolean hasFastTrace()
    {
        return true;
    }

    @Override
	public int trace()
    {
        return SecT113Field.trace(x);
    }

    @Override
	public ECFieldElement invert()
    {
        final long[] z = Nat128.create64();
        SecT113Field.invert(x, z);
        return new SecT113FieldElement(z);
    }

    @Override
	public ECFieldElement sqrt()
    {
        final long[] z = Nat128.create64();
        SecT113Field.sqrt(x, z);
        return new SecT113FieldElement(z);
    }

    public int getRepresentation()
    {
        return ECFieldElement.F2m.TPB;
    }

    public int getM()
    {
        return 113;
    }

    public int getK1()
    {
        return 9;
    }

    public int getK2()
    {
        return 0;
    }

    public int getK3()
    {
        return 0;
    }

    @Override
	public boolean equals(final Object other)
    {
        if (other == this)
        {
            return true;
        }

        if (!(other instanceof SecT113FieldElement))
        {
            return false;
        }

        final SecT113FieldElement o = (SecT113FieldElement)other;
        return Nat128.eq64(x, o.x);
    }

    @Override
	public int hashCode()
    {
        return 113009 ^ Arrays.hashCode(x, 0, 2);
    }
}
