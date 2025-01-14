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

package net.bither.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;

public class CurrencySymbolUtil {
	private static final int MinBlackValue = 0;

	private static Bitmap bmpBtc;
	private static Bitmap bmpBtcSlim;

	public static Bitmap getBtcSymbol(TextView tv) {
		return getBtcSymbol(adjustTextColor(tv.getTextColors()
				.getDefaultColor()), tv.getTextSize());
	}

	public static Bitmap getBtcSymbol(int color) {
		return getBtcSymbol(color, 0);
	}

	public static Bitmap getBtcSymbol(int color, float textSize) {
		if (bmpBtc == null) {
			bmpBtc = BitmapFactory.decodeResource(
					BitherApplication.mContext.getResources(),
					R.drawable.symbol_btc);
		}
		Bitmap bmp = scaleToTextSize(bmpBtc, textSize);
		if (color == Color.WHITE) {
			return bmp;
		}
		return changeBitmapColor(bmp, color);
	}

	public static Bitmap getBtcSlimSymbol(TextView tv) {
		return getBtcSlimSymbol(adjustTextColor(tv.getTextColors()
				.getDefaultColor()), tv.getTextSize());
	}

	public static Bitmap getBtcSlimSymbol(int color) {
		return getBtcSlimSymbol(color, 0);
	}

	public static Bitmap getBtcSlimSymbol(int color, float textSize) {
		if (bmpBtcSlim == null) {
			bmpBtcSlim = BitmapFactory.decodeResource(
					BitherApplication.mContext.getResources(),
					R.drawable.symbol_btc_slim);
		}
		Bitmap bmp = scaleToTextSize(bmpBtcSlim, textSize);
		if (color == Color.WHITE) {
			return bmp;
		}
		return changeBitmapColor(bmp, color);
	}

	private static Bitmap scaleToTextSize(Bitmap bmp, float textSize) {
		int bmpHeight = bmp.getHeight();
		if (textSize > 0 && textSize != bmp.getHeight()) {
			float scale = textSize / (float) bmpHeight;
			bmp = Bitmap.createScaledBitmap(bmp,
					(int) (bmp.getWidth() * scale), (int) (bmpHeight * scale),
					true);
		}
		return bmp;
	}

	public static Bitmap changeBitmapColor(Bitmap bmp, int color) {
		Bitmap result = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
				bmp.getConfig());
		Canvas c = new Canvas(result);
		Paint paint = new Paint();
		ColorFilter filter = new LightingColorFilter(color, 1);
		paint.setColorFilter(filter);
		c.drawBitmap(bmp, 0, 0, paint);
		return result;
	}

	private static int adjustTextColor(int color) {
		if (Color.red(color) == Color.green(color)
				&& Color.green(color) == Color.blue(color)) {
			int value = Color.red(color);
			if (value < MinBlackValue) {
				value = MinBlackValue;
				return Color.argb(Color.alpha(color), value, value, value);
			}
		}
		return color;
	}

	public static void test(Context context) {
		Dialog d = new Dialog(context);
		TextView tv = new TextView(context);
		ImageView iv = new ImageView(context);
		tv.setTextSize(20);
		tv.setText("100.00");
		iv.setImageBitmap(getBtcSymbol(Color.RED, tv.getTextSize()));
		LinearLayout ll = new LinearLayout(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.addView(iv);
		ll.addView(tv);
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv
				.getLayoutParams();
		lp.gravity = Gravity.CENTER_VERTICAL;
		lp = (LinearLayout.LayoutParams) tv.getLayoutParams();
		lp.gravity = Gravity.CENTER_VERTICAL;
		d.setContentView(ll, new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		d.show();
	}
}
