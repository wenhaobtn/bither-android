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

package net.bither.api;

import java.util.ArrayList;
import java.util.List;

import net.bither.http.BitherUrl;
import net.bither.http.HttpGetResponse;
import net.bither.util.BlockUtil;
import net.bither.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.bitcoin.core.StoredBlock;

public class DownloadSpvApi extends HttpGetResponse<StoredBlock> {

    public DownloadSpvApi() {
        setUrl(BitherUrl.BITHER_GET_ONE_SPVBLOCK_API);
    }

    @Override
    public void setResult(String response) throws Exception {
        LogUtil.d("http", response);
        List<StoredBlock> storedBlocks = new ArrayList<StoredBlock>();
        JSONObject jsonObject = new JSONObject(response);
        this.result = BlockUtil.formatStoredBlock(jsonObject);

    }

}
