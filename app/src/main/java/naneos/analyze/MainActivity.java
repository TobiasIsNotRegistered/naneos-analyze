package naneos.analyze;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private List<DataObject> dummyData;
    private ListView listView;
    private Button btn_syncOnce;
    private Button btn_flush;
    private Switch switch_keepSynced;
    private ArrayAdapter<DataObject> adapter;
    private Random rand;
    private FirebaseFirestore db;
    private Date currentTime;

    private Handler h = new Handler();
    private int delay = 1000; //1 second=1000 milisecond, 15*1000=15seconds
    private Runnable runnable;
    private int indexOfDataObject;

    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /* ******************* DB ********************* */

        db = FirebaseFirestore.getInstance();
        rand = new Random();
        listView = (ListView) findViewById(R.id.lv_main);
        btn_syncOnce = (Button) findViewById(R.id.btn_syncOnce);
        btn_flush = (Button) findViewById(R.id.btn_flush);
        switch_keepSynced =(Switch) findViewById(R.id.switch_keepSynced);
        dummyData = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dummyData);
        listView.setAdapter(adapter);

        indexOfDataObject = 0;

        btn_syncOnce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAllDataInFirestoreOnce();
            }
        });
        btn_flush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dummyData.clear();
                adapter.notifyDataSetChanged();
            }
        });

        /* ******************* DRAWER ********************* */
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
                    }
                });

        /* ******************* TOOLBAR ********************* */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void addData(int i){
            DataObject obj = new DataObject(i, currentTime, rand.nextInt(1000000));
            dummyData.add(obj);
            adapter.notifyDataSetChanged();
            listView.smoothScrollToPosition(adapter.getCount() -1);

            if(switch_keepSynced.isChecked()){
                setNewDataInFirestoreContinuously(obj);
            }
    }

    @Override
    protected void onResume() {

        h.postDelayed( runnable = new Runnable() {
            public void run() {
                //do something
                currentTime = Calendar.getInstance().getTime();
                addData(indexOfDataObject++);
                h.postDelayed(runnable, delay);
            }
        }, delay);

        super.onResume();

    }

    @Override
    protected void onPause() {
        h.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }

    private void setAllDataInFirestoreOnce() {
        for (int i = 0; i < dummyData.size(); i++) {

            db.collection("DummyData").document(dummyData.get(i).date.toString()).set(dummyData.get(i))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Firestore", "DocumentSnapshot written with ID: " + "?");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("Firestore", "Error adding document", e);
                        }
                    });
        }
    }


    private void setNewDataInFirestoreContinuously(DataObject obj) {
            db.collection("DummyData").document(obj.date.toString()).set(obj)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Firestore", "DocumentSnapshot written with ID: " + "?");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("Firestore", "Error adding document", e);
                        }
                    });
    }





}


