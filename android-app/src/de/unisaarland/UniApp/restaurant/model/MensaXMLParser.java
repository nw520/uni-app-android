package de.unisaarland.UniApp.restaurant.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.Html;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.unisaarland.UniApp.R;
import de.unisaarland.UniApp.utils.Util;
import de.unisaarland.UniApp.utils.XMLExtractor;


public class MensaXMLParser extends XMLExtractor<MensaDayMenu[]> {

    private static final String START_TAG = "speiseplan";
    private static final String TAG = "tag";
    private static final String ITEM_TAG = "item";
    private static final String TITLE = "title";
    private static final String CATEGORY = "category";
    private static final String DESCRIPTION = "description";
    private static final String PREIS1 = "preis1";
    private static final String PREIS2 = "preis2";
    private static final String PREIS3 = "preis3";
    private static final String COLOR = "color";
    private static final String TIMESTAMP = "timestamp";

    private String languageSuffix;

    MensaXMLParser(String language) {
        switch (language) {
            case "en":
                languageSuffix = "_en";
                break;
            case "de":
            default:
                languageSuffix = "";
        }
    }

    /**
     * Returns the recommended language code of the language the mensa menu should be displayed in
     */
    static String getMensaLanguage(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean forceGerman = settings.getBoolean(context.getString(R.string.pref_mensa_force_german), false);
        if (forceGerman) {
            return "de";
        } else {
            return context.getString(R.string.mensa_language);
        }
    }

    @Override
    public MensaDayMenu[] extractFromXML(XmlPullParser parser)
            throws IOException, XmlPullParserException, ParseException {
        List<MensaDayMenu> items = new ArrayList<>(10);

        parser.require(XmlPullParser.START_DOCUMENT, null, null);
        parser.next();
        parser.require(XmlPullParser.START_TAG, null, START_TAG);

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            if (parser.getName().equals(TAG)) {
                String tempDate = parser.getAttributeValue(null, TIMESTAMP);
                long date = Long.parseLong(tempDate) * 1000;
                long dayStartMillis = Util.getStartOfDay(date).getTimeInMillis();
                MensaItem[] tagItems = readItems(parser);
                items.add(new MensaDayMenu(dayStartMillis, tagItems));
            }
        }

        MensaDayMenu[] arr = items.toArray(new MensaDayMenu[items.size()]);
        Arrays.sort(arr);
        return arr;
    }

    private MensaItem[] readItems(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, TAG);
        List<MensaItem> mensaItems = new ArrayList<>(8);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            if (parser.getName().equals(ITEM_TAG))
                mensaItems.add(parseMensaItem(parser));
            else
                skipTag(parser);
        }
        return mensaItems.toArray(new MensaItem[mensaItems.size()]);
    }

    private MensaItem parseMensaItem(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, ITEM_TAG);

        String category = null;
        String desc = null;
        String title = null;
        int preis1 = 0;
        int preis2 = 0;
        int preis3 = 0;
        int color = 0;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();

            switch (name) {
                case TITLE:
                    if (title == null) title = getElementValue(parser, TITLE);
                    break;
                case CATEGORY:
                    if (category == null) category = getElementValue(parser, CATEGORY);
                    break;
                case DESCRIPTION:
                    if (desc == null) desc = getElementValue(parser, DESCRIPTION);
                    break;
                case PREIS1:
                    preis1 = parsePreis(getElementValue(parser, PREIS1));
                    break;
                case PREIS2:
                    preis2 = parsePreis(getElementValue(parser, PREIS2));
                    break;
                case PREIS3:
                    preis3 = parsePreis(getElementValue(parser, PREIS3));
                    break;
                case COLOR:
                    color = parseColor(getElementValue(parser, COLOR));
                    break;
                default:
                    // Check, if localised tag exist and if it's not empty
                    if (name.equals(CATEGORY + languageSuffix)) {
                        String val = getElementValue(parser, CATEGORY + languageSuffix);
                        if (!val.equals("")) category = val;
                    } else if (name.equals(DESCRIPTION + languageSuffix)) {
                        String val = getElementValue(parser, DESCRIPTION + languageSuffix);
                        if (!val.equals("")) desc = val;
                    } else if (name.equals(TITLE + languageSuffix)) {
                        String val = getElementValue(parser, TITLE + languageSuffix);
                        if (!val.equals("")) title = val;
                    } else {
                        // Unknown tag, skip
                        skipTag(parser);
                    }
            }
        }

        String[] labels = extractLabels(title + desc);
        return new MensaItem(category, desc, title, labels, preis1, preis2,
                preis3, color);
    }

    /**
     * Returns the list of all labels extracted from occurrences of "(A,B,C)" where each component is
     * at most two characters long.
     */
    private String[] extractLabels(String desc) {
        Set<String> labels = new HashSet<>();
        int pos = 0;
        while (pos < desc.length()) {
            int openParen = desc.indexOf('(', pos);
            int closeParen = desc.indexOf(')', openParen + 1);
            if (openParen == -1 || closeParen == -1)
                break;
            boolean valid = true;
            String[] parts = desc.substring(openParen + 1, closeParen).split(",");
            for (String part : parts)
                if (part.trim().length() > 2)
                    valid = false;
            if (valid)
                for (String part : parts)
                    labels.add(part.trim().toLowerCase());
            pos = closeParen + 1;
        }
        String[] arr = labels.toArray(new String[labels.size()]);
        Arrays.sort(arr, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                // sort numbers before anything else, and compare numbers numerically
                boolean lhsNum = isNumerical(lhs);
                boolean rhsNum = isNumerical(rhs);
                if (lhsNum && rhsNum)
                    return Integer.valueOf(lhs).compareTo(Integer.valueOf(rhs));
                if (!lhsNum && !rhsNum)
                    return lhs.compareTo(rhs);
                if (lhsNum) // && !rhsNum
                    return -1;
                // !lhsNum && rhsNum
                return 1;
            }

            private boolean isNumerical(String s) {
                for (int i = s.length() - 1; i >= 0; --i) {
                    char c = s.charAt(i);
                    if (c < '0' || c > '9')
                        return false;
                }
                return true;
            }
        });
        return arr;
    }

    /**
     * Parses strings like "2,07" into 207.
     */
    private int parsePreis(String str) {
        int commaPos = str.indexOf(',');
        if (commaPos < 1 || commaPos + 1 >= str.length())
            return 0;
        try {
            int euro = Integer.parseInt(str.substring(0, commaPos));
            int cent = Integer.parseInt(str.substring(commaPos + 1));
            return 100 * euro + cent;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseColor(String str) {
        String[] parts = str.split(",");
        if (parts.length != 3)
            return 0;
        int red;
        int green;
        int blue;
        try {
            red = Integer.parseInt(parts[0]);
            green = Integer.parseInt(parts[1]);
            blue = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return 0;
        }
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255)
            return 0;
        if (red == 0 && green == 0 && blue == 0)
            red = green = blue = 1;
        return Color.rgb(red, green, blue);
    }

    private String getElementValue(XmlPullParser parser, String tag)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, tag);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, tag);
        return title;
    }

    private String readText(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        StringBuilder res = new StringBuilder();
        while (parser.next() == XmlPullParser.TEXT)
            res.append(parser.getText());
        return Html.fromHtml(res.toString()).toString().trim();
    }

    private void skipTag(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG)
            throw new IllegalStateException("Should be at start of a tag, is " + parser.getEventType());
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
