package com.example.tunematev2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {android.Manifest.permission.RECORD_AUDIO};

    private TextView emotionResult;
    private Button spotifyPlaylistBtn;
    private Button textSendBtn;
    private Button voiceCommandBtn; // 음성 인식 시작 버튼
    private EditText editText;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false; // 음성 인식 상태를 관리하는 변수

    // Retrofit 인터페이스 정의
    public interface ApiService {
        @POST("/receive_text")  // 서버 엔드포인트 수정
        Call<PlaylistResponse> sendText(@Body TextRequest text);
    }

    // Retrofit 객체 초기화
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)  // build.gradle에 정의한 BASE_URL 사용
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    // ApiService 인스턴스 생성
    ApiService apiService = retrofit.create(ApiService.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 권한 요청
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        voiceCommandBtn = findViewById(R.id.voiceCommandBtn); // 음성 인식 시작 버튼
        textSendBtn = findViewById(R.id.sendTextBtn); // 텍스트 전송 버튼
        emotionResult = findViewById(R.id.emotionResult);
        spotifyPlaylistBtn = findViewById(R.id.spotifyPlaylistBtn);
        editText = findViewById(R.id.textInput); // 사용자가 입력한 텍스트

        // 음성 인식 설정
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                emotionResult.setText("음성 입력 중...");
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                emotionResult.setText("음성 인식 완료");
            }

            @Override
            public void onError(int error) {
                emotionResult.setText("오류 발생: 다시 시도하세요.");
                isListening = false; // 오류 발생 시 음성 인식 상태를 중단으로 설정
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0); // 음성 인식 결과로 얻은 텍스트
                    emotionResult.setText("분석 중: " + recognizedText);

                    // 음성 인식 결과로 얻은 텍스트를 서버로 전송
                    sendTextToServer(recognizedText);
                } else {
                    emotionResult.setText("음성 인식 실패: 다시 시도하세요.");
                }
                isListening = false; // 음성 인식이 끝나면 상태를 중단으로 설정
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        // 음성 인식 시작 및 중단 버튼 클릭 리스너
        voiceCommandBtn.setOnClickListener(v -> {
            if (isListening) {
                stopSpeechRecognition(); // 이미 인식 중이면 중단
            } else {
                startSpeechRecognition(); // 인식 중이 아니면 시작
            }
        });

        // 텍스트 전송 버튼 클릭 리스너
        textSendBtn.setOnClickListener(v -> {
            String userInput = editText.getText().toString(); // 사용자가 입력한 텍스트
            if (!userInput.isEmpty()) {
                sendTextToServer(userInput); // 사용자가 직접 입력한 텍스트 전송
            } else {
                Toast.makeText(MainActivity.this, "텍스트를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 음성 인식 시작 메소드
    private void startSpeechRecognition() {
        if (!permissionToRecordAccepted) {
            Toast.makeText(this, "녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "음성을 입력하세요");
        speechRecognizer.startListening(intent);
        isListening = true; // 음성 인식 중으로 설정
    }

    // 음성 인식 중단 메소드
    private void stopSpeechRecognition() {
        if (isListening && speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false; // 중단 상태로 설정
            emotionResult.setText("음성 인식 중단됨");
        }
    }

    // 텍스트를 서버로 전송하는 메소드
    private void sendTextToServer(String text) {
        TextRequest textRequest = new TextRequest(text);
        Call<PlaylistResponse> call = apiService.sendText(textRequest);

        // 서버로 텍스트 전송 및 응답 처리
        call.enqueue(new Callback<PlaylistResponse>() {
            @Override
            public void onResponse(Call<PlaylistResponse> call, Response<PlaylistResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String playlistUrl = response.body().getPlaylistUrl();
                    emotionResult.setText("Spotify playlist link: " + playlistUrl);

                    // 전송 성공 시 토스트 메시지 출력
                    Toast.makeText(MainActivity.this, "전송 성공: " + playlistUrl, Toast.LENGTH_SHORT).show();

                    spotifyPlaylistBtn.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(playlistUrl));
                        startActivity(browserIntent);
                    });
                    spotifyPlaylistBtn.setVisibility(Button.VISIBLE);
                } else {
                    // 응답이 실패했거나 응답 본문이 없는 경우
                    emotionResult.setText("서버 응답 오류: 다시 시도하세요.");
                    Toast.makeText(MainActivity.this, "전송 실패: 서버 응답 오류", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PlaylistResponse> call, Throwable t) {
                // 네트워크 오류의 원인 출력
                emotionResult.setText("네트워크 오류: " + t.getMessage());
                Toast.makeText(MainActivity.this, "전송 실패: 네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionToRecordAccepted = requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!permissionToRecordAccepted) {
            Toast.makeText(this, "녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}

// 데이터 클래스 정의
class TextRequest {
    private String text;

    TextRequest(String text) {
        this.text = text;
    }
}

// 서버 응답을 처리하는 PlaylistResponse 클래스
class PlaylistResponse {
    @SerializedName("playlist_url")  // 서버의 JSON 응답에서 "playlist_url"을 매핑
    private String playlistUrl;

    public String getPlaylistUrl() {
        return playlistUrl;
    }
}
