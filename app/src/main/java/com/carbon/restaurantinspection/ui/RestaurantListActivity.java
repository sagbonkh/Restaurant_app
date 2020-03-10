package com.carbon.restaurantinspection.ui;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.carbon.restaurantinspection.R;
import com.carbon.restaurantinspection.model.InspectionDetail;
import com.carbon.restaurantinspection.model.InspectionManager;
import com.carbon.restaurantinspection.model.Restaurant;
import com.carbon.restaurantinspection.model.RestaurantManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantListActivity extends AppCompatActivity {

    ArrayList<Integer> numIssues = new ArrayList<Integer>();

    // calls RestaurantManager class
    private RestaurantManager restaurantManager;
    private InspectionManager inspectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restaurant_list_activity);

        // Get that one instance that RestaurantManager class produced
        restaurantManager = RestaurantManager.getInstance(this);
        inspectionManager = InspectionManager.getInstance(this);
        if (inspectionManager == null){
            Log.e("Hello : ", "it is null");
        }
        if (inspectionManager.getInspections("SDFO-8HKP7E") == null){
            Log.e("Does not load : ", "SDFO-8HKP7E not loaded");
        }
        populateRestaurantListView();
    }

    private void populateRestaurantListView() {

        // Build adapter
        ArrayAdapter<Restaurant> adapter = new MyListAdapter();

        //configure list view
        ListView list = (ListView) findViewById(R.id.restaurant_list_view);
        list.setAdapter(adapter);

    }

    private class MyListAdapter extends ArrayAdapter<Restaurant> {
        public MyListAdapter(){
            super(RestaurantListActivity.this, R.layout.display_restaurant_list_activity, restaurantManager.getRestaurantList());
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View itemView = convertView;
            if(itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.display_restaurant_list_activity, parent,false);
            }

            // find the restaurant
            String currentRestaurants = restaurantManager.getRestaurant(position).getName().replace("\"", "");

            //fill in the view
            ImageView imageView = (ImageView) itemView.findViewById(R.id.item_restaurant_icon);
            if (currentRestaurants.contains("A&W")){
                imageView.setImageResource(R.drawable.a_w_restaurant_icon);
            } else
                imageView.setImageResource(R.drawable.beer_icon);

           String restaurantTrackingNum = restaurantManager.getRestaurant(position).getTrackingNumber();
           String sub = restaurantTrackingNum.replace("\"", "");

           ArrayList<InspectionDetail> inspections = inspectionManager.getInspections(sub);
//            Collections.sort(inspections);

            // set # issues
            if(inspections != null) {
                int numCrit = inspections.get(0).getNumCritical();
                int numNonCrit = inspections.get(0).getNumNonCritical();
                int totalIssues = numCrit + numNonCrit;

                TextView numIssuesText = (TextView) itemView.findViewById(R.id.num_issues_textview);
                numIssuesText.setText("# Issues: " + totalIssues);
            }

            else {
                TextView numIssuesText = (TextView) itemView.findViewById(R.id.num_issues_textview);
                numIssuesText.setText("" + 0);
            }

            // set restaurant name
           TextView restaurantNameText = (TextView) itemView.findViewById(R.id.restaurant_name_textview);
           restaurantNameText.setText(currentRestaurants);

            // clickable listview
            ListView list = findViewById(R.id.restaurant_list_view);
            list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id){

                    Toast.makeText(RestaurantListActivity.this, "Position: " + position, Toast.LENGTH_LONG).show();
                }
            });
            return itemView;
        }
    }
}
