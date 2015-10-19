package de.unisaarland.UniApp.restaurant.uihelper;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import de.unisaarland.UniApp.R;
import de.unisaarland.UniApp.restaurant.model.MensaItem;


public class RestaurantAdapter extends BaseAdapter {
    private final Context context;
    private final List<MensaItem> mensaItems;

    public RestaurantAdapter(Context context, List<MensaItem> mensaItems) {
        this.context = context;
        this.mensaItems = mensaItems;
    }

    @Override
    public int getCount() {
        return mensaItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mensaItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.mensa_item, null);
        }
        MensaItem model = mensaItems.get(position);

        TextView mealCategory = (TextView) convertView.findViewById(R.id.mensa_menu_category);
        mealCategory.setText(model.getCategory());

        TextView mealTitle = (TextView) convertView.findViewById(R.id.mensa_menu_title);
        mealTitle.setText(model.getTitle());

        TextView mealDescription = (TextView) convertView.findViewById(R.id.mensa_menu_description);
        mealDescription.setText(model.getDesc());

        ImageView info = (ImageView) convertView.findViewById(R.id.info);
        String labels = model.getKennzeichnungen();
        if (labels != null && !labels.equals("")) {
            info.setOnClickListener(new LabelsClickListener(labels, context));
        } else {
            info.setVisibility(View.GONE);
        }

        RelativeLayout descriptionBackgroundColor = (RelativeLayout) convertView.findViewById(R.id.contentBackground);
        descriptionBackgroundColor.setBackgroundColor(model.getColor());

        TextView mealPrice = (TextView) convertView.findViewById(R.id.mensa_menu_price);
        if (model.getPreis1() != 0) {
            String text = String.format(context.getString(R.string.mensaPriceFormat),
                    .01 * model.getPreis1(), .01 * model.getPreis2(), .01 * model.getPreis3());
            mealPrice.setText(text);
            mealPrice.setVisibility(View.VISIBLE);
        }
        return convertView;
    }


    private static class LabelsClickListener implements View.OnClickListener {

        private String ingredis;
        private Context context;

        public LabelsClickListener(String ingredis, Context context) {
            this.ingredis = ingredis;
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.label_list, null);
            ListView list = (ListView) view.findViewById(R.id.label_list);
            String[] labels = ingredis.split(",");
            list.setAdapter(new LabelAdapter(context, labels));
            Dialog dialog = new Dialog(context, R.style.Transparent);
            dialog.requestWindowFeature(Window.FEATURE_ACTION_BAR);
            dialog.setContentView(view);
            dialog.setTitle(view.getResources().getString(R.string.labels));
            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = (int) Math.round(size.x * 0.9);
            int height = (int) Math.round(size.y * 0.9);
            dialog.getWindow().setLayout(width, height);
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.7f;
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.show();
        }
    }

    private static class LabelAdapter extends BaseAdapter {

        private final Context context;
        private final String[] labels;


        public LabelAdapter(Context context, String[] labels) {
            this.context = context;
            this.labels = labels;
        }

        @Override
        public int getCount() {
            return labels.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        // sets the view of news item row
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.label_item, null);
            }
            TextView tw_label = (TextView) convertView.findViewById(R.id.label);
            String stringname;
            try {
                stringname = "label_" + labels[position];
            } catch (Exception e) {
                stringname = "Keine Angabe";
            }
            tw_label.setText(context.getResources().getIdentifier(stringname, "string", context.getPackageName()));
            return convertView;
        }
    }
}