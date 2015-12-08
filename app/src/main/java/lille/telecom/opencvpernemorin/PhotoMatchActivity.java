package lille.telecom.opencvpernemorin;

import android.app.Activity;
import android.content.res.Resources;
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
import java.util.SortedMap;
import java.util.TreeMap;


public class PhotoMatchActivity extends Activity {

    private ImageView imageView;
    private Bitmap photoRecup;

    private static final int DISTANCE_MAX= 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_match);

        Bundle extra = getIntent().getExtras();
        if(extra != null){
            // récupération de la photo
            Uri photoUri = (Uri)extra.get("uriFound");
            try {
                this.photoRecup = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.imageView = (ImageView)findViewById(R.id.imageRecup);

            // redimenssionne image si trop large car elle ne s'affiche pas sinon
            if (this.photoRecup.getHeight() > 2048 || this.photoRecup.getWidth() > 2048 ) {
                this.photoRecup = Bitmap.createScaledBitmap(this.photoRecup, 2048, 2048, false);
            }
            this.imageView.setImageBitmap(this.photoRecup);

            // liste des imageView contenat les images trouvés
            List<ImageView> listImageTrouve = new ArrayList<>();
            listImageTrouve.add((ImageView)findViewById(R.id.imageTrouve1));
            listImageTrouve.add((ImageView)findViewById(R.id.imageTrouve2));
            listImageTrouve.add((ImageView)findViewById(R.id.imageTrouve3));

            // récupération de l'ensemble des "champs" de la classe R.drawable
            Field[] fields = R.drawable.class.getFields();

            // traitement opencv
            OpenCVLoader.initDebug();

            Mat matRecup = new Mat();
            Utils.bitmapToMat(photoRecup, matRecup);

            // passage en gris de l'image a comparer
//            Mat matGray = Highgui.imread(photoUri.getPath(), Highgui.IMREAD_GRAYSCALE); // todo : marche pas dans les traitements opencv (surement recupération marche pas)

            SortedMap<Integer, Integer> drawableTrie = new TreeMap<>();
            drawableTrie.clear();

            for (Field field : fields) {
                try {
                    // pour ne prendre que les photos a comparer
                    if(!field.getName().startsWith("abc", 0)) {

                        // Pour chaque champ, on récupère sa valeur (c'est à dire l'identifiant de la ressource)
                        int currentResId = field.getInt(R.drawable.class);

                        Log.d("nomdraw", field.getName());

                        // récup Mat depuis drawable
                        Mat m = new Mat();
                        try {
                            m = Utils.loadResource(this, currentResId, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
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

                        List<DMatch> matchesList = matches.toList();
                        List<DMatch> matchesListFinal = new ArrayList<>();

                        for (int i = 0; i < matchesList.size(); i++) {
                            Log.d("matchesstring", matchesList.get(i).toString()); // donne DMatch [queryIdx=54, trainIdx=139, imgIdx=0, distance=54.0]
                            // queryIdx représente keypoints1
                            // trainIdx représente keypoints2
                            // distance à laquelle se trouve les deux points

                            if (matchesList.get(i).distance <= DISTANCE_MAX) {
                                matchesListFinal.add(matchesList.get(i));
                            }

                        }

//                        Toast.makeText(this.getApplicationContext(), String.valueOf(matchesListFinal.size()), Toast.LENGTH_SHORT).show();

//                        if(matchesListFinal.size() > this.scoreMax){
//                            this.scoreMax = matchesListFinal.size();
//                            drawableMatche = field.getName();
//                        }
//                        Toast.makeText(this.getApplicationContext(), "score : " + this.scoreMax, Toast.LENGTH_SHORT).show();
//                        Toast.makeText(this.getApplicationContext(), drawableMatche, Toast.LENGTH_LONG).show();
//                        Toast.makeText(this.getApplicationContext(), drawableMatche, Toast.LENGTH_LONG).show();

                        drawableTrie.put(matchesListFinal.size(), currentResId);
                    }

                    Log.d("tree", drawableTrie.toString());

                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

            // affichraage des images trouvés
            if(!drawableTrie.isEmpty() && drawableTrie.size() >= 3) {
                for (int i = 0; i<3; i++) {
                    Resources resources = this.getApplicationContext().getResources();
                    listImageTrouve.get(i).setImageDrawable(resources.getDrawable(drawableTrie.get(drawableTrie.lastKey()))); // todo : erreur out of memory
                    drawableTrie.remove(drawableTrie.lastKey());
                }

            }else{
                Toast.makeText(this.getApplicationContext(), "Pas assez de photo référence pour comparer", Toast.LENGTH_LONG).show();
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
