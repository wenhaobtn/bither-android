/* * Copyright 2014 http://Bither.net * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * *    http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */package net.bither.http;import java.io.BufferedReader;import java.io.InputStreamReader;import net.bither.factory.CookieFactory;import net.bither.http.HttpSetting.HttpType;import net.bither.preference.PersistentCookieStore;import org.apache.http.HttpEntity;import org.apache.http.HttpResponse;import org.apache.http.conn.ClientConnectionManager;import org.apache.http.impl.client.DefaultHttpClient;import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;import org.apache.http.params.HttpConnectionParams;import org.apache.http.params.HttpParams;import android.content.Context;public abstract class BaseHttpResponse<T> {	private HttpType mHttpType = HttpType.BitherApi;	protected T result;	private Context mContext;	private String mUrl;	private DefaultHttpClient mHttpClient;	public T getResult() {		return result;	}	public abstract void setResult(String response) throws Exception;	public Context getContext() {		return mContext;	}	public void setContext(Context context) {		this.mContext = context;	}	public String getUrl() {		return mUrl;	}	public void setUrl(String url) {		this.mUrl = url;	}	public DefaultHttpClient getHttpClient() {		return mHttpClient;	}	public void setHttpClient() {		this.mHttpClient = getThreadSafeHttpClient();	}	public HttpType getHttpType() {		return mHttpType;	}	public void setHttpType(HttpType mHttpType) {		this.mHttpType = mHttpType;	}	protected String getReponse(HttpResponse httpResponse) throws Exception {		HttpEntity httpEntity = httpResponse.getEntity();		String response = getResponseFromEntity(httpEntity);		int code = httpResponse.getStatusLine().getStatusCode();		String error = code + ":" + response;		switch (code) {		case 200:			break;		case 400:			throw new Http400Exception(error);		case 403:			if (!CookieFactory.isRunning()					&& getHttpType() == HttpType.BitherApi) {				PersistentCookieStore.getInstance().clear();			}			throw new HttpAuthException(error);		case 404:			throw new Http404Exception(error);		case 500:			throw new Http500Exception(error);		default:			throw new HttpException(error);		}		return response;	}	private String getResponseFromEntity(HttpEntity entity) throws Exception {		StringBuffer buffer = new StringBuffer();		if (entity != null) {			BufferedReader reader = new BufferedReader(new InputStreamReader(					entity.getContent(), "utf-8"), 8192);			String line = null;			while ((line = reader.readLine()) != null) {				buffer.append(line);			}			reader.close();		}		return buffer.toString();	}	private DefaultHttpClient getThreadSafeHttpClient() {		if (getHttpType() == HttpType.BitherApi) {			PersistentCookieStore persistentCookieStore = PersistentCookieStore					.getInstance();			if (persistentCookieStore.getCookies() == null					|| persistentCookieStore.getCookies().size() == 0) {				CookieFactory.initCookie();			}		}		DefaultHttpClient httpClient = new DefaultHttpClient();		ClientConnectionManager mgr = httpClient.getConnectionManager();		HttpParams params = httpClient.getParams();		HttpConnectionParams.setConnectionTimeout(params,				HttpSetting.HTTP_CONNECTION_TIMEOUT);		HttpConnectionParams.setSoTimeout(params, HttpSetting.HTTP_SO_TIMEOUT);		httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(				params, mgr.getSchemeRegistry()), params);		if (getHttpType() != HttpType.OtherApi) {			httpClient.setCookieStore(PersistentCookieStore.getInstance());		}		return httpClient;	}}