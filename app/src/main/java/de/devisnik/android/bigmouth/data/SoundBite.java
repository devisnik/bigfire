package de.devisnik.android.bigmouth.data;

import java.io.Serializable;

public class SoundBite implements Serializable {

	private static final long serialVersionUID = -5723272842697483929L;
	
	public long id = -1;
	public String title;
	public String message;
	public String language;
	public String pitch;
	public String volume;
	public String speed;
	
}
