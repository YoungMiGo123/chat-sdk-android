/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package co.chatsdk.core.image;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntegerRes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import co.chatsdk.core.R;
import co.chatsdk.core.session.ChatSDK;

import static android.os.Environment.isExternalStorageRemovable;


public class ImageUtils {

    static {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    public static File getDiskCacheDir(Context context) {
        return getDiskCacheDir(context, ChatSDK.config().imageDirectoryName);
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
                        context.getCacheDir().getPath();

        if (uniqueName == null || uniqueName.isEmpty()) {
            uniqueName = context.getResources().getString(R.string.app_name);
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public static File getFileInCacheDirectory(Context context, String name, String ext) {
        return getFileInCacheDirectory(getDiskCacheDir(context), name, ext);
    }

    public static File getFileInCacheDirectory(File dir, String name, String ext) {
        if (!dir.exists()){
            if (!dir.mkdir()) {
                return null;
            }
        }
        if (name.contains(ext)) {
            return new File(dir, name);
        }
        return new File(dir, name + ext);
    }

    public static File createEmptyFileInCacheDirectory(File dir, String name, String ext) {
        return createEmptyFileInCacheDirectory(dir, name, ext, true);
    }

    public static File createEmptyFileInCacheDirectory(Context context, String name, String ext) {
        return createEmptyFileInCacheDirectory(getDiskCacheDir(context), name, ext);
    }

    public static File createEmptyFileInCacheDirectory(Context context, String name, String ext, boolean addRandomIdToName) {
        return createEmptyFileInCacheDirectory(getDiskCacheDir(context), name, ext, addRandomIdToName);
    }

    public static File createEmptyFileInCacheDirectory(File dir, String name, String ext, boolean addRandomIdToName) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return null;
            }
        }
        if (name.contains(ext)) {
            return new File(dir, name);
        }
        if (!addRandomIdToName) {
            return new File(dir, name + ext);
        } else {
            String fileName = (name += UUID.randomUUID()).replace("@", "_");
            File file = new File(dir, fileName + ext);
            while (file.exists()) {
                fileName = (name += UUID.randomUUID()).replace("@", "_");
                file = new File(dir, fileName + ext);
            }
            return file;
        }
    }

//    public static File compressImageToFile(Bitmap bitmap, File outFile, Bitmap.CompressFormat format) {
//        try {
//            OutputStream outStream = new FileOutputStream(outFile);
//            bitmap.compress(format, 100, outStream);
//            outStream.flush();
//            outStream.close();
//        }
//        catch (Exception e) {
//            ChatSDK.logError(e);
//            return null;
//        }
//        return outFile;
//    }
//
//    public static File compressImageToFile(Bitmap bitmap, File outFile) {
//        String path = outFile.getPath();
//        String ext = path.substring(path.lastIndexOf(".")).toLowerCase();
//        Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
//        if (ext == "png") {
//            format = Bitmap.CompressFormat.PNG;
//        }
//        return compressImageToFile(bitmap, outFile, format);
//    }
//
//    public static File compressImageToFile(Context context, Bitmap bitmap, String name, String ext, boolean addRandomIdToName) {
//        File outFile = createEmptyFileInCacheDirectory(context, name, ext, addRandomIdToName);
//        return compressImageToFile(bitmap, outFile);
//    }
//
//    public static File compressImageToFile(Context context, Bitmap bitmap, String name, String ext) {
//        return compressImageToFile(context, bitmap, name, ext, true);
//    }
//
//    public static File compressImageToFile(Context context, String filePath, String name, String ext) {
//        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
//        return compressImageToFile(context, bitmap, name, ext);
//    }

    public static File saveBitmapToFile(Context context, Bitmap bitmap) {
        File outFile = createEmptyFileInCacheDirectory(context, "Image", "jpg", true);
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outFile;
    }

    /**
     * Constructing a bitmap that contains the given bitmaps(max is three).
     *
     * For given two bitmaps the result will be a half and half bitmap.
     *
     * For given three the result will be a half of the first bitmap and the second
     * half will be shared equally by the two others.
     *
     * @param  bitmaps Array of bitmaps to use for the final image.
     * @param  w width of the final image, A positive number.
     * @param  h height of the final image, A positive number.
     *
     * @return A Bitmap containing the given images.
     * */
    public static Bitmap getMixImagesBitmap(int w, int h, Bitmap...bitmaps){

        if (h == 0 || w == 0 || bitmaps.length == 0) {
            return null;
        }

        Bitmap finalImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        finalImage.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(finalImage);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

        int margin = 1;

        int w_2 = w/2 - margin;
        int x_2 = w/2 + margin;

        int h_2 = h/2 - margin;
        int y_2 = h/2 + margin;

        if(bitmaps.length == 1) {
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[0], w, h), 0, 0, paint);
        }
        else if (bitmaps.length == 2) {
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[0], w_2, h), 0, 0, paint);
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[1], w_2, h), x_2, 0, paint);
        }
        else if (bitmaps.length == 3) {
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[0], w_2, h), 0, 0, paint);
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[1], w_2, h_2), x_2, 0, paint);
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[2], w_2, h_2), x_2, y_2, paint);
        }
        else {
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[0], w_2, h_2), 0, 0, paint);
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[0], w_2, h_2), 0, y_2, paint);
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[1], w_2, h_2), x_2, 0, paint);
            canvas.drawBitmap(ThumbnailUtils.extractThumbnail(bitmaps[2], w_2, h_2), x_2, y_2, paint);
        }

        return finalImage;
    }

    public static Bitmap getMixImagesBitmap(int width, int height, List<Bitmap> bitmaps) {
        return getMixImagesBitmap(width, height, bitmaps.toArray(new Bitmap[0]));
    }

    public static Bitmap scaleImage(Bitmap bitmap, int boxSize) {
        if (boxSize == 0)
            return null;

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) boxSize) / bitmap.getWidth();
        float yScale = ((float) boxSize) / bitmap.getHeight();
        float scale = (xScale <= yScale) ? xScale : yScale;

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static void scanFilePathForGallery(Context context, String path) {
        if (context == null)
            return;
        
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }


    public static byte[] getImageByteArray(Bitmap bitmap){
        return getImageByteArray(bitmap, 50);
    }

    public static byte[] getImageByteArray(Bitmap bitmap, int quality) {
        // Converting file to a JPEG and then to byte array.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    public static Uri uriForResourceId(Context context, @DrawableRes int resourceId) {
        Resources resources = context.getResources();
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resourceId))
                .appendPath(resources.getResourceTypeName(resourceId))
                .appendPath(resources.getResourceEntryName(resourceId))
                .build();
    }

}

