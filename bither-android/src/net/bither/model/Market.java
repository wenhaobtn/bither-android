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

package net.bither.model;

import net.bither.BitherApplication;
import net.bither.BitherSetting;
import net.bither.BitherSetting.MarketType;
import net.bither.R;

public class Market {

    private Ticker mTicker;
    private MarketType marketType;
    private boolean showDetail;

    public Market(MarketType marketType) {
        this.marketType = marketType;
    }

    public static String getMarketState(MarketType marketType) {
        String str = "";
        switch (marketType) {
            case HUOBI:
                str = "huobi";
                break;
            case BITSTAMP:
                str = "bitstamp";
                break;
            default:
                break;
        }
        return str;

    }

    public boolean isShowDetail() {

        return showDetail;
    }

    public void setShowDetail(boolean showDetail) {
        this.showDetail = showDetail;
    }

    public String getName() {
        return BitherSetting.getMarketName(getMarketType());
    }

    public MarketType getMarketType() {
        return marketType;
    }

    public int getMarketColor() {
        int resource = -1;
        switch (getMarketType()) {
            case HUOBI:
                resource = R.color.market_color_huobi;
                break;
            case BITSTAMP:
                resource = R.color.market_color_bitstamp;
                break;
            case BTCE:
                resource = R.color.market_color_btce;
                break;
            case OKCOIN:
                resource = R.color.market_color_okcoin;
                break;
            case CHBTC:
                resource = R.color.market_color_chbtc;
                break;
            case BTCCHINA:
                resource = R.color.market_color_btcchina;
                break;
            default:
                resource = R.color.text_field_text_color;
                break;
        }
        return BitherApplication.mContext.getResources().getColor(resource);
    }

    public void setPriceAlert(double low, double high) {
        if (low <= 0 && high <= 0) {
            PriceAlert.removePriceAlert(getPriceAlert());
        } else {
            PriceAlert alert = getPriceAlert();
            if (alert != null && alert.getExchangeHigher() == high && alert.getExchangeLower() ==
                    low) {
                return;
            }
            PriceAlert.addPriceAlert(new PriceAlert(getMarketType(), low, high));
        }
    }

    public PriceAlert getPriceAlert() {
        return PriceAlert.getPriceAlert(getMarketType());
    }

    public String getUrl() {
        return "http://" + getDomainName();
    }

    public String getDomainName() {
        switch (getMarketType()) {
            case HUOBI:
                return "huobi.com";
            case BITSTAMP:
                return "bitstamp.net";
            case BTCE:
                return "btc-e.com";
            case OKCOIN:
                return "okcoin.cn";
            case CHBTC:
                return "chbtc.com";
            case BTCCHINA:
                return "btcchina.com";
            default:
                return null;
        }
    }

    public Ticker getTicker() {
        return mTicker;
    }

    public void setTicker(Ticker mTicker) {
        this.mTicker = mTicker;
    }

}
