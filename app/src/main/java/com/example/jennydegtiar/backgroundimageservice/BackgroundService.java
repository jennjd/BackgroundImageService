package com.example.jennydegtiar.backgroundimageservice;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


import com.example.jennydegtiar.backgroundimageservice.encryptor.EncryptorFacade;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by Jenny.Degtiar on 12/20/2017.
 */

public class BackgroundService extends IntentService {

    private String lastImageChecked = "";
    private String cameraFolderBase = "/storage/emulated/0/WhatsApp/Media/WhatsApp Images/";
    public String hostName = "54.165.181.155:8080";
    private String sharonUrl = "http://"+hostName+"/en-creep-t/classify/?phoneNumber=1";
    private Double threshold = 0.5;
    private int numberOfPicturesToRunTheProcess = 3;
//    private String url = "http://maps.googleapis.com/maps/api/geocode/xml?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&sensor=false";
//    private String url = "http://10.0.2.2/cfc/iphoneWebservice.cfc?returnformat=json&amp;method=testUpload";
    public BackgroundService() {
        super("");
    }



    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        //String dataString = workIntent.getDataString();

//        while (true) {
//            encryptFiles();
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        try {
            encryptFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void encryptFiles() throws Exception {
        File[] files = getFiles();
        sortFiles(files);
        // GO OVER 3 FILES
        //int length = files.length;
        int fileIndex =0;
        int counter = 0;
        boolean isFirst = true;
        String newLastCheck = "";
        while (fileIndex < files.length && counter < numberOfPicturesToRunTheProcess) {
            if (!files[fileIndex].isDirectory()) {
                if (isFirst){
                    newLastCheck = files[fileIndex].getName();
                    isFirst = false;
                }
                counter++;
                if (!files[fileIndex].getName().equals(lastImageChecked)) {
                    //send multipart file
                    encryptIfNeeded(files[fileIndex].getPath());
                }
                else {
                    break;
                }
            }
            fileIndex++;
        }

        lastImageChecked = newLastCheck;
    }

    private void encryptIfNeeded(final String filePath) throws Exception {

        final boolean[] shouldEncrypt = {false};
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        Service service = new Retrofit.Builder().baseUrl(sharonUrl).client(client).build().create(Service.class);
        File file = new File(filePath);
        if(isTemporaryFile(file)) {
            return;
        }
        RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), reqFile);
        RequestBody name = RequestBody.create(MediaType.parse("text/plain"), "upload_test");

//            Log.d("THIS", data.getData().getPath());

        retrofit2.Call<okhttp3.ResponseBody> req = service.postImage(body, name);
        req.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String scoreStr = json.getString("score");
                    Double score = Double.valueOf(scoreStr);
                    if(score > threshold) {
                        Log.i("","Photo should be encrypted: " + filePath);
                        EncryptorFacade.encryptImage(filePath, "123", "1234");
                    }
                    Log.i("", "Got score for picture: " + score);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ImageWriteException e) {
                    e.printStackTrace();
                } catch (ImageReadException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public String executeMultipartPost(String filePath) throws Exception {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Bitmap bm = BitmapFactory.decodeFile(filePath);
            bm.compress(Bitmap.CompressFormat.JPEG, 75, bos);
            byte[] data = bos.toByteArray();
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(sharonUrl);
            ByteArrayBody bab = new ByteArrayBody(data, "forest.jpg");
            // File file= new File("/mnt/sdcard/forest.png");
            // FileBody bin = new FileBody(file);
            MultipartEntity reqEntity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("file", bab);
            //reqEntity.addPart("photoCaption", new StringBody("sfsdfsdf"));
            String boundary = "*****";
            postRequest.setEntity(reqEntity);
            postRequest.addHeader("Content-Type", "multipart/form-data");
            HttpResponse response = httpClient.execute(postRequest);


            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent(), "UTF-8"));
            String sResponse;
            StringBuilder s = new StringBuilder();

            while ((sResponse = reader.readLine()) != null) {
                s = s.append(sResponse);
            }
            JSONObject json = new JSONObject(s.toString());    // create JSON obj from string
            String score = json.getString("score");
            System.out.println("Response: " + s);

            return score;
        } catch (Exception e) {
            // handle exception here
            Log.e(e.getClass().getName(), e.getMessage());
        }
        return null;
    }


    public String execute2(String sourceFileUri) throws IOException {
        String charset = "UTF-8";
        File binaryFile = new File(sourceFileUri);
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        URLConnection connection = new URL(sharonUrl).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (
                OutputStream output = connection.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
        ) {
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            copy(binaryFile, output);
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF).flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

// Request is lazily fired whenever you need to obtain information about response.
        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        return "" + responseCode;
    }

    private File[] getFiles() {
        File imgFile = new File(cameraFolderBase);
        return imgFile.listFiles();
    }

    private void sortFiles(File[] files) {
        Arrays.sort(files, new Comparator<File>(){
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
            }
        });
    }

    public static void copy(File src, OutputStream dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
                // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                dst.write(buf, 0, len);
            }
        }
     }

    private boolean isTemporaryFile(File file) {
        return file.getAbsolutePath().endsWith("_temp.jpg");
    }

}

