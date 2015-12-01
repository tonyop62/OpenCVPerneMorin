package lille.telecom.opencvpernemorin;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class PhotoMatchActivity extends Activity {

    private static final String TAG = "PhotoMatchActivity";
    private ImageView imageView;
    private Bitmap photoRecup;

    private static final int DISTANCE_MAX= 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_match);

        // récupération de la photo
        Bundle extra = getIntent().getExtras();
        if(extra != null){
            Uri photoUri = (Uri)extra.get("uriFound");
            try {
                this.photoRecup = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.imageView = (ImageView)findViewById(R.id.photoRecup);
            this.imageView.setImageBitmap(this.photoRecup);

            /******* récupération de tous les drawables ******/
            // On récupère l'ensemble des "champs" de la classe R.drawable
            Field[] fields = R.drawable.class.getFields();

            for (Field field : fields) {
                try {

                    // pour ne prendre que les photos a comparer
                    if(!field.getName().startsWith("abc", 0)) {

                        // Pour chaque champ, on récupère sa valeur (c'est à dire l'identifiant de la ressource)
                        int currentResId = field.getInt(R.drawable.class);

                        Log.d("nomdraw", field.getName());


                        // traitement opencv

                        OpenCVLoader.initDebug();

                        Mat matRecup = new Mat();
                        Utils.bitmapToMat(photoRecup, matRecup);

                        // test passage en gris
                        Mat matGray = Highgui.imread(photoUri.getPath(), Highgui.IMREAD_GRAYSCALE);

                        // récup Mat depuis drawable
                        Mat m = new Mat();
                        try {

                            m = Utils.loadResource(this, currentResId, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
                            Log.d("drawable", m.dump());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
                    DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
                    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

                    // 1er descripteur
                    Mat descriptors1 = new Mat();
                    MatOfKeyPoint keypoints1 = new MatOfKeyPoint();

                    detector.detect(matRecup, keypoints1);
                    descriptor.compute(matRecup, keypoints1, descriptors1);

                    // 2e descripteur
                    Mat descriptors2 = new Mat();
                    MatOfKeyPoint keypoints2 = new MatOfKeyPoint();

                    detector.detect(m, keypoints2);
                    descriptor.compute(m, keypoints2, descriptors2);

                    // points "matchés"
                    MatOfDMatch matches = new MatOfDMatch();
                    matcher.match(descriptors1, descriptors2, matches);

                    Log.d("matches", matches.dump());

                    List<DMatch> matchesList = matches.toList();
                    List<DMatch> matchesListFinal = new ArrayList<DMatch>();

                    for(int i=0 ; i < matchesList.size() ; i++){
                        Log.d("matchesstring" ,matchesList.get(i).toString()); // donne DMatch [queryIdx=54, trainIdx=139, imgIdx=0, distance=54.0]
                        // queryIdx représente keypoints1
                        // trainIdx représente keypoints2
                        // distance à laquelle se trouve les deux points

                        if(matchesList.get(i).distance <= this.DISTANCE_MAX){
                            matchesListFinal.add(matchesList.get(i));
                        }

                    }

                    Toast.makeText(this.getApplicationContext(), String.valueOf(matchesListFinal.size()), Toast.LENGTH_LONG).show();
                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();;
                }
            }





        }
        // todo : ajouter photo dans un coin pour rappeler la photo qu'on vient de capturer qu'on veut faire analyser
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_photo_match, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
