package net.snapdrop;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;

public class JavaScriptInterface {
    private final Context context;

    public JavaScriptInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void getBase64FromBlobData(String base64Data, String mimeType, String extension) throws IOException {
        convertBase64StringToFileAndSaveIt(base64Data, mimeType, extension);
    }

    public static String getBase64StringFromBlobUrl(String blobUrl, String mimeType, String extension) {
        if (blobUrl.startsWith("blob")) {
            return "javascript: var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '" + blobUrl + "', true);" +
                    "xhr.setRequestHeader('Content-type','" + mimeType + "');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobPdf = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobPdf);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            mime = '" + mimeType + "';" +
                    "            ext = '" + extension + "';" +
                    "            Android.getBase64FromBlobData(base64data,mime,ext);" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }

    private void convertBase64StringToFileAndSaveIt(String base64Data, String mimeType, String extension) throws IOException {
        String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
        String fileName = "Snapdrop_" + currentDateTime;
        if (!extension.isEmpty()) fileName = fileName + "_." + extension;
        byte[] fileAsBytes = Base64.decode(base64Data.replaceFirst("^data:" + mimeType + ";base64,", ""), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, mimeType);
            contentValues.put(MediaStore.Downloads.DATE_ADDED, System.currentTimeMillis());
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Snapdrop");

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            OutputStream outputStream = resolver.openOutputStream(uri);
            outputStream.write(fileAsBytes);
            outputStream.close();

        } else {
            final File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/" + fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.write(fileAsBytes);
            fileOutputStream.flush();
        }
        Toast.makeText(context, "Download complete!\nCheck downloads folder.", Toast.LENGTH_SHORT).show();
    }
}