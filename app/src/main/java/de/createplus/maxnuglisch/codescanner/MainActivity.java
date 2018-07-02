package de.createplus.maxnuglisch.codescanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    SurfaceView cameraView;
    TextView textView;
    CameraSource cameraSource;
    TextRecognizer textRecognizer;
    String[] brands = {"GAFFELS", "FLIMM", "JAGERMEI", "REISSDORF"};
    int[] brandIDS = {R.drawable.gaffels, R.drawable.flimm, R.drawable.jaegermeister, R.drawable.reisendorf};
    String[] brandCodeDef = {"9n", "5n", "6", "7"}; // n=only numbers

    String currentCode="";

    int state = 0; // 0=scan brand // 1=scan code // 2= locked

    int currentBrand = -1;
    ArrayList<String> recognizedBrands = new ArrayList<String>();
    final int RequestCameraPermissionID = 1001;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (SurfaceView) findViewById(R.id.surface_view);
        textView = (TextView) findViewById(R.id.text_view);
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        setupCamera();
        ImageButton btn = (ImageButton) findViewById(R.id.var_button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "state:" + state, Toast.LENGTH_SHORT).show();
                if (state == 0 && currentBrand != -1) {

                    state = 1;
                    ImageButton btn = (ImageButton) findViewById(R.id.var_button);
                    btn.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void setupCamera() {
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector not available");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < items.size(); i++) {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    //stringBuilder.append("\n");
                                }
                                String in = stringBuilder.toString();

                                if (state == 0) {
                                    for (int i = 0; i < brands.length; i++) {
                                        if (in.toUpperCase().replace(" ", "").contains(brands[i])) {
                                            recognizedBrands.add(brands[i]);

                                            ImageButton btn = (ImageButton) findViewById(R.id.var_button);
                                            btn.setImageResource(brandIDS[i]);
                                            currentBrand = i;


                                        }
                                    }
                                }

                                if (state == 1) {
                                    String filteredIN = "";
                                    if (brandCodeDef[currentBrand].contains("n")) {
                                        filteredIN = in.replaceAll("[^0-9]", "");
                                    } else {
                                        filteredIN = in;
                                    }

                                    textView.setText("Scanne den Code:\n" + filteredIN);
                                    textView.setTextColor(Color.WHITE);

                                    if(filteredIN.length() == Integer.parseInt(""+brandCodeDef[currentBrand].charAt(0))){
                                        textView.setText("Code:\n" + filteredIN);
                                        textView.setTextColor(Color.GREEN);
                                    }
                                }


                            }
                        });
                    }
                }
            });
        }
    }
}
