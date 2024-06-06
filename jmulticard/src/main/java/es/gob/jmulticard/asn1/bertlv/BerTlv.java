/*
   Copyright Isaac Levin

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package es.gob.jmulticard.asn1.bertlv;

import java.io.ByteArrayInputStream;

import es.gob.jmulticard.HexUtils;

/** TLV seg&uacute;n ASN&#46;1 BER. Soporta etiquetas de doble octeto.
 * @author Isaac Levin. */
public final class BerTlv {

    private BerTlvIdentifier tag;
    private int length;
    private byte[] value;

    /** Obtiene la etiqueta (tipo) del TLV.
     * @return Etiqueta (tipo) del TLV. */
    public byte getTag() {
        return (byte) tag.getTagValue();
    }

    /** Obtiene el valor del TLV.
     * @return Valor del TLV. */
    public byte[] getValue() {
        if (value == null) {
            return null;
        }
        final byte[] out = new byte[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    /** Obtiene la longitud de los datos del valor del TLV.
     * @return Longitud de los datos del valor del TLV. */
    public int getLength() {
    	return length;
    }

    /** Obtiene una instancia del TLV.
     * @param stream Representaci&oacute;n binaria del TLV.
     * @return Instancia del TLV. */
    public static BerTlv createInstance(final byte[] stream) {
        final BerTlv tlv = new BerTlv();
        tlv.decode(new ByteArrayInputStream(stream));
        return tlv;
    }

    /** Obtiene una instancia del TLV.
     * @param stream Flujo hacia la representaci&oacute;n binaria del TLV.
     *               El flujo se devuelve con avanzado hasta el final del TLV.
     * @return Instancia del TLV. */
    public static BerTlv createInstance(final ByteArrayInputStream stream) {
        final BerTlv tlv = new BerTlv();
        tlv.decode(stream);
        return tlv;
    }

    private void decode(final ByteArrayInputStream stream) {
        // Decodificamos el Tag
        tag = new BerTlvIdentifier();
        tag.decode(stream);

        // Decodificamos la longitud
        int tmpLength = stream.read();
        if (tmpLength > 127 && tmpLength != 128) {
        	// Es un long
            final int numberOfLengthOctets = tmpLength & 127; // turn off 8th bit
            tmpLength = 0;
            for (int i = 0; i < numberOfLengthOctets; i++) {
                final int nextLengthOctet = stream.read();
                tmpLength <<= 8;
                tmpLength |= nextLengthOctet;
            }
        }
		length = tmpLength;

        // Decodificamos el valor
        if (length == 128) { // 1000 0000
            // Formato indefinido
            stream.mark(0);
            int prevOctet = 1;
            int curOctet = 0;
            int len = 0;
            while (true) {
                len++;
                curOctet = stream.read();
                if (prevOctet == 0 && curOctet == 0) {
                    break;
                }
                prevOctet = curOctet;
            }
            len -= 2;
            value = new byte[len];
            stream.reset();
            if (len != stream.read(value, 0, len)) {
                throw new IndexOutOfBoundsException(
            		"La longitud de los datos leidos no coincide con el parametro indicado" //$NON-NLS-1$
        		);
            }
            length = len;
        }
        else {
            // Formato definido
            value = new byte[length];
            if (length != stream.read(value, 0, length)) {
                throw new IndexOutOfBoundsException(
            		"La longitud de los datos leidos no coincide con el parametro indicado" //$NON-NLS-1$
        		);
            }
        }
    }

    @Override
    public String toString() {
        return "[TLV: T=" + tag + "; L=" + length + "d; V=" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		(value == null ? "null" : HexUtils.hexify(value, false)) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
