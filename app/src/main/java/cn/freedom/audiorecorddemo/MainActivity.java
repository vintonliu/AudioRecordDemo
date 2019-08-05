package cn.freedom.audiorecorddemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class MainActivity extends Activity implements EasyPermissions.PermissionCallbacks,
    View.OnClickListener{
    final static String TAG = "MainActivity";

    Button btn_rec_start = null;
    Button btn_rec_stop = null;
    Button btn_play_sample = null;
    Button btn_play_start = null;
    Button btn_play_stop = null;
    Button btn_speaker_on = null;
    Button btn_speaker_off = null;

    Spinner spn_rec_source = null;
    Spinner spn_rec_rate = null;
    Spinner spn_rec_channel = null;
    Spinner spn_play_stream = null;
    Spinner spn_play_rate = null;
    Spinner spn_play_channel = null;
    Spinner spn_audio_mode = null;

    TextView tv_audio_state = null;
    TextView tv_device_info = null;

    ArrayAdapter<CharSequence> mRecSrcAdapter = null;
    ArrayAdapter<CharSequence> mRecRateAdapter = null;
    ArrayAdapter<CharSequence> mRecChnlAdapter = null;
    ArrayAdapter<CharSequence> mPlayTypeAdapter = null;
    ArrayAdapter<CharSequence> mPlayRateAdapter = null;
    ArrayAdapter<CharSequence> mPlayChnlAdapter = null;
    ArrayAdapter<CharSequence> mAudioModeAdapter = null;

    static Context mContext = null;

    private PowerManager.WakeLock mWakeLock = null;
    private AudioManager mAudioManager = null;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        mContext = this;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);


        tv_audio_state = (TextView) findViewById(R.id.tv_audio_state);
        tv_device_info = (TextView) findViewById(R.id.tv_device_info);
        String deviceInfo = "Android SDK: " + Build.VERSION.SDK_INT + ", "
                            + "Release: " + Build.VERSION.RELEASE + ", "
                            + "Brand: " + Build.BRAND + ", "
                            + "Device: " + Build.DEVICE + ", "
                            + "Id: " + Build.ID + ", "
                            + "Hardware: " + Build.HARDWARE + ", "
                            + "Manufacturer: " + Build.MANUFACTURER + ", "
                            + "Model: " + Build.MODEL + ", "
                            + "Product: " + Build.PRODUCT;
        tv_device_info.setText(deviceInfo);


        btn_rec_start = (Button) findViewById(R.id.btn_rec_start);
        btn_rec_start.setOnClickListener(this);

        btn_rec_stop = (Button) findViewById(R.id.btn_rec_stop);
        btn_rec_stop.setOnClickListener(this);

        btn_play_sample = (Button) findViewById(R.id.btn_play_sample);
        btn_play_sample.setOnClickListener(this);

        btn_play_start = (Button) findViewById(R.id.btn_play_start);
        btn_play_start.setOnClickListener(this);

        btn_play_stop = (Button) findViewById(R.id.btn_play_stop);
        btn_play_stop.setOnClickListener(this);

        btn_speaker_on = (Button) findViewById(R.id.btn_speaker_on);
        btn_speaker_on.setOnClickListener(this);

        btn_speaker_off = (Button) findViewById(R.id.btn_speaker_off);
        btn_speaker_off.setOnClickListener(this);

        spn_rec_source = (Spinner) findViewById(R.id.spn_rec_source);
        mRecSrcAdapter = ArrayAdapter.createFromResource(this,
                R.array.rec_source_entries,
                android.R.layout.simple_spinner_item);
        spn_rec_source.setAdapter(mRecSrcAdapter);

        spn_rec_rate = (Spinner) findViewById(R.id.spn_rec_rate);
        mRecRateAdapter = ArrayAdapter.createFromResource(this,
                R.array.audio_sample_entries,
                android.R.layout.simple_spinner_item);
        spn_rec_rate.setAdapter(mRecRateAdapter);
        spn_rec_rate.setSelection(1);

        spn_rec_channel = (Spinner) findViewById(R.id.spn_rec_channel);
        mRecChnlAdapter = ArrayAdapter.createFromResource(this,
                R.array.rec_channel_entries,
                android.R.layout.simple_spinner_item);
        spn_rec_channel.setAdapter(mRecChnlAdapter);
        spn_rec_channel.setSelection(1);

        spn_play_stream = (Spinner) findViewById(R.id.spn_play_stream);
        mPlayTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.play_stream_type_entries,
                android.R.layout.simple_spinner_item);
        spn_play_stream.setAdapter(mPlayTypeAdapter);
        spn_play_stream.setSelection(1);

        spn_play_rate = (Spinner) findViewById(R.id.spn_play_rate);
        mPlayRateAdapter = ArrayAdapter.createFromResource(this,
                R.array.audio_sample_entries,
                android.R.layout.simple_spinner_item);
        spn_play_rate.setAdapter(mPlayRateAdapter);
        spn_play_rate.setSelection(1);

        spn_play_channel = (Spinner) findViewById(R.id.spn_play_channel);
        mPlayChnlAdapter = ArrayAdapter.createFromResource(this,
                R.array.play_channel_entries,
                android.R.layout.simple_spinner_item);
        spn_play_channel.setAdapter(mPlayChnlAdapter);
        spn_play_channel.setSelection(1);

        spn_audio_mode = (Spinner) findViewById(R.id.spn_audio_mode);
        mAudioModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.audio_mode_entries,
                android.R.layout.simple_spinner_item);
        spn_audio_mode.setAdapter(mAudioModeAdapter);
        spn_audio_mode.setSelection(0);

        if ( verifyPermissions()) {
            Log.i(TAG, "onCreate() has permissions.");
            AfterPermissionGranted();
        }
    }


    /**
     * Checks if the app has permission to write to device storage If the app
     * does not has permission then the user will be prompted to grant
     * permissions
     *
     */
    public static boolean verifyPermissions() {
        boolean hasPermission = true;
        if (!EasyPermissions.hasPermissions(mContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO)) {
            EasyPermissions.requestPermissions(mContext,
                    EasyPermissions.RC_CAMERA_PERM,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO);
            hasPermission = false;
        }

        return hasPermission;
    }

    /* (non-Javadoc)
	 * @see android.app.Activity#onRequestPermissionsResult(int, java.lang.String[], int[])
	 */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onRequestPermissionsResult requestCode = " + requestCode);

        for (String p : permissions) {
            Log.i(TAG, "onRequestPermissionsResult permissions = " + p);
        }

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /* (non-Javadoc)
     * @see cn.freedom.audiorecorddemo.EasyPermissions.PermissionCallbacks#onPermissionsGranted(int, java.util.List)
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
	 * @see cn.freedom.audiorecorddemo.EasyPermissions.PermissionCallbacks#onPermissionsDenied(int, java.util.List)
	 */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        // TODO Auto-generated method stub

    }

    private void AfterPermissionGranted() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_rec_start:
            {
                RecordTask recordTask = new RecordTask();
                RecordTask.Parameters parameters = recordTask.new Parameters();

                String strSource = spn_rec_source.getSelectedItem().toString();
                if (strSource.equals("MIC")) {
                    parameters.source = MediaRecorder.AudioSource.MIC;
                } else if (strSource.equals("VOICE_CALL")) {
                    parameters.source = MediaRecorder.AudioSource.VOICE_CALL;
                } else if (strSource.equals("VOICE_COMMUNICATION")) {
                    parameters.source = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
                } else if (strSource.equals("CAMCORDER")) {
                    parameters.source = MediaRecorder.AudioSource.CAMCORDER;
                } else {
                    parameters.source = MediaRecorder.AudioSource.DEFAULT;
                }

                String strChannels = spn_rec_channel.getSelectedItem().toString();
                if (strChannels.equals("CHANNEL_CONFIGURATION_MONO")) {
                    parameters.channels = AudioFormat.CHANNEL_CONFIGURATION_MONO;
                } else if (strChannels.equals("CHANNEL_CONFIGURATION_STEREO")) {
                    parameters.channels = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
                } else if (strChannels.equals("CHANNEL_IN_STEREO")) {
                    parameters.channels = AudioFormat.CHANNEL_IN_STEREO;
                } else {
                    parameters.channels = AudioFormat.CHANNEL_IN_MONO;
                }

                parameters.sampleRate = Integer.parseInt(spn_rec_rate.getSelectedItem().toString());

                isRecording = true;
                recordTask.execute(parameters);
            }
            break;

            case R.id.btn_rec_stop:
            {
                isRecording = false;
            }
            break;

            case R.id.btn_play_sample:
            case R.id.btn_play_start:
            {
                PlayTask playTask = new PlayTask();
                PlayTask.Parameters parameters = playTask.new Parameters();

                String strType = spn_play_stream.getSelectedItem().toString();
                if (strType.equals("STREAM_SYSTEM")) {
                    parameters.streamType = AudioManager.STREAM_SYSTEM;
                } else if (strType.equals("STREAM_MUSIC")) {
                    parameters.streamType = AudioManager.STREAM_MUSIC;
                } else {
                    parameters.streamType = AudioManager.STREAM_VOICE_CALL;
                }

                String strChannels = spn_play_channel.getSelectedItem().toString();
                if (strChannels.equals("CHANNEL_CONFIGURATION_MONO")) {
                    parameters.channels = AudioFormat.CHANNEL_CONFIGURATION_MONO;
                } else if (strChannels.equals("CHANNEL_CONFIGURATION_STEREO")) {
                    parameters.channels = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
                } else if (strChannels.equals("CHANNEL_OUT_STEREO")) {
                    parameters.channels = AudioFormat.CHANNEL_OUT_STEREO;
                } else {
                    parameters.channels = AudioFormat.CHANNEL_OUT_MONO;
                }

                parameters.sampleRate = Integer.parseInt(spn_play_rate.getSelectedItem().toString());
                parameters.recSampleRate = Integer.parseInt(spn_rec_rate.getSelectedItem().toString());

                if (v.getId() == R.id.btn_play_sample) {
                    parameters.isSample = true;
                } else {
                    parameters.isSample = false;
                }

                isPlaying = true;
                playTask.execute(parameters);
            }
            break;

            case R.id.btn_play_stop:
            {
                isPlaying = false;
            }
            break;

            case R.id.btn_speaker_on:
            {
                String strAM = spn_audio_mode.getSelectedItem().toString();
                int defmode = mAudioManager.getMode();
                int mode;
                if (strAM.equals("MODE_NORMAL")) {
                    mode = AudioManager.MODE_NORMAL;
                } else if (strAM.equals("MODE_IN_COMMUNICATION")) {
                    mode = AudioManager.MODE_IN_COMMUNICATION;
                } else {
                    mode = AudioManager.MODE_IN_CALL;
                }

                mAudioManager.setMode(mode);
                if (mAudioManager.getMode() != mode) {
                    tv_audio_state.setText("模式设置失败, 可能不支持此模式.");
                    mAudioManager.setMode(defmode);
                } else {
                    tv_audio_state.setText("模式设置成功.");
                }
                mAudioManager.setSpeakerphoneOn(true);
            }
            break;

            case R.id.btn_speaker_off:
            {
                String strAM = spn_audio_mode.getSelectedItem().toString();
                int mode = mAudioManager.getMode();
                if (strAM.equals("MODE_NORMAL")) {
                    mode = AudioManager.MODE_NORMAL;
                } else if (strAM.equals("MODE_IN_COMMUNICATION")) {
                    mode = AudioManager.MODE_IN_COMMUNICATION;
                } else {
                    mode = AudioManager.MODE_IN_CALL;
                }
                mAudioManager.setMode(mode);
                if (mAudioManager.getMode() != mode) {
                    tv_audio_state.setText("模式恢复失败, 可能不支持此模式");
                }
                mAudioManager.setSpeakerphoneOn(false);
            }
            break;
        }
    }

    protected class RecordTask extends AsyncTask<RecordTask.Parameters, Integer, Long> {
        public final class Parameters {
            int source;
            int sampleRate;
            int channels;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "onPreExecute()");
            btn_rec_start.setEnabled(false);
            btn_rec_stop.setEnabled(true);
            btn_play_stop.setEnabled(false);
            btn_play_start.setEnabled(false);
            btn_play_sample.setEnabled(false);
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            Log.i(TAG, "onPostExecute()");
            btn_rec_start.setEnabled(true);
            btn_rec_stop.setEnabled(true);
            btn_play_stop.setEnabled(true);
            btn_play_start.setEnabled(true);
            btn_play_sample.setEnabled(true);
        }

        @Override
        protected Long doInBackground(Parameters... params) {
            Log.i(TAG, "doInBackground()");

            RecordTask.Parameters param = params[0];

            // 根据定义好的几个配置，来获取合适的缓冲大小
            int bufferSize = AudioRecord.getMinBufferSize(param.sampleRate,
                    param.channels,
                    AudioFormat.ENCODING_PCM_16BIT);

            bufferSize *= 2;
            AudioRecord audioRecord = null;

            try {
                // 实例化AudioRecord
                audioRecord = new AudioRecord(param.source,
                                                    param.sampleRate,
                                                    param.channels,
                                                    AudioFormat.ENCODING_PCM_16BIT,
                                                    bufferSize);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            // 开始录制
            try {
                audioRecord.startRecording();

                final AudioRecord tmpRecord = audioRecord;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "AudioRecord "
                                + "session ID: " + tmpRecord.getAudioSessionId() + ", "
                                + "audio format: " + tmpRecord.getAudioFormat() + ", "
                                + "channels: " + tmpRecord.getChannelCount() + ", "
                                + "sample rate: " + tmpRecord.getSampleRate(), Toast.LENGTH_LONG).show();
                    }
                });

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }

            FileOutputStream fosRecord = null;
            try {
                fosRecord = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/recordOut.pcm");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            try {
                int bytesPer10ms = 2 * param.channels * (param.sampleRate * 10 / 1000);
                int bytesRead;
                byte[] tempBuf = new byte[bytesPer10ms];
                // 定义循环，根据isRecording的值来判断是否继续录制
                while (isRecording) {
                    bytesRead = audioRecord.read(tempBuf, 0, bytesPer10ms);
                    // 循环将buffer中的音频数据写入到OutputStream中
                    if (fosRecord != null) {
                        try {
                            fosRecord.write(tempBuf, 0, bytesRead);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 录制结束
            audioRecord.stop();
            audioRecord.release();

            try {
                if (fosRecord != null) {
                    fosRecord.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    protected class PlayTask extends AsyncTask<PlayTask.Parameters, Integer, Long> {
        public final class Parameters {
            int streamType;
            int sampleRate;
            int channels;
            int recSampleRate;
            boolean isSample;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(TAG, "onPreExecute()");
            btn_rec_start.setEnabled(false);
            btn_rec_stop.setEnabled(false);
            btn_play_stop.setEnabled(true);
            btn_play_start.setEnabled(false);
            btn_play_sample.setEnabled(false);
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);
            Log.i(TAG, "onPostExecute()");
            btn_rec_start.setEnabled(true);
            btn_rec_stop.setEnabled(true);
            btn_play_stop.setEnabled(true);
            btn_play_start.setEnabled(true);
            btn_play_sample.setEnabled(true);
        }

        @Override
        protected Long doInBackground(Parameters... params) {
            Log.i(TAG, "doInBackground()");
            Parameters param = params[0];
            int sampleRate;
            if (param.isSample) {
                sampleRate = param.sampleRate;
            } else {
                sampleRate = param.recSampleRate;
            }
            int bufferSize = AudioTrack.getMinBufferSize(sampleRate,
                    param.channels,
                    AudioFormat.ENCODING_PCM_16BIT);

            // 实例AudioTrack
            AudioTrack audioTrack = null;
            try {
                audioTrack = new AudioTrack(param.streamType,
                        param.sampleRate,
                        param.channels,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            // 定义输入流，将音频写入到AudioTrack类中，实现播放
            FileInputStream fis = null;
            InputStream inputStream = null;
            if (param.isSample) {
                String filename;
                if (param.sampleRate == 8000) {
                    filename = "cuiniao8k.pcm";
                } else if (param.sampleRate == 44100) {
                    filename = "cuiniao441k.pcm";
                } else {
                    filename = "cuiniao.pcm";
                }
                try {
                    inputStream = mContext.getResources().getAssets().open(filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    fis = new FileInputStream(Environment.getExternalStorageDirectory().getPath() + "/recordOut.pcm");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            // 开始播放
            try {
                audioTrack.play();

                final AudioTrack tmpTrack = audioTrack;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "AudioTrack "
                                + "session ID: " + tmpTrack.getAudioSessionId() + ", "
                                + "audio format: " + tmpTrack.getAudioFormat() + ", "
                                + "channels: " + tmpTrack.getChannelCount() + ", "
                                + "sample rate: " + tmpTrack.getSampleRate(), Toast.LENGTH_LONG).show();
                    }
                });

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            int bytesPer10ms = 2 * param.channels * (sampleRate * 10 / 1000);
            int bytesRead;
            byte[] tempBuf = new byte[bytesPer10ms];

            // 由于AudioTrack播放的是流，所以，我们需要一边播放一边读取
            try {
                if ((inputStream != null && inputStream.available() > 0) ||
                        (fis != null && fis.available() > 0)) {
                    while (isPlaying) {
                        if (param.isSample) {
                            bytesRead = inputStream.read(tempBuf, 0, bytesPer10ms);
                        } else {
                            bytesRead = fis.read(tempBuf, 0, bytesPer10ms);
                        }
                        // 然后将数据写入到AudioTrack中
                        if (bytesRead == bytesPer10ms) {
                            audioTrack.write(tempBuf, 0, bytesPer10ms);
                        } else {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 播放结束
            try {
                if (param.isSample) {
                    inputStream.close();
                } else {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            audioTrack.stop();
            audioTrack.release();

            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PowerManager pManager = ((PowerManager) getSystemService(POWER_SERVICE));
        mWakeLock = pManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, "unlock");
        mWakeLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mWakeLock) {
            mWakeLock.release();
        }
    }
}
