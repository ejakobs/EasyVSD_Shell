package org.invotek.apps.easyvsd_shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.io.IOException;

/**
 * Created by ejako on 9/20/2017.
 */

public class ImageDecoder {

    int width, height;
    String photoFilename;
    int photoId;
    public ImageDecoder(){}

    public Bitmap getImage(Context c, int width, int height, boolean thumbnail, String imageFilename){
        //SharedPreferences prefs = c.getSharedPreferences("PlayTalkPrefs", Context.MODE_PRIVATE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        int PHOTO_QUALITY = Integer.parseInt(prefs.getString("photo_quality", "2"));
        this.width = width;
        this.height = height;
        photoFilename = imageFilename;
        return getDimensions(c, TextUtils.equals(photoFilename, "default") ? decodeBitmapFromResource(c.getResources(),
                R.drawable.lost_page, width, height) : decodeBitmapFromFile(c.getResources(),
                (int)Math.round(width / PHOTO_QUALITY), (int)Math.round(height / PHOTO_QUALITY)), thumbnail);
    }

    public Bitmap getImage(Context c, int width, int height, boolean thumbnail, int resId){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        int PHOTO_QUALITY = Integer.parseInt(prefs.getString("photo_quality", "2"));
        this.width = width;
        this.height = height;
        photoId = resId;
        return getDimensions(c, TextUtils.equals(photoFilename, "default") ? decodeBitmapFromResource(c.getResources(),
                R.drawable.lost_page, width, height) : decodeBitmapFromResource(c.getResources(), photoId,
                (int)Math.round(width / PHOTO_QUALITY), (int)Math.round(height / PHOTO_QUALITY)), thumbnail);
    }

    private Bitmap getDimensions(Context c, Bitmap image, boolean thumbnail){
        if(image == null){
            image = decodeBitmapFromResource(c.getResources(), R.drawable.lost_page, width, height);
        }
        if(!thumbnail){
            width = image.getWidth();
            height = image.getHeight();
        }
        return image;
    }

    private Bitmap decodeBitmapFromFile(Resources res, int reqWidth, int reqHeight){
        Bitmap ret = null;
        Bitmap tmp = null;
        try
        {
            System.gc();
            //fixPhotoFilename();
            //Log.i("PlayTalk", "ActivityPage.decodeBitmapFromFile enter:"+
            //	" reqSize=[" + Integer.toString(reqWidth) + "," + Integer.toString(reqHeight) + "]" +
            //	", filename=[" + photoFilename + "]");
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            tmp = BitmapFactory.decodeFile(photoFilename, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            ret =  BitmapFactory.decodeFile(photoFilename, options);
            int rotation = getRotationForImage(photoFilename);
            if(rotation != 0){
                Matrix m = new Matrix();
                m.postRotate(rotation);
                ret = Bitmap.createBitmap(ret, 0,0,ret.getWidth(), ret.getHeight(), m, true);
            }
        }catch(Exception e){
            ret = decodeBitmapFromResource(res, R.drawable.lost_page, reqWidth, reqHeight);
        }finally{
            if(tmp != null){
                tmp.recycle();
                tmp = null;
            }
        }

        if(ret == null){
            ret = decodeBitmapFromResource(res, R.drawable.lost_page, reqWidth, reqHeight);
        }

        //Log.i("PlayTalk", "ActivityPage.decodeBitmapFromFile exit:" +
        //	" size=[" + Integer.toString(ret.getWidth()) +
        //	"," + Integer.toString(ret.getHeight()) + "]");
        if( ret.getHeight()<=1 || ret.getWidth()<=1 ) {
            //tmp.getHeight();
        }
        return ret;
    }

    private static int getRotationForImage(String path){
        int rotation = 0;
        try{
            ExifInterface exif = new ExifInterface(path);
            switch(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)){
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                default:
                    break;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        //Log.i("PlayTalk", "ActivityPage.getRotationForImage exit" +
        //	", rotation=" + Integer.toString(rotation) );
        return rotation;
    }

    private Bitmap decodeBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight){
        Bitmap ret = null;
        Bitmap tmp = null;
        //Log.i("PlayTalk", "ActivityPage.decodeBitmapFromResource entry:"+
        //	" reqSize=[" + Integer.toString(reqWidth) + "," + Integer.toString(reqHeight) + "]" +
        //	", resID=" + Integer.toString(resId));
        try{
            System.gc();
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            tmp = BitmapFactory.decodeResource(res, resId, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            ret = BitmapFactory.decodeResource(res, resId, options);
        }catch(Exception e){
            ret = null;
        }finally{
            if(tmp != null){
                tmp.recycle();
                tmp = null;
            }
        }
        //Log.i("PlayTalk", "ActivityPage.decodeBitmapFromResource exit:" +
        //	" size=[" + Integer.toString(ret.getWidth()) +
        //	"," + Integer.toString(ret.getHeight()) + "]");
        return ret;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if(height > width){
            if((width > reqHeight) || (height > reqWidth))
            {
                final int heightRatio = Math.round((float)width/(float)reqHeight);
                final int widthRatio = Math.round((float)height/(float)reqWidth);
                inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }
        }else{
            if((height > reqHeight) || (width > reqWidth)){
                final int heightRatio = Math.round((float)height/(float)reqHeight);
                final int widthRatio = Math.round((float)width/(float)reqWidth);
                inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
            }
        }
        //Log.i("PlayTalk", "ActivityPage.calculateInSampleSize exit" +
        //	", requestSize=[" + Integer.toString(reqWidth) + "," + Integer.toString(reqHeight) + "]" +
        //	", options.outSize=[" + Integer.toString(options.outWidth) + "," + Integer.toString(options.outHeight) + "]" +
        //	", inSampleSize=" + Integer.toString(inSampleSize) );
        return inSampleSize;
    }
}
