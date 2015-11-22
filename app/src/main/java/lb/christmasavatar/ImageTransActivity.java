package lb.christmasavatar;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import org.lucasr.twowayview.ItemClickSupport;
import org.lucasr.twowayview.widget.TwoWayView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 11/18/2015.
 */
public class ImageTransActivity extends AppCompatActivity implements View.OnClickListener {

    private int CAMERA_REQUEST = 123;

    private ImageView imvMain;
    private ImageView imvCover;
    private LinearLayout lnlSave;
    private LinearLayout lnlShare;
    private TwoWayView twoWayView;
    private RelativeLayout rltImage;

    private List<Integer> images;
    private List<Integer> thumbs;
    private ShapeAdapter shapeAdapter;

    private String filename;
    private Toast toast;
    private ProgressDialog progress_dialog;

    private boolean isShowAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_trans);

        isShowAd = false;

        initView();
        initData();
    }

    private void initView() {
        imvMain = (ImageView) findViewById(R.id.imvMain);
        imvCover = (ImageView) findViewById(R.id.imvCover);
        lnlSave = (LinearLayout) findViewById(R.id.lnlSave);
        lnlShare = (LinearLayout) findViewById(R.id.lnlShare);
        twoWayView = (TwoWayView) findViewById(R.id.twoWayView);
        rltImage = (RelativeLayout) findViewById(R.id.rltImage);

        lnlSave.setOnClickListener(this);
        lnlShare.setOnClickListener(this);

        twoWayView.setHasFixedSize(true);
    }

    private void initData() {
        filename = null;

        if (getIntent().hasExtra("camera")) {
            openCamera();
        } else {
            openImagePicker();
        }

        images = new ArrayList<>();
        images.add(R.drawable.chirst_1);
        images.add(R.drawable.chirst_2);
        images.add(R.drawable.chirst_3);
        images.add(R.drawable.chirst_4);
        images.add(R.drawable.chirst_5);
        images.add(R.drawable.chirst_6);
        images.add(R.drawable.chirst_7);

        thumbs = new ArrayList<>();
        thumbs.add(R.drawable.thumb_1);
        thumbs.add(R.drawable.thumb_2);
        thumbs.add(R.drawable.thumb_3);
        thumbs.add(R.drawable.thumb_4);
        thumbs.add(R.drawable.thumb_5);
        thumbs.add(R.drawable.thumb_6);
        thumbs.add(R.drawable.thumb_7);

        shapeAdapter = new ShapeAdapter(this, twoWayView, thumbs, 0);
        twoWayView.setAdapter(shapeAdapter);

        imvCover.setImageResource(images.get(0));

        ItemClickSupport itemClick = ItemClickSupport.addTo(twoWayView);
        itemClick.setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View child, int position, long id) {
                imvCover.setImageResource(images.get(position));
                shapeAdapter.chooseItem(position);
                filename = null;
                rltImage.invalidate();
            }
        });
    }

    @Override
    public void onBackPressed() {
//        Intent returnIntent = new Intent();
//        if (isShowAd) {
//        setResult(Activity.RESULT_OK, returnIntent);
//        } else {
//            setResult(Activity.RESULT_CANCELED, returnIntent);
//        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteTempFile();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lnlSave:
                SaveImageAsync saveImageAsync = new SaveImageAsync();
                saveImageAsync.execute();
                break;
            case R.id.lnlShare:
                ShareImageAsync shareImageAsync = new ShareImageAsync();
                shareImageAsync.execute();
                break;
        }
    }

    private void openImagePicker() {
        Crop.pickImage(this);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, createTempFile());
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(data.getData());
        } else if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            handleCrop(resultCode, data);
        } else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Uri uri = getTempFile();
            beginCrop(uri);
        } else {
            finish();
        }
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
        Crop.of(source, destination).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            Uri selectedImageUri = Crop.getOutput(result);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                int screenWidth = getScreenWidth();
                if (bitmap.getWidth() > screenWidth) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, screenWidth, screenWidth, false);
                }
                imvMain.setImageBitmap(bitmap);
            } catch (Exception e) {

            }
        } else if (resultCode == Crop.RESULT_ERROR) {
            showToast(Crop.getError(result).getMessage());
        }
    }

    private class SaveImageAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (filename == null) {
                Bitmap bitmap = convertViewToBitmap();
                filename = saveBitmapToSDCard(bitmap);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            hideProgressDialog();
            showToast("Saved");
            isShowAd = true;
        }
    }

    private class ShareImageAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (filename == null) {
                Bitmap bitmap = convertViewToBitmap();
                filename = saveBitmapToSDCard(bitmap);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            hideProgressDialog();
            shareImage(filename);
            isShowAd = true;
        }
    }

    private Bitmap convertViewToBitmap() {
        rltImage.setDrawingCacheEnabled(true);
        rltImage.buildDrawingCache();
        return rltImage.getDrawingCache();
    }

    private String saveBitmapToSDCard(Bitmap bitmap) {
        String imageName = "ChristmasAvatar_" + System.currentTimeMillis();
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/ChristmasAvatar");

        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        String fname = imageName + ".png";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            ContentValues image = new ContentValues();
            image.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {

        }
    }

    private File saveImageCameraToSDCard(Bitmap bitmap) {
        deleteTempFile();
        String imageName = "temp";
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/temp");

        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        String fname = imageName + ".png";
        File file = new File(myDir, fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
//            ContentValues image = new ContentValues();
//            image.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
//            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {

        }
    }

    private void deleteTempFile() {
        String imageName = "temp";
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/temp");

        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        String fname = imageName + ".jpg";
        String fpath = myDir.getAbsolutePath() + "/" + fname;

        File file = new File(fpath);
        file.delete();

        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(fpath))));
    }

    private Uri createTempFile() {
        deleteTempFile();
        String imageName = "temp";
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/temp");

        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        String fname = imageName + ".jpg";
        File file = new File(myDir, fname);
        Uri uri = Uri.fromFile(file);

//        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        return uri;
    }

    private Uri getTempFile() {
        String imageName = "temp";
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/temp");
        String fname = imageName + ".jpg";
        File file = new File(myDir, fname);
        Uri uri = Uri.fromFile(file);

        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        return uri;
    }

    private int getScreenWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    // Method to share any image.
    private void shareImage(String path) {
        Intent share = new Intent(Intent.ACTION_SEND);

        // If you want to share a png image only, you can do:
        // setType("image/png"); OR for jpeg: setType("image/jpeg");
        share.setType("image/*");

        // Make sure you put example png image named myImage.png in your
        // directory
        String imagePath = Environment.getExternalStorageDirectory()
                + "/loading.png";

        File imageFileToShare = new File(path);

        Uri uri = Uri.fromFile(imageFileToShare);
        share.putExtra(Intent.EXTRA_STREAM, uri);

        startActivity(Intent.createChooser(share, "Share Image!"));
    }

    private void showToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        toast.setText(msg);
        toast.show();
    }

    public void showProgressDialog() {
        if (progress_dialog == null) {
            progress_dialog = new ProgressDialog(this);
        }

        if (!progress_dialog.isShowing()) {
            progress_dialog.setMessage("Progressing ... ");
            progress_dialog.setCancelable(false);
            progress_dialog.show();
        }
    }

    public void hideProgressDialog() {
        if (progress_dialog != null && progress_dialog.isShowing()) {
            progress_dialog.dismiss();
        }
    }
}
