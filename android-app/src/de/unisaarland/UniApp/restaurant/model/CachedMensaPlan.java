package de.unisaarland.UniApp.restaurant.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;
import java.util.Map;

import de.unisaarland.UniApp.R;
import de.unisaarland.UniApp.utils.ContentCache;
import de.unisaarland.UniApp.utils.NetworkRetrieveAndCache;
import de.unisaarland.UniApp.utils.Util;

public class CachedMensaPlan {

    private static final String MENSA_URL_SB = "http://studentenwerk-saarland.de/_menu/actual/speiseplan-saarbruecken.xml";
    private static final String MENSA_URL_HOM = "http://studentenwerk-saarland.de/_menu/actual/speiseplan-homburg.xml";

    private NetworkRetrieveAndCache<Map<Long, List<MensaItem>>> mensaFetcher = null;

    private final NetworkRetrieveAndCache.Delegate<Map<Long, List<MensaItem>>> networkDelegate;

    private final Context context;

    public CachedMensaPlan(NetworkRetrieveAndCache.Delegate<Map<Long, List<MensaItem>>> networkDelegate, Context context) {
        this.networkDelegate = networkDelegate;
        this.context = context;
    }

    private void initFetcher() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String campus = settings.getString(context.getString(R.string.pref_campus), null);
        String mensaUrl = campus.equals(context.getString(R.string.pref_campus_saar))
                ? MENSA_URL_SB : MENSA_URL_HOM;

        if (mensaFetcher == null || !mensaUrl.equals(mensaFetcher.getUrl())) {
            ContentCache cache = Util.getContentCache(context);
            mensaFetcher = new NetworkRetrieveAndCache<>(mensaUrl, "mensa-"+campus, cache,
                    new MensaXMLParser(), new NetworkDelegate(), context);
        }
    }

    /**
     * @return true if the data was initially loaded from the cache, false otherwise. in any case,
     *         reloading might have been triggered.
     */
    public boolean load(int reloadIfOlderSeconds) {
        initFetcher();
        return mensaFetcher.loadAsynchronously(reloadIfOlderSeconds);
    }

    public void cancel() {
        if (mensaFetcher != null) {
            mensaFetcher.cancel();
            mensaFetcher = null;
        }
    }

    public static boolean loadedSince(long timeMillis, Context context) {
        CachedMensaPlan plan = new CachedMensaPlan(null, context);
        plan.initFetcher();
        boolean ret = plan.loadedSince(timeMillis);
        plan.cancel();
        return ret;
    }

    public boolean loadedSince(long timeMillis) {
        initFetcher();
        return mensaFetcher.loadedSince(timeMillis);
    }

    private final class NetworkDelegate implements NetworkRetrieveAndCache.Delegate<Map<Long, List<MensaItem>>> {
        @Override
        public void onUpdate(Map<Long, List<MensaItem>> result) {
            if (networkDelegate != null)
                networkDelegate.onUpdate(result);
        }

        @Override
        public void onStartLoading() {
            if (networkDelegate != null)
                networkDelegate.onStartLoading();
        }

        @Override
        public void onFailure(String message) {
            if (networkDelegate != null)
                networkDelegate.onFailure(message);
        }

        @Override
        public String checkValidity(Map<Long, List<MensaItem>> result) {
            if (result.isEmpty())
                return context.getString(R.string.emptyDocumentError);
            return null;
        }
    }
}