/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.runnable;

import android.media.Image;

import net.bither.api.UploadAvatarApi;
import net.bither.preference.AppSharedPreference;
import net.bither.util.FileUtil;
import net.bither.util.ImageFileUtil;
import net.bither.util.StringUtil;

import java.io.File;


public class UploadAvatarRunnable extends BaseRunnable {
    @Override
    public void run() {
        obtainMessage(HandlerMessage.MSG_PREPARE);
        try {
            String avatar = AppSharedPreference.getInstance().getUserAvatar();
            if (!StringUtil.isEmpty(avatar)) {
                File file = ImageFileUtil.getUploadAvatarFile(avatar);
                if (file.exists()) {
                    UploadAvatarApi uploadAvatarApi = new UploadAvatarApi(file);
                    uploadAvatarApi.handleHttpPost();
                    file.delete();
                }
            }
            obtainMessage(HandlerMessage.MSG_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            obtainMessage(HandlerMessage.MSG_FAILURE);
        }

    }
}
