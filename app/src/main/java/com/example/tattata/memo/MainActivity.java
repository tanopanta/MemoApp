package com.example.tattata.memo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    EditText editText;
    SharedPreferences pref;

    String fileName;

    private static final int REQUEST_CODE_VOICE = 1000;
    private static final int REQUEST_CODE_EXPORT = 1234;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonSave = (Button)findViewById(R.id.buttonSave);
        Button buttonCancel = (Button)findViewById(R.id.buttonCancel);
        ImageButton buttonVoice = (ImageButton)findViewById(R.id.imageButtonVoice);

        editText = (EditText)findViewById(R.id.editText);
        pref = getSharedPreferences("memoPref", MODE_PRIVATE);


        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "保存が完了しました。", Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("memo", editText.getText().toString());
                editor.commit();
                finishApp();
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               finishApp();
            }
        });
        buttonVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "音声を入力");
                startActivityForResult(intent, REQUEST_CODE_VOICE);
            }
        });

        editText.setBackgroundColor(Color.rgb(245, 245, 220));
        editText.setText(pref.getString("memo", ""));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.item1:
                //共有
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, editText.getText().toString());
                startActivity(shareIntent);
                return true;
            case R.id.item2:
                exportFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }
    public void finishApp() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    public void exportFile() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Toast.makeText(getApplicationContext(), "Androidのバージョンが古いので無理です。", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText editView = new EditText(MainActivity.this);
        editView.setText(".txt");
        //左端にカーソルが移動するように
        editView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editView.setSelection(0);
                editView.setOnClickListener(null);
            }
        });

        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("ファイル名を入力してください");
        dialog.setView(editView);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    return;
                }
                fileName = editView.getText().toString();

                StorageManager sm = (StorageManager)getSystemService(Context.STORAGE_SERVICE);
                StorageVolume volume = sm.getPrimaryStorageVolume();

                Intent intent = volume.createAccessIntent(Environment.DIRECTORY_DOWNLOADS);
                startActivityForResult(intent, REQUEST_CODE_EXPORT);
            }
        });
        dialog.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                return;
            }
        });

        dialog.show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_EXPORT:
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    return;
                }
                Uri uri = intent.getData();
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, uri);

                //text/plainとかだと勝手に拡張子を付けちゃう
                DocumentFile newFile = pickedDir.createFile("text/spain", fileName);
                try {
                    OutputStream out = getContentResolver().openOutputStream(newFile.getUri());
                    out.write(editText.getText().toString().getBytes());
                    out.close();
                    Toast.makeText(getApplicationContext(), fileName + "をダウンロードフォルダに保存しました。", Toast.LENGTH_LONG).show();
                } catch (FileNotFoundException e) {
                    Toast.makeText(getApplicationContext(), "FileNotFoundException", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "IOException", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                break;
            case REQUEST_CODE_VOICE:
                ArrayList<String> results = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if(results.size() > 0) {
                    // 認識結果候補で一番有力なものを表示
                    editText.getText().insert(editText.getSelectionEnd(), results.get(0));
                }
                break;
            default:
                break;
        }


    }
}
