package lille.telecom.opencvpernemorin;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.features2d.DMatch;

import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
//            this.photoRecup = BitmapFactory.decodeFile(photoUri.getPath());
            this.imageView = (ImageView)findViewById(R.id.photoRecup);
            this.imageView.setImageBitmap(this.photoRecup);

            Log.d("path", photoUri.getPath());

            OpenCVLoader.initDebug();
//            Mat matRecup = Highgui.imread(photoUri.getPath(), Highgui.IMREAD_GRAYSCALE);

            Mat matRecup = new Mat();
            Utils.bitmapToMat(photoRecup, matRecup);

            // test passage en gris
//                Bitmap bmp = null;
//                Utils.matToBitmap(matRecup, bmp);
//                this.imageView.setImageBitmap(bmp);

            // récup Mat depuis drawable
            Mat m = new Mat();
            try {
                m = Utils.loadResource(this, R.drawable.frame_18, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
                Log.d("drawable", m.dump());
            } catch (IOException e) {
                e.printStackTrace();
            }

            FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
            DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            Mat descriptors1 = new Mat();
            MatOfKeyPoint keypoints1 = new MatOfKeyPoint();

            detector.detect(matRecup, keypoints1);
            descriptor.compute(matRecup, keypoints1, descriptors1);

            Mat descriptors2 = new Mat();
            MatOfKeyPoint keypoints2 = new MatOfKeyPoint();

            detector.detect(m, keypoints2);
            descriptor.compute(m, keypoints2, descriptors2);

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

            File sdCard = Environment.getExternalStorageDirectory();
            Toast.makeText(this.getApplicationContext(),
                    sdCard.getAbsolutePath(),
                    Toast.LENGTH_LONG).show(); // return false

            File dir = new File (sdCard.getAbsolutePath() + "/opencv_perne_morin");
            dir.mkdirs();
            File file = new File(dir, "mat_store.json");

            FileOutputStream f = null;
            try {
                f = new FileOutputStream(file);
                f.write(matToJson(descriptors1).getBytes());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            /***************** Serialisation marche pas ***********/
//                ObjectOutputStream os = new ObjectOutputStream(f);
//                os.writeObject(matRecup);
//                os.close();
//                f.close();





        }
        // todo : ajouter photo dans un coin pour rappeler la photo qu'on vient de capturer qu'on veut faire analyser
    }

    public static String matToJson(Mat mat){
        JsonObject obj = new JsonObject();

        if(mat.isContinuous()){
            int cols = mat.cols();
            int rows = mat.rows();
            int elemSize = (int) mat.elemSize();

            byte[] data = new byte[cols * rows * elemSize];

            mat.get(0, 0, data);

            obj.addProperty("rows", mat.rows());
            obj.addProperty("cols", mat.cols());
            obj.addProperty("type", mat.type());

            // We cannot set binary data to a json object, so:
            // Encoding data byte array to Base64.
            String dataString = new String(Base64.encode(data, Base64.DEFAULT));

            obj.addProperty("data", dataString);

            Gson gson = new Gson();
            String json = gson.toJson(obj);

            return json;
        } else {
            Log.e(TAG, "Mat not continuous.");
        }
        return "{}";
    }

    public static Mat matFromJson(String json){
        JsonParser parser = new JsonParser();
        JsonObject JsonObject = parser.parse(json).getAsJsonObject();

        int rows = JsonObject.get("rows").getAsInt();
        int cols = JsonObject.get("cols").getAsInt();
        int type = JsonObject.get("type").getAsInt();

        String dataString = JsonObject.get("data").getAsString();
        byte[] data = Base64.decode(dataString.getBytes(), Base64.DEFAULT);

        Mat mat = new Mat(rows, cols, type);
        mat.put(0, 0, data);

        return mat;
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
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
