/*
 * Controlador Java de la Secretaria de Estado de Administraciones Publicas
 * para el DNI electronico.
 *
 * El Controlador Java para el DNI electronico es un proveedor de seguridad de JCA/JCE
 * que permite el acceso y uso del DNI electronico en aplicaciones Java de terceros
 * para la realizacion de procesos de autenticacion, firma electronica y validacion
 * de firma. Para ello, se implementan las funcionalidades KeyStore y Signature para
 * el acceso a los certificados y claves del DNI electronico, asi como la realizacion
 * de operaciones criptograficas de firma con el DNI electronico. El Controlador ha
 * sido disenado para su funcionamiento independiente del sistema operativo final.
 *
 * Copyright (C) 2012 Direccion General de Modernizacion Administrativa, Procedimientos
 * e Impulso de la Administracion Electronica
 *
 * Este programa es software libre y utiliza un licenciamiento dual (LGPL 2.1+
 * o EUPL 1.1+), lo cual significa que los usuarios podran elegir bajo cual de las
 * licencias desean utilizar el codigo fuente. Su eleccion debera reflejarse
 * en las aplicaciones que integren o distribuyan el Controlador, ya que determinara
 * su compatibilidad con otros componentes.
 *
 * El Controlador puede ser redistribuido y/o modificado bajo los terminos de la
 * Lesser GNU General Public License publicada por la Free Software Foundation,
 * tanto en la version 2.1 de la Licencia, o en una version posterior.
 *
 * El Controlador puede ser redistribuido y/o modificado bajo los terminos de la
 * European Union Public License publicada por la Comision Europea,
 * tanto en la version 1.1 de la Licencia, o en una version posterior.
 *
 * Deberia recibir una copia de la GNU Lesser General Public License, si aplica, junto
 * con este programa. Si no, consultelo en <http://www.gnu.org/licenses/>.
 *
 * Deberia recibir una copia de la European Union Public License, si aplica, junto
 * con este programa. Si no, consultelo en <http://joinup.ec.europa.eu/software/page/eupl>.
 *
 * Este programa es distribuido con la esperanza de que sea util, pero
 * SIN NINGUNA GARANTIA; incluso sin la garantia implicita de comercializacion
 * o idoneidad para un proposito particular.
 */
package es.gob.jmulticard.apdu.iso7816four;

import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.apdu.Apdu;
import es.gob.jmulticard.apdu.ResponseApdu;

/** APDU respuesta al comando APDU ISO 7816-4 de selecci&oacute;n de fichero.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class SelectFileApduResponse extends ResponseApdu {

	/** Nombre del DF que contiene el EF. */
    private byte[] dfName = null;

    /** Identificador del EF seleccionado. */
    private byte[] fileId = null;

    /** Longitud (en octetos) del EF seleccionado. */
    private byte[] fileLength = null;

    /** Construye una APDU respuesta al comando APDU ISO 7816-4 de selecci&oacute;n de fichero.
     * @param apduResponse APDU devuelta por la operaci&oacute;n de selecci&oacute;n de fichero. */
    public SelectFileApduResponse(final Apdu apduResponse) {
        super(apduResponse.getBytes());
        decode();
    }

    private void decode() {
    	// Comprobamos que haya respuesta (getData().length > 0), ya que puede no devolver un FCI
        if (isOk() && getData().length > 0) {
            // Longitud del troncho.
            final int length = getData()[1];

            // El primer byte es 0x6F el segundo es la long. y los 2 ultimos son el sw. por eso length - 2.
            if (getData().length - 2 == length) {
                int propInformationIndex = 2;
                // Tamano del fichero
                if (getData()[propInformationIndex] == (byte) 0x81) {
                	final int lengthLength = getData()[++propInformationIndex];
                	fileLength = getBytesFromData(++propInformationIndex, lengthLength);
                	propInformationIndex += lengthLength;
                }
                // FileID
                if (getData()[propInformationIndex] == (byte) 0x81) {
                	final int fileIdLength = getData()[++propInformationIndex];
                	fileId = getBytesFromData(++propInformationIndex, fileIdLength);
                	propInformationIndex += fileIdLength;
                }
                // Nombre del DF
                if (getData()[propInformationIndex] == (byte) 0x84) {
                    final int nameLength = getData()[++propInformationIndex];
                    dfName = getBytesFromData(++propInformationIndex, nameLength);
                    propInformationIndex += nameLength;
                }
                // El campo FCI propietario 0x85 en tarjetas FNMT contiene el FileID y el tamano
                if (getData()[propInformationIndex] == (byte) 0x85 && getData()[propInformationIndex + 1] == 10) {
                    fileId = getBytesFromData(propInformationIndex + 3, 2);
                    fileLength = getBytesFromData(propInformationIndex + 5, 2);
                }
            }
        }
    }

    private byte[] getBytesFromData(final int offset, final int length) {
        final byte[] result = new byte[length];
        System.arraycopy(getData(), offset, result, 0, length);
        return result;
    }

    /** Obtiene el nombre del DF.
     * @return Nombre del DF. */
    byte[] getDfName() {
    	if (dfName != null) {
	        final byte[] out = new byte[dfName.length];
	        System.arraycopy(dfName, 0, out, 0, dfName.length);
	        return out;
    	}
    	return null;
    }

    /** Devuelve el identificador del fichero seleccionado.
     * @return Identificador del fichero. */
    byte[] getFileId() {
    	if (fileId != null) {
	        final byte[] out = new byte[fileId.length];
	        System.arraycopy(fileId, 0, out, 0, fileId.length);
	        return out;
    	}
    	return null;
    }

    /** Devuelve la longitud del fichero seleccionado.
     * @return Longitud del fichero. */
    public int getFileLength() {
    	if (fileLength != null) {
    		return (fileLength[0] & 0xFF) << 8 | fileLength[1] & 0xFF;
    	}
    	// Un DF puede no tener tamano en el FCI
    	return 0;
    }

    @Override
    public boolean isOk() {
    	if (getData().length == 0) {
    		return super.isOk();
    	}
        return super.isOk() && getData()[0] == (byte) 0x6F && getData().length > 2;
    }

    @Override
    public String toString() {
    	final StringBuilder sb = new StringBuilder("Resultado de la seleccion de fichero:\n"); //$NON-NLS-1$
    	if (getDfName() != null) {
    		sb.append(" Nombre del fichero: " + new String(getDfName())); //$NON-NLS-1$
    		sb.append('\n');
    	}
    	if (getFileId() != null) {
    		sb.append(" Identificador de fichero: " + HexUtils.hexify(getFileId(), true)); //$NON-NLS-1$
    		sb.append('\n');
    	}
    	sb.append(" Longitud del fichero: " + getFileLength()); //$NON-NLS-1$
    	return sb.toString();
    }
}
