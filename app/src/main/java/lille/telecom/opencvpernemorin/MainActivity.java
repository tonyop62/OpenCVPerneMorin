package lille.telecom.opencvpernemorin;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements View.OnClickListener{

    private static final int IMAGE_PHOTOLIBRARY = 2;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MAX_SIZE = 2048;
    private Button captureBtn;
    private ImageView imageActivityMain;
    private Button libraryBtn;
    private Button photoMatchBtn;
    private Bitmap imageBitmap;
    private Uri uriFound;
    private RetainFragment dataFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        captureBtn = (Button)findViewById(R.id.captureBtn);
        captureBtn.setOnClickListener(this);
        
        libraryBtn = (Button)findViewById(R.id.photoLibraryBtn);
        libraryBtn.setOnClickListener(this);

        photoMatchBtn = (Button)findViewById(R.id.analysisBtn);
        photoMatchBtn.setOnClickListener(this);

        imageActivityMain = (ImageView)findViewById(R.id.imageActivityMain);

        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainFragment) fm.findFragmentByTag("data");

        // pour la rotation
        if(dataFragment == null){
            // création si pas de datafragment (lancement de l'activity)
            dataFragment = new RetainFragment();
            fm.beginTransaction().add(dataFragment, "data").commit();
            dataFragment.setData(this.imageBitmap);
        }else{
            // récupération du bitmap du dataFragment
            this.imageBitmap = this.dataFragment.getData();
            imageActivityMain.setImageBitmap(this.imageBitmap);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // recupération du bitmap lors de la destruction (pour la rotation)
        dataFragment.setData(this.imageBitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        this.imageBitmap = null;
        if(requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK){
            try {
                this.imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), this.uriFound);

                // redimenssionne image si trop large car elle ne s'affiche pas sinon
                if (this.imageBitmap.getHeight() > MAX_SIZE || this.imageBitmap.getWidth() > MAX_SIZE ) {
                    this.imageBitmap = Bitmap.createScaledBitmap(this.imageBitmap, MAX_SIZE, MAX_SIZE, false);
                    // rotation
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    this.imageBitmap = Bitmap.createBitmap(this.imageBitmap, 0, 0, this.imageBitmap.getWidth(), this.imageBitmap.getHeight(), matrix, true);
                }

                imageActivityMain.setImageBitmap(this.imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == IMAGE_PHOTOLIBRARY && resultCode == RESULT_OK){
            Uri photoUri = data.getData();
            this.uriFound = photoUri;
            try {
                this.imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);

                // redimenssionne image si trop large car elle ne s'affiche pas sinon
                if (this.imageBitmap.getHeight() > MAX_SIZE || this.imageBitmap.getWidth() > MAX_SIZE ) {
                    this.imageBitmap = Bitmap.createScaledBitmap(this.imageBitmap, MAX_SIZE, MAX_SIZE, false);
                }

                imageActivityMain.setImageBitmap(this.imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainFragment) fm.findFragmentByTag("data");

        if(dataFragment == null){
            dataFragment = new RetainFragment();
            fm.beginTransaction().add(dataFragment, "data").commit();
            dataFragment.setData(this.imageBitmap);
        }
    }

    @Override
    public void onClick(View view) {
        if(view == captureBtn){
            startCaptureActivity();
        }else if(view == libraryBtn){
            startPhotoLibraryActivity();
        }else if(view == photoMatchBtn){
            startPhotoMatchActivity();
        }
    }

    protected void startCaptureActivity() {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        this.uriFound = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
        Log.d("urifound", uriFound.toString());
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, this.uriFound);
        startActivityForResult(captureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    private void startPhotoLibraryActivity() {
        Intent libraryIntent = new Intent();
        libraryIntent.setType("image/*");
        libraryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(libraryIntent, "select location picture"), IMAGE_PHOTOLIBRARY);
    }

    private void startPhotoMatchActivity() {
        Intent photoMatchIntent = new Intent(this, PhotoMatchActivity.class);
        photoMatchIntent.putExtra("uriFound", this.uriFound);
        startActivity(photoMatchIntent);
    }

    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // todo : To be safe, you should check that the SDCard is mounted using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    // todo : changer la couleur du bouton quand on clic dessus (pour IHM) : https://openclassrooms.com/courses/creez-des-applications-pour-android/creation-de-vues-personnalisees
}
