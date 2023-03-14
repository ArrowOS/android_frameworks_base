/*
 * Copyright (C) 2023 LibreMobileOS Foundation
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

package com.android.server.libremobileos;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;

import android.content.Context;

import com.android.server.ServiceThread;
import com.android.server.SystemService;

import com.libremobileos.faceunlock.server.FaceUnlockServer;

public class FaceUnlockService extends SystemService {
	private final String TAG = "FaceUnlockService";
	private final Context mContext;
	private FaceUnlockServer mServer;

	public FaceUnlockService(Context context) {
		super(context);

		mContext = context;
	}

	@Override
	public void onStart() {
		ServiceThread st = new ServiceThread(TAG, THREAD_PRIORITY_DEFAULT, false);
		st.start();
		mServer = new FaceUnlockServer(mContext, st.getLooper(), this::publishBinderService);
	}
}
