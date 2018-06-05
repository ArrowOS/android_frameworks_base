/*
 * Copyright (C) 2018 Potato Open Sauce Project
 * Copyright (C) 2021 WaveOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * Helper functions for uploading to KatBin (https://katb.in).
 */
public final class KatBinUtils {

    private static Handler mHandler;

    /**
     * Uploads {@code content} to KatBin
     *
     * @param content  the content to upload to KatBin
     * @param callback the callback to call on success / failure
     */
    public static void upload(String content, UploadResultCallback callback) {
        getHandler().post(() -> {
            try {
                URL url = new URL("https://api.katb.in/api/paste");
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept-Charset", "UTF-8");
                urlConnection.setDoOutput(true);

                StringWriter stringWriter = new StringWriter();
                JsonWriter jsonWriter = new JsonWriter(stringWriter);
                jsonWriter.beginObject().name("content").value(content).endObject();

                try (OutputStream output = urlConnection.getOutputStream()) {
                    output.write(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
                }

                String id = "";
                try (JsonReader reader = new JsonReader(new InputStreamReader(urlConnection.getInputStream(),
                        StandardCharsets.UTF_8))) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        if (reader.nextName().equals("paste_id")) {
                            id = reader.nextString();
                            break;
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                }

                if (!id.isEmpty()) {
                    callback.onSuccess(String.format("https://katb.in/%s", id));
                } else {
                    String msg = "Failed to upload to KatBin: No id retrieved";
                    callback.onFail(msg, new Exception(msg));
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                String msg = "Failed to upload to KatBin";
                callback.onFail(msg, e);
            }
        });
    }

    private static Handler getHandler() {
        if (mHandler == null) {
            HandlerThread katBinThread = new HandlerThread("KatBinThread");
            if (!katBinThread.isAlive()) {
                katBinThread.start();
            }
            mHandler = new Handler(katBinThread.getLooper());
        }
        return mHandler;
    }

    public interface UploadResultCallback {
        void onSuccess(String url);

        void onFail(String message, Exception e);
    }
}
