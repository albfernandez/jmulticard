package es.gob.jmulticard.card.icao;

/** Sexo del titular de un MRTD.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public enum Gender {

	/** Hombre. */
	MALE("Hombre"), //$NON-NLS-1$

	/** Mujer. */
	FEMALE("Mujer"), //$NON-NLS-1$

	/** Desconocido. */
	OTHER("No conocido"); //$NON-NLS-1$

	/** Texto descriptivo del sexo. */
	private final String desc;

	/** Constructor.
	 * @param d Texto descriptivo del sexo del titular. */
	Gender(final String d) {
		desc = d;
	}

	@Override
	public String toString() {
		return desc;
	}

	/** Obtiene el sexo del titular a partir del texto correspondiente en la MRZ del MRTD.
	 * @param text Texto correspondiente al sexo en la MRZ del MRTD.
	 *             Debe ser "F" (<i>female</i>) o "M" (<i>male</i>).
	 * @return Sexo del titular del MRTD. */
	public static Gender getGender(final String text) {
		if (text == null) {
			throw new IllegalArgumentException("El texto de descripcion del sexo no puede ser nulo"); //$NON-NLS-1$
		}
		if ("F".equalsIgnoreCase(text.trim())) { //$NON-NLS-1$
			return FEMALE;
		}
		if ("M".equalsIgnoreCase(text.trim())) { //$NON-NLS-1$
			return MALE;
		}
		throw new IllegalArgumentException("Sexo indeterminado: " + text); //$NON-NLS-1$
	}
}
