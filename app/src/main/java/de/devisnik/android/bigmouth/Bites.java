package de.devisnik.android.bigmouth;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import de.devisnik.android.bigmouth.data.DBHelper;
import de.devisnik.android.bigmouth.data.SoundBite;
import java.util.HashMap;
import java.util.Locale;

public class Bites extends AppCompatActivity implements OnInitListener, AdapterView.OnItemClickListener {

  private static final int DIALOG_STOP_SPEAKING = 333;
  private static final int DIALOG_CONFIRM_DELETE = 444;
  private static final int DIALOG_INSTALL_TTS = 555;
  private static final int CHECK_TTS = 11;
  private static final int EDITOR = 22;

  private static final Logger LOGGER = new Logger(Bites.class);

  private TextToSpeech itsTextToSpeech;
  private SimpleCursorAdapter itsAdapter;
  private SoundBite itsItemToDelete;
  private int itsRestoreAudio = -1;
  private DBHelper itsDbHelper;

  @Override
  public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
    SoundBite soundBite = itsDbHelper.read(cursor);
    speak(soundBite);
  }

  private class InstallTTSDialogBuilder extends Builder {

    public InstallTTSDialogBuilder() {
      super(Bites.this);
      setTitle(R.string.dialog_install_tts_title);
      setMessage(R.string.dialog_install_tts_message);
      setPositiveButton(R.string.dialog_install_tts_button, new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          Intent installIntent = new Intent();
          installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
          installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          try {
            startActivity(installIntent);
          } catch (ActivityNotFoundException e) {
            LOGGER.e("while starting TTS installer.", e);
          }
        }
      });
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    LOGGER.d("onCreate");
    super.onCreate(savedInstanceState);
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    setContentView(R.layout.bites);
    itsDbHelper = new DBHelper(this);
    fillInData();
    getListView().setOnItemClickListener(this);
    registerForContextMenu(getListView());
  }

  private ListView getListView() {
    return (ListView) findViewById(android.R.id.list);
  }

  @Override
  protected void onStart() {
    LOGGER.d("onStart");
    super.onStart();
    startCheckTTS();
  }

  @Override
  protected void onRestart() {
    LOGGER.d("onRestart");
    super.onRestart();
  }

  @Override
  protected void onStop() {
    LOGGER.d("onStop");
    if (itsTextToSpeech != null) {
      itsTextToSpeech.shutdown();
    }
    itsTextToSpeech = null;
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    itsDbHelper.close();
    super.onDestroy();
  }

  private void startCheckTTS() {
    Intent ttsIntent = new Intent();
    ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
    startActivityForResult(ttsIntent, CHECK_TTS);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    new MenuInflater(this).inflate(R.menu.bites_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
      case R.id.add_bite:
        startEditor(createBiteWithDefaults());
        return true;
      case R.id.settings:
        startSettings();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private SoundBite createBiteWithDefaults() {
    SoundBite bite = new SoundBite();
    bite.language = getPrefValue(R.string.pref_language);
    bite.pitch = getPrefValue(R.string.pref_pitch);
    bite.speed = getPrefValue(R.string.pref_speed);
    bite.volume = getPrefValue(R.string.pref_volume);
    return bite;
  }

  private String getPrefValue(int resourceId) {
    return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(resourceId),
        null);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    LOGGER.d("onActivityResult");
    switch (requestCode) {
      case CHECK_TTS:
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
          itsTextToSpeech = new TextToSpeech(this, this);
        } else {
          showDialog(DIALOG_INSTALL_TTS);
        }
        return;
      case EDITOR:
        handleEditorResult(resultCode, data);
        return;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void handleEditorResult(int resultCode, Intent data) {
    LOGGER.d("handleEditorResult");
    if (data == null || resultCode == BiteEditor.BITE_ABORTED) {
      return;
    }
    SoundBite bite = (SoundBite) data.getSerializableExtra(BiteEditor.BITE_DATA);
    if (resultCode == BiteEditor.BITE_SAVED) {
      save(bite);
    }
    if (resultCode == BiteEditor.BITE_DELETED) {
      confirmAndDelete(bite);
    }
  }

  void confirmAndDelete(SoundBite bite) {
    itsItemToDelete = bite;
    showDialog(DIALOG_CONFIRM_DELETE);
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    if (id == DIALOG_CONFIRM_DELETE) {
      ((AlertDialog) dialog).setMessage(itsItemToDelete.title);
    }
    super.onPrepareDialog(id, dialog);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (itsItemToDelete != null) {
      outState.putSerializable("itemToDelete", itsItemToDelete);
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle state) {
    itsItemToDelete = (SoundBite) state.getSerializable("itemToDelete");
    super.onRestoreInstanceState(state);
  }

  private void delete(SoundBite bite) {
    itsDbHelper.delete(bite);
    itsAdapter.getCursor().requery();
    toast(R.string.toast_deleted);
  }

  private void save(SoundBite bite) {
    itsDbHelper.save(bite);
    itsAdapter.getCursor().requery();
    toast(R.string.toast_saved);
  }

  private void toast(int stringId) {
    Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onPause() {
    removeDialog(DIALOG_STOP_SPEAKING);
    stopSpeaking();
    super.onPause();
  }

  @Override
  public void onInit(int status) {
    LOGGER.d("onInit");
  }

  private Locale parseLocale(String localeString) {
    String[] splitData = localeString.split("-");
    return new Locale(splitData[0], splitData[1]);
  }

  private void speak(SoundBite bite) {
    if (itsTextToSpeech == null) {
      LOGGER.e("TTS not properly set up!");
      return;
    }
    itsTextToSpeech.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

      @Override
      public void onUtteranceCompleted(String utteranceId) {
        dismissDialog(DIALOG_STOP_SPEAKING);
        adjustAudio(itsRestoreAudio);
      }
    });
    showDialog(DIALOG_STOP_SPEAKING);
    if (getString(R.string.volume_value_no_adjust).equals(bite.volume)) {
      itsRestoreAudio = getCurrentAudio();
    } else {
      itsRestoreAudio = adjustAudio(Math.round(getMaxAudio() * Float.parseFloat(bite.volume)));
    }
    HashMap<String, String> params = new HashMap<String, String>();
    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "text");
    itsTextToSpeech.setPitch(Float.parseFloat(bite.pitch));
    itsTextToSpeech.setLanguage(parseLocale(bite.language));
    itsTextToSpeech.setSpeechRate(Float.parseFloat(bite.speed));
    itsTextToSpeech.speak(bite.message, TextToSpeech.QUEUE_FLUSH, params);
  }

  private int getMaxAudio() {
    AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    return audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
  }

  private int getCurrentAudio() {
    AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC);
  }

  private int adjustAudio(int value) {
    AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    int oldVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
    return oldVolume;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id == DIALOG_STOP_SPEAKING) {
      return new StopSpeakingDialogBuilder(this).create();
    }
    if (id == DIALOG_CONFIRM_DELETE) {
      return new ConfirmDeleteDialogBuilder(this).create();
    }
    if (id == DIALOG_INSTALL_TTS) {
      return new InstallTTSDialogBuilder().create();
    }
    return super.onCreateDialog(id);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    new MenuInflater(this).inflate(R.menu.bites_context, menu);
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Cursor cursor = (Cursor) getListView().getItemAtPosition(info.position);
    SoundBite soundBite = itsDbHelper.read(cursor);
    switch (item.getItemId()) {
      case R.id.context_speak:
        speak(soundBite);
        return true;
      case R.id.context_edit:
        startEditor(soundBite);
        return true;
      case R.id.context_delete:
        confirmAndDelete(soundBite);
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  private void startEditor(SoundBite bite) {
    Intent intent = createEditorIntent(bite);
    startActivityForResult(intent, EDITOR);
  }

  private void startSettings() {
    Intent intent = createSettingsIntent();
    startActivityForResult(intent, 1234);
  }

  private Intent createSettingsIntent() {
    Intent intent = new Intent(this, BitePreferences.class);
    return intent;
  }

  private Intent createEditorIntent(SoundBite soundBite) {
    Intent intent = new Intent();
    intent.setClass(this, BiteEditor.class);
    intent.putExtra(BiteEditor.BITE_DATA, soundBite);
    return intent;
  }

  private void fillInData() {
    Cursor cursor = itsDbHelper.createCursor();
    startManagingCursor(cursor);
    itsAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, cursor,
        new String[] { DBHelper.TITLE_COLUMN, DBHelper.TEXT_COLUMN }, new int[] {
        android.R.id.text1, android.R.id.text2
    });
    getListView().setAdapter(itsAdapter);
  }

  void stopSpeaking() {
    if (itsTextToSpeech == null || !itsTextToSpeech.isSpeaking()) {
      return;
    }
    itsTextToSpeech.stop();
    adjustAudio(itsRestoreAudio);
  }

  public void onDeleteConfirmed() {
    delete(itsItemToDelete);
  }
}
