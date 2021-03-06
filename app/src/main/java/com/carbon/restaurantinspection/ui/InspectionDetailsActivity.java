package com.carbon.restaurantinspection.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.core.content.ContextCompat;

import com.carbon.restaurantinspection.R;
import com.carbon.restaurantinspection.model.InspectionDetail;
import com.carbon.restaurantinspection.model.InspectionManager;
import com.carbon.restaurantinspection.model.Violation;

import java.util.ArrayList;

/**
 * Inspection Details Activity contains all details of a specific restaurant's inspection.
 * Data is passed from Restaurant Details via intent encapsulation.
 * Contains a private MyArrayAdapter Class for the listView.
 */

public class InspectionDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_POSITION = "com.carbon.restaurantinspection.userinterface.InspectionDetailsActivity.position";
    private static final String EXTRA_TRACKING_NUMBER = "com.carbon.restaurantinspection.userinterface.InspectionDetailsActivity.trackingNumber";
    private int inspectionPosition;
    private String trackingNumber;
    private InspectionManager inspectionManager;
    ArrayList<InspectionDetail> inspectionList = new ArrayList<>();
    private ArrayList<Violation> violationList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inspectionManager = InspectionManager.getInstance(this);
        setContentView(R.layout.activity_inspection_details);

        toolbarBackButton();
        extractDataFromIntent();

        updateLists();

        if (violationList != null){
            updateViews();
        }
        populateListView();
        registerClickCallBack();
    }

    private void toolbarBackButton() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String activityTitle = getString(R.string.inspectionDetails);
        getSupportActionBar().setTitle(activityTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inspection_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back:
              finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void extractDataFromIntent() {
        Intent intent = getIntent();
        inspectionPosition = intent.getIntExtra(EXTRA_POSITION, 0);
        trackingNumber = intent.getStringExtra(EXTRA_TRACKING_NUMBER);
    }

    public static Intent makeIntent(Context context, int position, String trackingNumber) {
        Intent intent = new Intent(context, InspectionDetailsActivity.class);
        intent.putExtra(EXTRA_POSITION, position);
        intent.putExtra(EXTRA_TRACKING_NUMBER, trackingNumber);
        return intent;
    }

    private void registerClickCallBack() {
        ListView list = findViewById(R.id.inspection_listView);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Violation clickedViolation = violationList.get(position);
                Toast.makeText(InspectionDetailsActivity.this, clickedViolation.getDescription(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateLists(){
        inspectionList = inspectionManager.getInspections(trackingNumber);
        violationList = inspectionList.get(inspectionPosition).getViolations();
    }

    private void populateListView() {
        ArrayAdapter<Violation> adapter = new MyListAdapter();
        ListView list = findViewById(R.id.inspection_listView);
        list.setAdapter(adapter);
    }

    private void updateViews(){

        TextView textView = findViewById(R.id.inspection_dateText);
        String inspectionDate = inspectionList.get(inspectionPosition).getFullDate(InspectionDetailsActivity.this);
        String dateDisplay = getString(R.string.date) + " " + inspectionDate;
        textView.setText(dateDisplay);

        textView = findViewById(R.id.inspection_typeText);
        String inspectionType = inspectionList.get(inspectionPosition).getInspectionType();
        String inspectionTypeDisplay = getString(R.string.type) + " " + inspectionType;
        textView.setText(inspectionTypeDisplay);

        textView = findViewById(R.id.inspection_criticalText);
        int numCritical = inspectionList.get(inspectionPosition).getNumCritical();
        String criticalDisplay = getString(R.string.criticalIssues) + " " + numCritical;
        textView.setText(criticalDisplay);

        textView = findViewById(R.id.inspection_nonCriticalText);
        int numNonCritical = inspectionList.get(inspectionPosition).getNumNonCritical();
        String nonCriticalDisplay = getString(R.string.nonCriticalIssues) + " " + numNonCritical;
        textView.setText(nonCriticalDisplay);

        textView = findViewById(R.id.inspection_hazardText);
        String hazardLevel = inspectionList.get(inspectionPosition).getHazardLevel();
        String hazardDisplay = getString(R.string.hazardLevel) + " " + hazardLevel;
        textView.setText(hazardDisplay);

        ImageView imageView = findViewById(R.id.inspection_hazardImage);
        if(hazardLevel.equals("High")){
            imageView.setImageResource(R.drawable.red_skull_crossbones);
            textView.setTextColor(ContextCompat.getColor(this, R.color.highCriticalColour));
        }
        else if(hazardLevel.equals("Moderate")){
            imageView.setImageResource(R.drawable.ic_warning_yellow_24dp);
            textView.setTextColor(ContextCompat.getColor(this, R.color.moderateCriticalColour));
        }
        else{
            imageView.setImageResource(R.drawable.greencheckmark);
            textView.setTextColor(ContextCompat.getColor(this, R.color.lowCriticalColour));
        }
    }

    /**
     * MyListAdapter formats the row in the listView to include the necessary data.
     */
    private class MyListAdapter extends ArrayAdapter<Violation> {
        public MyListAdapter(){
            super(InspectionDetailsActivity.this, R.layout.inspection_item_view, violationList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View itemView = convertView;
            if (itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.inspection_item_view, parent, false);
            }

            Violation currentViolation = violationList.get(position);

            ImageView violationView = itemView.findViewById(R.id.item_violationIcon);
            int iconID = findResourceID(currentViolation);
            violationView.setImageResource(iconID);

            violationType(itemView, currentViolation);

            TextView descriptionText = itemView.findViewById(R.id.item_violationDescription);
            if(currentViolation.getStatus().equals("Critical")) {
                String critical = getString(R.string.critical);
                String violationCodeStatus = currentViolation.getCode() + ", " + critical;
                String violationCodeDisplay = getString(R.string.violationcode) + " " + violationCodeStatus;
                descriptionText.setText(violationCodeDisplay);
            } else {
                String notCritical = getString(R.string.notCritical);
                String violationCodeStatus = currentViolation.getCode() + ", " + notCritical;
                String violationCodeDisplay = getString(R.string.violationcode) + " " + violationCodeStatus;
                descriptionText.setText(violationCodeDisplay);
            }

            ImageView hazardView = itemView.findViewById(R.id.item_hazardStatus);
            if (currentViolation.getStatus().equals("Not Critical")){
                hazardView.setImageResource(R.drawable.yellow_caution);
            } else {
                hazardView.setImageResource(R.drawable.red_skull_crossbones);
            }
            return itemView;
        }

        private void violationType(View itemView, Violation currentViolation) {
            TextView titleText = itemView.findViewById(R.id.item_violationTitle);
            String violationType = currentViolation.getType();
            if(violationType.equals("Permit")) {
                String permit = getString(R.string.permit);
                String violationDisplay = permit + " " + getString(R.string.violation);
                titleText.setText(violationDisplay);
            }
            else if(violationType.equals("Food")) {
                String food = getString(R.string.food);
                String violationDisplay = food + " " + getString(R.string.violation);
                titleText.setText(violationDisplay);
            }
            else if(violationType.equals("Foodsafe")) {
                String foodSafe = getString(R.string.foodSafe);
                String violationDisplay = foodSafe + " " + getString(R.string.violation);
                titleText.setText(violationDisplay);
            }
            else if(violationType.equals("Pest")) {
                String pest = getString(R.string.pest);
                String violationDisplay = pest + " " + getString(R.string.violation);
                titleText.setText(violationDisplay);
            }
            else if(violationType.equals("Hygiene")) {
                String hygiene = getString(R.string.hygiene);
                String violationDisplay = hygiene + " " + getString(R.string.violation);
                titleText.setText(violationDisplay);
            }
            else {
                String equipment = getString(R.string.equipment);
                String violationDisplay = equipment + " " + getString(R.string.violation);
                titleText.setText(violationDisplay);
            }
        }
        private int findResourceID(Violation currentViolation){
            String type = currentViolation.getType();
            if (type.equals("Permit")){
                return R.drawable.permit;
            } else if (type.equals("Food")){
                return R.drawable.food;
            } else if (type.equals("Foodsafe")){
                return R.drawable.permit;
            } else if (type.equals("Pest")){
                return R.drawable.pest;
            } else if (type.equals("Hygiene")){
                return R.drawable.hygiene;
            } else {
                return R.drawable.equipment;
            }
        }
    }
}