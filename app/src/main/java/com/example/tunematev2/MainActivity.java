package com.example.tunematev2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

    private TextView emotionResult;
    private Button spotifyPlaylistBtn;
    private Button textSendBtn;
    private EditText editText;
    private SpeechRecognizer speechRecognizer;

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

        Button voiceCommandBtn = findViewById(R.id.voiceCommandBtn); // 음성 인식 시작 버튼
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
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                emotionResult.setText("오류 발생: 다시 시도하세요.");
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    String recognizedText = matches.get(0);
                    emotionResult.setText("분석 중...");

                    TextRequest textRequest = new TextRequest(recognizedText);
                    Call<PlaylistResponse> call = apiService.sendText(textRequest);

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
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        // 음성 인식 시작 버튼 클릭 리스너
        voiceCommandBtn.setOnClickListener(v -> startSpeechRecognition());

        // 텍스트 전송 버튼 클릭 리스너
        textSendBtn.setOnClickListener(v -> {
            String userInput = editText.getText().toString(); // 사용자가 입력한 텍스트
            if (!userInput.isEmpty()) {
                TextRequest textRequest = new TextRequest(userInput);
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
            } else {
                Toast.makeText(MainActivity.this, "텍스트를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 음성 인식 시작 메소드
    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "음성을 입력하세요");
        speechRecognizer.startListening(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    public void onFailure(Call<PlaylistResponse> call, Throwable t) {
        // 네트워크 오류의 원인 출력
        emotionResult.setText("네트워크 오류: " + t.getMessage());
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
    private String emotion;

    @SerializedName("playlist_url")  // 서버의 JSON 응답에서 "playlist_url"을 매핑
    private String playlistUrl;

    public String getEmotion() {
        return emotion;
    }

    public String getPlaylistUrl() {
        return playlistUrl;
    }
}
