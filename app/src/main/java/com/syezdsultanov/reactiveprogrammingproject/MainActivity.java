package com.syezdsultanov.reactiveprogrammingproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";
    private final static String JPG_FILE = "jpg.jpg";
    private final static String PNG_FILE = "png.png";
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button convertButton = findViewById(R.id.convert_button);
        mImageView = findViewById(R.id.imageView);
        convertButton.setOnClickListener(v -> convertToPNG());
        downloadJPGFile();
    }

    public Observable<Response> getData() {
        final OkHttpClient client = new OkHttpClient();
        return Observable.create(emitter -> {
            try {
                final ConnectivityManager connectivityManager =
                        ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE));
                assert connectivityManager != null;
                if (connectivityManager.getActiveNetworkInfo() != null
                        && connectivityManager.getActiveNetworkInfo().isConnected()) {
                    Request request = new Request.Builder()
                            .url("http://personal.psu.edu/xqz5228/jpg.jpg")
                            .build();
                    Response response = client.newCall(request).execute();
                    emitter.onNext(response);
                    emitter.onComplete();
                }
            } catch (IOException e) {
                e.printStackTrace();
                emitter.onError(e);
            }
        });
    }

    public void convertToPNG() {
        Observable.fromCallable(() -> readImage(JPG_FILE))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new DisposableObserver<Bitmap>() {
                    @Override
                    public void onNext(Bitmap bitmap) {
                        Bitmap bmp = readImage(JPG_FILE);
                        FileOutputStream out;
                        try {
                            out = openFileOutput(PNG_FILE,
                                    Context.MODE_PRIVATE);
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                            out.flush();
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        TextView mTextview = findViewById(R.id.text);
                        mTextview.setText(R.string.png_image);
                    }
                });


    }

    void downloadJPGFile() {
        getData().observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(new DisposableObserver<Response>() {
                    @Override
                    public void onNext(Response response) {
                        try {
                            writeImage(JPG_FILE, response.body().bytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        Bitmap jpgBitmap = readImage(JPG_FILE);
                        mImageView.post(() -> mImageView.setImageBitmap(jpgBitmap));
                    }
                });
    }

    public void writeImage(String fileName, byte[] bytes) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(fileName,
                    Context.MODE_PRIVATE);
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap readImage(String fileName) {
        Bitmap img = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = openFileInput(fileName);
            byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);
            img = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IOException ex) {
            Log.d(LOG_TAG, "" + ex.getMessage());
        } finally {
            try {
                if (fileInputStream != null)
                    fileInputStream.close();
            } catch (IOException ex) {
                Log.d(LOG_TAG, "" + ex.getMessage());
            }
        }
        return img;
    }
}
