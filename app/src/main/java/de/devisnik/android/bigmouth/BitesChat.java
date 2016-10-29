package de.devisnik.android.bigmouth;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import de.devisnik.android.bigmouth.data.SoundBite;
import java.util.HashMap;
import java.util.Locale;

public class BitesChat extends AppCompatActivity implements OnInitListener {

  private static final int DIALOG_INSTALL_TTS = 555;
  private static final int CHECK_TTS = 11;

  private static final Logger LOGGER = new Logger(BitesChat.class);

  private TextToSpeech itsTextToSpeech;
  private int itsRestoreAudio = -1;
  private EditText input;

  private class InstallTTSDialogBuilder extends Builder {

    public InstallTTSDialogBuilder() {
      super(BitesChat.this);
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
    setContentView(R.layout.activity_bites_chat);

    input = (EditText) findViewById(R.id.chat_input);

    findViewById(R.id.chat_send).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        SoundBite bite = createBiteWithDefaults();
        bite.message = input.getText().toString();
        speak(bite);
      }
    });
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

  private void startCheckTTS() {
    Intent ttsIntent = new Intent();
    ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
    startActivityForResult(ttsIntent, CHECK_TTS);
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
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void onPause() {
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
        adjustAudio(itsRestoreAudio);
      }
    });
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

  void stopSpeaking() {
    if (itsTextToSpeech == null || !itsTextToSpeech.isSpeaking()) {
      return;
    }
    itsTextToSpeech.stop();
    adjustAudio(itsRestoreAudio);
  }
}
