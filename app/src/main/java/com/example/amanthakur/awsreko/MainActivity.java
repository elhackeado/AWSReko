package com.example.amanthakur.awsreko;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

//import sun.applet.Main;

import static android.os.Environment.DIRECTORY_PICTURES;

public class MainActivity extends AppCompatActivity {

    Float similarityThreshold = 70F;
    String path = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString();
    String sourceImage = path+"/source.jpg";
    String targetImage = path+"/target.jpg";
    ByteBuffer sourceImageBytes=null;
    ByteBuffer targetImageBytes=null;
    Button button;
    AmazonRekognition amazonRekognitionClient;
    CompareFacesResult compareFacesResult;
    MainActivity mn;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mn = MainActivity.this;
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                credentialsProvider();
                startRunning();
            }
        });



    }

    public void credentialsProvider(){

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-south-1:fe3bd17c-bec8-4ce7-be59-0a4d05193346", // Identity pool ID
                Regions.AP_SOUTH_1 // Region
        );

        setAmazonRekognitionClient(credentialsProvider);
    }

    /**
     *  Create a AmazonS3Client constructor and pass the credentialsProvider.
     * @param credentialsProvider
     */
    public void setAmazonRekognitionClient(CognitoCachingCredentialsProvider credentialsProvider){

        // Create an S3 client
        amazonRekognitionClient = new AmazonRekognitionClient(credentialsProvider);

        // Set the region of your Rekognition
        amazonRekognitionClient.setRegion(Region.getRegion(Regions.AP_SOUTH_1));

    }

    public void startRunning(){

        //Load source and target images and create input parameters
        try (InputStream inputStream = new FileInputStream(new File(sourceImage))) {
            sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }
        catch(Exception e)
        {
            Toast.makeText(MainActivity.this,"Failed to load source image " + sourceImage,Toast.LENGTH_LONG).show();

        }
        try (InputStream inputStream = new FileInputStream(new File(targetImage))) {
            targetImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
        }
        catch(Exception e)
        {
            //System.out.println("Failed to load target images: " + targetImage);
            Toast.makeText(MainActivity.this,"Failed to load target images: " + targetImage,Toast.LENGTH_LONG).show();
        }

        Image source=new Image()
                .withBytes(sourceImageBytes);
        Image target=new Image()
                .withBytes(targetImageBytes);

        final CompareFacesRequest request = new CompareFacesRequest()
                .withSourceImage(source)
                .withTargetImage(target)
                .withSimilarityThreshold(similarityThreshold);

        new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    // Call operation
                    compareFacesResult=amazonRekognitionClient.compareFaces(request);
                    // Display results
                    List <CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();
                    for (CompareFacesMatch match: faceDetails){
                        final ComparedFace face= match.getFace();
                        final BoundingBox position = face.getBoundingBox();
                        //System.out.println("Face at " + position.getLeft().toString()
                        // + " " + position.getTop()
                        //  + " matches with " + face.getConfidence().toString()
                        // + "% confidence.");
                        mn.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this,"Face at " + position.getLeft().toString()
                                        + " " + position.getTop()
                                        + " matches with " + face.getConfidence().toString()
                                        + "% confidence.",Toast.LENGTH_LONG).show();
                            }
                        });


                    }
                    final List<ComparedFace> uncompared = compareFacesResult.getUnmatchedFaces();

                    //System.out.println("There was " + uncompared.size()
                    //      + " face(s) that did not match");
                    mn.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this,"There was " + uncompared.size()
                                    + " face(s) that did not match",Toast.LENGTH_LONG).show();
                        }
                    });

                    //  System.out.println("Source image rotation: " + compareFacesResult.getSourceImageOrientationCorrection());

                    mn.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this,"Source image rotation: " + compareFacesResult.getSourceImageOrientationCorrection(),Toast.LENGTH_LONG).show();
                        }
                    });
                    //Toast.makeText(MainActivity.this,"Source image rotation: " + compareFacesResult.getSourceImageOrientationCorrection(),Toast.LENGTH_LONG).show();
                    //   System.out.println("target image rotation: " + compareFacesResult.getTargetImageOrientationCorrection());
                    mn.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this,"target image rotation: " + compareFacesResult.getTargetImageOrientationCorrection(),Toast.LENGTH_LONG).show();
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }
}
