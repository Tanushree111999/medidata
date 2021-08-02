package org.tensorflow.lite.codelabs.medidata;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.Continuation;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;

import android.os.Bundle;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MedidataClassificationDemo";

    private TextView resultTextView;
    private EditText inputEditText;
    private ExecutorService executorService;
    private ScrollView scrollView;
    private Button predictButton;

    // TODO 5: Define a NLClassifier variable
    private NLClassifier textClassifier;
    private static final String MODEL_PATH = "medi_data.tflite";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG, "onCreate");

        executorService = Executors.newSingleThreadExecutor();
        resultTextView = findViewById(R.id.result_text_view);
        inputEditText = findViewById(R.id.input_text);
        scrollView = findViewById(R.id.scroll_view);

        predictButton = findViewById(R.id.predict_button);
        predictButton.setOnClickListener(
                (View v) -> {
                    classify(inputEditText.getText().toString());
                });

        // TODO 3: Call the method to download TFLite model
        downloadModel("medi_data");
    }

    /** Send input text to TextClassificationClient and get the classify messages. */
    private void classify(final String text) {
        executorService.execute(
                () -> {
                    // TODO 7: Run sentiment analysis on the input text
                    List<Category> results = textClassifier.classify(text);

                    // TODO 8: Convert the result to a human-readable text
                    String textToShow = "Input: " + text + "\nOutput:\n";
                    for (int i = 0; i < results.size(); i++) {
                        Category result = results.get(i);
                        textToShow +=
                                String.format("    %s: %s\n", result.getLabel(), result.getScore());
                    }
                    textToShow += "---------\n";

                    // Show classification result on screen
                    showResult(textToShow);
                });
    }

    /** Show classification result on the screen. */
    private void showResult(final String textToShow) {
        // Run on UI thread as we'll updating our app UI
        runOnUiThread(
                () -> {
                    // Append the result to the UI.
                    resultTextView.append(textToShow);

                    // Clear the input text.
                    inputEditText.getText().clear();

                    // Scroll to the bottom to show latest entry's classification result.
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                });
    }

    // TODO 2: Implement a method to download TFLite model from Firebase
    /** Download model from Firebase ML. */
    private synchronized void downloadModel(String modelName) {
        final FirebaseCustomRemoteModel remoteModel =
                new FirebaseCustomRemoteModel
                        .Builder(modelName)
                        .build();
        FirebaseModelDownloadConditions conditions =
                new FirebaseModelDownloadConditions.Builder()
                        .requireWifi()
                        .build();
        final FirebaseModelManager firebaseModelManager = FirebaseModelManager.getInstance();
        firebaseModelManager
                .download(remoteModel, conditions)
                .continueWithTask(task ->
                        firebaseModelManager.getLatestModelFile(remoteModel)
                )
                .continueWith(executorService, (Continuation<File, Void>) task -> {
                    // Initialize a text classifier instance with the model
                    File modelFile = task.getResult();

                    //String MODEL_PATH = modelFile.toString();
                    // TODO 6: Initialize a TextClassifier with the downloaded model
                    textClassifier = NLClassifier.createFromFile(MainActivity.this,MODEL_PATH);

                    // Enable predict button
                    predictButton.setEnabled(true);
                    return null;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to download and initialize the model. ", e);
                    Toast.makeText(
                            MainActivity.this,
                            "Model download failed, please check your connection.",
                            Toast.LENGTH_LONG)
                            .show();
                    predictButton.setEnabled(false);
                });
    }
}
