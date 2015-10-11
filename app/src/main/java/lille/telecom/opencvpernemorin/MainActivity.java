package lille.telecom.opencvpernemorin;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener{

    static final String tag = MainActivity.class.getName();
    private static final int IMAGE_CAPTURE = 1;
    private static final int IMAGE_PHOTOLIBRARY = 2;
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
//        super.onActivityResult(requestCode, resultCode, data);
        this.imageBitmap = null;
        if(requestCode == IMAGE_CAPTURE && resultCode == RESULT_OK){
            Bundle extra = data.getExtras();
            this.imageBitmap = (Bitmap)extra.get("data");
//            this.uriFound = getImageUri(getApplicationContext(), this.imageBitmap); // Marche pas car la photo n'est pas enregistrée, faut envoyer le descriptor opencv
            imageActivityMain.setImageBitmap(this.imageBitmap);
        }
        else if(requestCode == IMAGE_PHOTOLIBRARY && resultCode == RESULT_OK){
            Uri photoUri = data.getData();
            this.uriFound = photoUri;
            try {
                this.imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
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
        startActivityForResult(captureIntent, IMAGE_CAPTURE);
    }

    private void startPhotoLibraryActivity() {
        Intent libraryIntent = new Intent();
        libraryIntent.setType("image/*");
        libraryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(libraryIntent, "select location picture"), IMAGE_PHOTOLIBRARY);
        // todo : si image grande, impossible upload voir photo.compress
    }

    private void startPhotoMatchActivity() {
        Intent photoMatchIntent = new Intent(this, PhotoMatchActivity.class);
        photoMatchIntent.putExtra("uriFound", this.uriFound);
        startActivity(photoMatchIntent);
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    // todo : changer la couleur du bouton quand on clic dessus (pour IHM) : https://openclassrooms.com/courses/creez-des-applications-pour-android/creation-de-vues-personnalisees
}
