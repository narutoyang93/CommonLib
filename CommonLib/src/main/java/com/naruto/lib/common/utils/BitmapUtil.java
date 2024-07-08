package com.naruto.lib.common.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.naruto.lib.common.Global;
import com.naruto.lib.common.InterfaceFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Purpose 处理图片的工具类
 * @Author Naruto Yang
 * @CreateDate 2019/7/11 0011
 * @Note
 */
public class BitmapUtil {
    /**
     * 压缩图片（尺寸压缩&&质量压缩）
     *
     * @param maxWidth        图片最大宽度（单位：px）
     * @param maxHeight       图片最大高度
     * @param maxLength       图片最大占用空间（单位：b）
     * @param imgPath         图片文件路径
     * @param matchTargetSize 是否绝对匹配目标尺寸，如果true,即使原图尺寸小于目标尺寸也将其放大到目标尺寸;如果为false，原图尺寸小于目标尺寸则不缩放
     * @param callback        回调
     */
    public static void compressPicture(final int maxWidth, final int maxHeight, final int maxLength, final String imgPath, final boolean matchTargetSize, final CompressCallBack callback) {
        new Thread(() -> {
            Bitmap bitmap = getScaleBitmap(imgPath, maxWidth, maxHeight, matchTargetSize);
            compressPicture(bitmap, maxLength, callback);//质量压缩
            bitmap.recycle();
        }).start();
    }

    public static void compressPicture(final int maxWidth, final int maxHeight, final int maxLength, final String imgPath, final CompressCallBack callback) {
        compressPicture(maxWidth, maxHeight, maxLength, imgPath, false, callback);
    }

    /**
     * 质量压缩
     *
     * @param bitmap
     * @param maxLength 图片最大占用空间（单位：b）
     * @param format    压缩格式
     * @param callback  回调
     */
    public static void compressPicture(Bitmap bitmap, int maxLength, Bitmap.CompressFormat format, CompressCallBack callback) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int quality = 100;//质量百分比
        int newSize = 0;
        // 质量压缩
        do {
            stream.reset();
            bitmap.compress(format, quality, stream);
            newSize = stream.toByteArray().length;
        } while (newSize > maxLength && (quality -= 5) > 0);
        callback.onCompressFinish(stream);
    }

    public static void compressPicture(Bitmap bitmap, int maxLength, CompressCallBack callback) {
        compressPicture(bitmap, maxLength, Bitmap.CompressFormat.WEBP, callback);
    }


    /**
     * 获取缩放后的bitmap
     *
     * @param imgPath
     * @param targetWidth
     * @param targetHeight
     * @param matchTargetSize 是否绝对匹配目标尺寸，如果true,即使原图尺寸小于目标尺寸也将其放大到目标尺寸;如果为false，原图尺寸小于目标尺寸则不缩放
     * @return
     */
    public static Bitmap getScaleBitmap(final String imgPath, final int targetWidth, final int targetHeight, boolean matchTargetSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//只获取图片的大小信息，而不是将整张图片载入在内存中，避免内存溢出
        BitmapFactory.decodeFile(imgPath, options);
        final int realWidth = options.outWidth;
        final int realHeight = options.outHeight;
        int width = realWidth;
        int height = realHeight;
        Bitmap bitmap;
        float scale;
        if (width > targetWidth || height > targetHeight) {//尺寸压缩
            //如果图片过大，先加载比目标尺寸稍大一点的缩略图（优化内存）
            width /= 2;
            height /= 2;
            int inSampleSize = 1;
            while (width > targetWidth || height > targetHeight) {
                inSampleSize *= 2;
                width /= 2;
                height /= 2;
            }
            options.inSampleSize = inSampleSize;
            //尺寸压缩
            scale = Math.max(realWidth / (float) targetWidth, realHeight / (float) targetHeight);//压缩比
        } else if (matchTargetSize && width < targetWidth && height < targetHeight) {//放大
            //尺寸放大
            scale = Math.max(realWidth / (float) targetWidth, realHeight / (float) targetHeight);//压缩比
        } else {
            scale = 1;
        }
        options.inJustDecodeBounds = false;
        Bitmap src = BitmapFactory.decodeFile(imgPath, options);

        if (scale == 1) {//不用缩放
            bitmap = src;
        } else {//执行缩放
            bitmap = Bitmap.createScaledBitmap(src, (int) (realWidth / scale), (int) (realHeight / scale), false);
            if (src != bitmap) {
                src.recycle();
            }
        }

        int rotateDegree;//旋转角度
        if ((rotateDegree = getPictureRotateDegree(imgPath)) != 0) {//旋转图片
            Bitmap bitmap0 = rotateBitmap(rotateDegree, bitmap);
            if (bitmap0 != bitmap) {
                bitmap.recycle();
            }
            bitmap = bitmap0;
        }
        return bitmap;
    }


    /**
     * 得到bitmap的大小
     *
     * @param bitmap
     * @return
     */
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//API 19 以上
            return bitmap.getAllocationByteCount();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {//API 12 以上
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }


    /**
     * 利用view创建Bitmap
     *
     * @param view
     * @return
     */
    public static Bitmap createBitmapByView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * Bitmap保存为图片
     *
     * @param bitmap
     * @param savePath
     * @return
     */
    public static File saveBitmapToFile(Bitmap bitmap, String savePath) {
        File file = new File(savePath);
        if (file.exists()) {
            file.delete();
        } else if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    /**
     * 将Bitmap保存成图片
     *
     * @param bitmap
     * @param saveUri
     * @return
     */

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static Uri saveBitmapToFile(Bitmap bitmap, Uri saveUri) {
        if (bitmap == null || saveUri == null) return null;
        if (FileUtil.INSTANCE.exists(saveUri)) FileUtil.INSTANCE.delete(saveUri);
        ContentResolver resolver = Global.getMainModuleContext.invoke().getContentResolver();
        try (BufferedOutputStream bos = new BufferedOutputStream(resolver.openOutputStream(saveUri))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return saveUri;
    }


    /**
     * 旋转图片
     *
     * @param angle
     * @param bitmap
     * @return
     */
    public static Bitmap rotateBitmap(int angle, Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 获取图片的旋转角度
     *
     * @param path
     * @return
     */
    public static int getPictureRotateDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }


    /**
     * 将输入流变为byte数组
     *
     * @param inputStreamCreator 输入流
     * @return
     */
    public static byte[] readStream(InterfaceFactory.Func0<InputStream> inputStreamCreator) {
        byte[] buffer = new byte[1024];
        int len;
        byte[] data;
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(); InputStream inStream = inputStreamCreator.execute()) {
            while ((len = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            data = outStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
        return data;
    }


    /**
     * 字节数组转bitmap
     *
     * @param bytes 数据
     * @param opts  BitmapFactory.Options
     * @return
     */
    public static Bitmap getPicFromBytes(byte[] bytes, BitmapFactory.Options opts) {
        if (bytes == null) {
            return null;
        }
        if (opts != null) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length,
                    opts);
        } else {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

    /**
     * 图片缩放
     *
     * @param bitmap 图片对象
     * @param w      要缩放的宽度
     * @param h      要缩放的高度
     * @return newBmp 新 Bitmap对象
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    /**
     * bitmap转字节数组
     *
     * @param bm 图片
     * @return
     */
    public static byte[] bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toByteArray();
    }


    /**
     * @Purpose 图片压缩回调
     * @Author Naruto Yang
     * @CreateDate 2020/2/27
     * @Note
     */
    public interface CompressCallBack {
        void onCompressFinish(ByteArrayOutputStream stream);
    }

}
