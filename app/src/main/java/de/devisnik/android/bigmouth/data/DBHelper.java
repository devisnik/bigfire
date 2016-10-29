package de.devisnik.android.bigmouth.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import de.devisnik.android.bigmouth.R;

public class DBHelper {
	public static final String BITES_DB = "bites";
	public static final String BITES_TABLE = "bites_table";
	public static final int BITES_VERSION = 5;

	public static final String TITLE_COLUMN = "title";
	public static final String TEXT_COLUMN = "text";
	public static final String LANGUAGE_COLUMN = "language";
	public static final String PITCH_COLUMN = "pitch";
	public static final String VOLUME_COLUMN = "volume";
	public static final String SPEED_COLUMN = "speed";

	private static final String ID_COLUMN = "_id";
	private static final String[] COLUMNS = { ID_COLUMN, TITLE_COLUMN, TEXT_COLUMN,
			LANGUAGE_COLUMN, PITCH_COLUMN, VOLUME_COLUMN, SPEED_COLUMN };
	private DBOpenHelper itsDbOpenHelper;
	private SQLiteDatabase itsDatabase;

	private class DBOpenHelper extends SQLiteOpenHelper {

		private static final String CREATE = "CREATE TABLE " + DBHelper.BITES_TABLE 
		+ " ("
			+ ID_COLUMN + " INTEGER PRIMARY KEY, " 
			+ TITLE_COLUMN + " TEXT, " 
			+ TEXT_COLUMN + " TEXT, " 
			+ LANGUAGE_COLUMN + " TEXT, " 
			+ PITCH_COLUMN + " TEXT, " 
			+ VOLUME_COLUMN + " TEXT, " 
			+ SPEED_COLUMN + " TEXT)";
		private final Context itsContext;

		public DBOpenHelper(final Context context) {
			super(context, BITES_DB, null, BITES_VERSION);
			itsContext = context;
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL(CREATE);
			createInitialBites(db);
		}

		private void createInitialBites(SQLiteDatabase db) {
			CharSequence[] titles = itsContext.getResources().getTextArray(R.array.usage_titles);
			CharSequence[] messages = itsContext.getResources().getTextArray(R.array.usage_messages);
			for (int i = 0; i < messages.length; i++) {
				SoundBite bite = createSoundBite(titles[i], messages[i]);
				insert(bite, db);				
			}
		}

		private SoundBite createSoundBite(CharSequence title, CharSequence message) {
			SoundBite bite = new SoundBite();
			bite.title = title.toString();
			bite.message = message.toString();
			bite.language = getString(R.string.language_value_us);
			bite.pitch = getString(R.string.pitch_value_normal);
			bite.speed = getString(R.string.speed_value_normal);
			bite.volume = getString(R.string.volume_value_normal);
			return bite;
		}
		
		private String getString(int resourceId) {
			return itsContext.getString(resourceId);
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + BITES_TABLE);
			onCreate(db);
		}

	}

	public DBHelper(Context context) {
		itsDbOpenHelper = new DBOpenHelper(context);
		itsDatabase = itsDbOpenHelper.getWritableDatabase();
	}

	public void close() {
		if (itsDatabase == null) {
			return;
		}
		itsDatabase.close();
		itsDatabase = null;
	}

	public long insert(SoundBite bite) {
		ContentValues values = createValue(bite);
		return itsDatabase.insert(BITES_TABLE, null, values);
	}

	private long insert(SoundBite bite, SQLiteDatabase db) {
		ContentValues values = createValue(bite);
		return db.insert(BITES_TABLE, null, values);
	}
	
	private ContentValues createValue(SoundBite bite) {
		ContentValues values = new ContentValues();
		values.put(TITLE_COLUMN, bite.title);
		values.put(TEXT_COLUMN, bite.message);
		values.put(LANGUAGE_COLUMN, bite.language);
		values.put(PITCH_COLUMN, bite.pitch);
		values.put(VOLUME_COLUMN, bite.volume);
		values.put(SPEED_COLUMN, bite.speed);
		return values;
	}

	public void update(SoundBite bite) {
		ContentValues values = createValue(bite);
		itsDatabase.update(BITES_TABLE, values, "_id=" + bite.id, null);
	}

	public void delete(SoundBite bite) {
		itsDatabase.delete(BITES_TABLE, "_id=" + bite.id, null);
	}

	public void delete(long biteId) {
		itsDatabase.delete(BITES_TABLE, "_id=" + biteId, null);
	}
	
	public void save(SoundBite bite) {
		if (bite.id == -1)
			insert(bite);
		else
			update(bite);		
	}

	public SoundBite read(Cursor cursor) {
		SoundBite bite = new SoundBite();
		bite.id = cursor.getLong(0);
		bite.title = cursor.getString(1);
		bite.message = cursor.getString(2);
		bite.language = cursor.getString(3);
		bite.pitch = cursor.getString(4);
		bite.volume = cursor.getString(5);
		bite.speed = cursor.getString(6);
		return bite;
	}

	public Cursor createCursor() {
		return itsDatabase.query(BITES_TABLE, COLUMNS, null, null, null, null, TITLE_COLUMN);
	}

}
