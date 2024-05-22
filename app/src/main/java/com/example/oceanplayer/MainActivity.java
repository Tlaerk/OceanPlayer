package com.example.oceanplayer;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ADD_AUDIO = 1;

    private MediaPlayer mediaPlayer;
    private List<Uri> audioUris = new ArrayList<>();
    private int currentTrackIndex = 0;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private TextView tvAudioCount;
    private TextView tvCurrentAudio;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPaused = false;
    private Button btnPlayPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAddAudio = findViewById(R.id.btnAddAudio);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        Button btnNext = findViewById(R.id.btnNext);
        Button btnPrevious = findViewById(R.id.btnPrevious);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvAudioCount = findViewById(R.id.tvAudioCount);
        tvCurrentAudio = findViewById(R.id.tvCurrentAudio);

        btnAddAudio.setOnClickListener(v -> addAudio());
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNextAudio());
        btnPrevious.setOnClickListener(v -> playPreviousAudio());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
                if (mediaPlayer != null) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });
    }

    private void addAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE_ADD_AUDIO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_AUDIO && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri audioUri = data.getClipData().getItemAt(i).getUri();
                    audioUris.add(audioUri);
                }
            } else if (data.getData() != null) {
                Uri audioUri = data.getData();
                audioUris.add(audioUri);
            }
            tvAudioCount.setText("Total Audios: " + audioUris.size());
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            pauseAudio();
        } else {
            resumeAudio();
        }
    }

    private void resumeAudio() {
        if (mediaPlayer != null && isPaused) {
            mediaPlayer.start();
            isPaused = false;
            btnPlayPause.setText("Pause");
        } else {
            playAudio();
        }
    }

    private void playAudio() {
        if (!audioUris.isEmpty()) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(this, audioUris.get(currentTrackIndex));
                mediaPlayer.prepare();
                mediaPlayer.start();

                seekBar.setMax(mediaPlayer.getDuration());
                tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
                tvCurrentAudio.setText("Now Playing: " + getFileName(audioUris.get(currentTrackIndex)));
                handler.post(updateSeekBar);

                btnPlayPause.setText("Pause");

                mediaPlayer.setOnCompletionListener(mp -> {
                    handler.removeCallbacks(updateSeekBar);
                    playNextAudio();
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void playNextAudio() {
        if (!audioUris.isEmpty()) {
            currentTrackIndex++;
            if (currentTrackIndex >= audioUris.size()) {
                currentTrackIndex = 0;
            }
            playAudio();
        }
    }

    private void playPreviousAudio() {
        if (!audioUris.isEmpty()) {
            currentTrackIndex--;
            if (currentTrackIndex < 0) {
                currentTrackIndex = audioUris.size() - 1;
            }
            playAudio();
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
            btnPlayPause.setText("Play");
        }
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
                handler.postDelayed(this, 1000);
            }
        }
    };

    private String formatTime(int milliseconds) {
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        int seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
