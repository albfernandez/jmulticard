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
package es.gob.jmulticard.apdu;

import java.io.ByteArrayOutputStream;

/** Comando APDU para comunicaci&oacute;n con tarjeta inteligente.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public class CommandApdu extends Apdu {

	private final byte cla;
	private final byte ins;
	private final byte p1;
	private final byte p2;
	private Integer le;
	private final byte[] body;

	private static byte[] getBody(final byte[] bytes) {
		if (bytes == null || bytes.length < 5) {
			throw new IllegalArgumentException(
				"La longitud del array de octetos debe ser igual o mayor que 5" //$NON-NLS-1$
			);
		}
		final byte[] data;
		final int i = bytes[4] & 0xff;
		if (bytes.length > 5) {
			data = new byte[i];
			System.arraycopy(bytes, 5, data, 0, i);
			return data;
		}
		return null;
	}

	private static Integer getLength(final byte[] bytes) {

		if (bytes == null || bytes.length < 5) {
			throw new IllegalArgumentException(
				"La longitud del array de octetos debe ser igual o mayor que 5." //$NON-NLS-1$
			);
		}
		final int i = bytes[4] & 0xff;
		if (bytes.length>5 && bytes.length>i+5) {
			return Integer.valueOf(bytes[i+5]);
		}
		if (bytes.length==5) {
			return Integer.valueOf(i);
		}
		return null;
	}

	/** Construye una APDU en base a un array de octetos.
	 * @param bytes Array de octetos para la construcci&oacute;n del objeto. */
	public CommandApdu(final byte[] bytes) {
		this(
			bytes[0], // CLA
			bytes[1], // INS
			bytes[2], // P1
			bytes[3], // P2
			getBody(bytes),
			getLength(bytes)
		);
	}

	/** Construye una APDU gen&eacute;rica.
	 * @param apduCla Clase (CLA) de APDU.
	 * @param apduIns Identificador de la instrucci&oacute;n (INS) que esta APDU representa.
	 * @param param1 Primer par&aacute;metro (P1) de la APDU.
	 * @param param2 Segundo par&aacute;metro (P2) de la APDU.
	 * @param data Datos del comando.
	 * @param ne N&uacute;mero de octetos esperados en la respuesta (Ne). */
	public CommandApdu(final byte apduCla,
			           final byte apduIns,
			           final byte param1,
			           final byte param2,
			           final byte[] data,
			           final Integer ne) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		cla = apduCla;
		baos.write(apduCla);

		ins = apduIns;
		baos.write(apduIns);

		p1 = param1;
		baos.write(param1);

		p2 = param2;
		baos.write(param2);

		if (data == null) {
			body = null;
		}
		else {
			body = new byte[data.length];
			System.arraycopy(data, 0, body, 0, data.length);

			// Caso 4s: |CLA|INS|P1 |P2 |LC |...BODY...|LE |              len = 7..261
			if (data.length <= 255) {
				baos.write(Integer.valueOf(String.valueOf(body.length)).byteValue());
			}
			// Caso 3e: |CLA|INS|P1 |P2 |00 |LC1|LC2|...BODY...|          len = 8..65542
			else {
				baos.write((byte) 0x00);
				baos.write((byte) (data.length >> 8));   // LC1
				baos.write((byte) (data.length & 0xff)); // LC2
			}

			if (body.length > 0) {
				try {
					baos.write(body);
				}
				catch (final Exception e) {
					throw new IllegalArgumentException(
						"No se pueden tratar los datos de la APDU", e //$NON-NLS-1$
					);
				}
			}
		}

		le = ne;
		if (ne != null) {
			if (ne.intValue() <= 0xff) {
				baos.write(ne.byteValue());
			}
			else {
				baos.write((byte) (ne.intValue() >> 8));
				baos.write((byte) (ne.intValue() & 0xff));
			}
		}

		setBytes(baos.toByteArray());
	}

	/** Devuelve la clase (CLA) de APDU.
	 * @return Clase (CLA) de APDU. */
	public byte getCla() {
		return cla;
	}

	/** Obtiene el cuerpo de la APDU.
	 * @return Cuerpo de la APDU, o <code>null</code> si no est&aacute; establecido. */
	public byte[] getData() {
		if (body == null) {
			return null;
		}
		final byte[] out = new byte[body.length];
		System.arraycopy(body, 0, out, 0, body.length);
		return out;
	}

	/** Devuelve el octeto identificador de la instrucci&oacute;n (INS) que esta
	 * APDU representa.
	 * @return Identificador de instrucci&oacute;n. */
	public byte getIns() {
		return ins;
	}

	/** Obtiene el n&uacute;mero m&aacute;ximo de octetos esperados en la APDU de respuesta.
	 * @return N&uacute;mero m&aacute;ximo de octetos esperados en la APDU de
	 *         respuesta, o <code>null</code> si no est&aacute; establecido. */
	public Integer getLe() {
		return le;
	}

	/** Devuelve el primer par&aacute;metro (P1) de la APDU.
	 * @return Primer par&aacute;metro (P1) de la APDU. */
	public byte getP1() {
		return p1;
	}

	/** Devuelve el segundo par&aacute;metro (P2) de la APDU.
	 * @return Segundo par&aacute;metro (P2) de la APDU. */
	public byte getP2() {
		return p2;
	}

	/** Establece el n&uacute;mero de octetos esperados en la APDU de respuesta.
	 * @param apduLe N&uacute;mero esperado de octetos. */
	public void setLe(final int apduLe) {
		le = Integer.valueOf(String.valueOf(apduLe));
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(cla);
		baos.write(ins);
		baos.write(p1);
		baos.write(p2);
		if (body != null && body.length > 0) {
			try {
				baos.write(body);
			}
			catch (final Exception e) {
				throw new IllegalArgumentException(
					"No se pueden tratar los datos de la APDU", e //$NON-NLS-1$
				);
			}
		}
		baos.write(apduLe);
		setBytes(baos.toByteArray());
	}
}