package com.google.android.systemui.smartspace;

import android.content.Context;
import android.util.Log;
import com.google.protobuf.nano.MessageNano;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class ProtoStore {
    private final Context mContext;

    public ProtoStore(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public void store(MessageNano proto, String file) {
        FileOutputStream fos;
        try {
            fos = this.mContext.openFileOutput(file, 0);
            if (proto != null) {
                fos.write(MessageNano.toByteArray(proto));
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("deleting ");
                stringBuilder.append(file);
                Log.d("ProtoStore", stringBuilder.toString());
                this.mContext.deleteFile(file);
            }
            if (fos != null) {
                closeResource(null, fos);
            }
        } catch (FileNotFoundException e) {
            Log.d("ProtoStore", "file does not exist");
        } catch (Exception exc) {
            Log.e("ProtoStore", "unable to write file", exc);
        }
    }

    private static /* synthetic */ void closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
    }

    public <T extends MessageNano> boolean load(String filename, T protoOut) {
        File file = this.mContext.getFileStreamPath(filename);
        FileInputStream is;
        try {
            is = new FileInputStream(file);
            byte[] bytes = new byte[((int) file.length())];
            is.read(bytes, 0, bytes.length);
            MessageNano.mergeFrom(protoOut, bytes);
            closeResource(null, is);
            return true;
        } catch (FileNotFoundException e) {
            Log.d("ProtoStore", "no cached data");
            return false;
        } catch (Exception exc) {
            Log.e("ProtoStore", "unable to load data", exc);
            return false;
        }
    }
}
