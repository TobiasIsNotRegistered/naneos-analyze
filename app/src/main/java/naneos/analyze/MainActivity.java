package naneos.analyze;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        rand = new Random();
        listView = (ListView) findViewById(R.id.lv_main);
        btn_syncOnce = (Button) findViewById(R.id.btn_syncOnce);
        btn_flush = (Button) findViewById(R.id.btn_flush);
        switch_keepSynced =(Switch) findViewById(R.id.switch_keepSynced);
        dummyData = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dummyData);
        listView.setAdapter(adapter);

        index = 0;

        btn_syncOnce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addDataToFirestoreOnce();
            }
        });
        btn_flush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dummyData.clear();
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void addData(int i){
            DataObject obj = new DataObject(i, currentTime, rand.nextInt(1000000));
            dummyData.add(obj);
            adapter.notifyDataSetChanged();
            listView.smoothScrollToPosition(adapter.getCount() -1);

            if(switch_keepSynced.isChecked()){
                addDataToFirestoreContinously(obj);
            }
    }

    @Override
    protected void onResume() {

        h.postDelayed( runnable = new Runnable() {
            public void run() {
                //do something
                currentTime = Calendar.getInstance().getTime();
                addData(index++);
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

    private void addDataToFirestoreOnce() {
        for (int i = 0; i < dummyData.size(); i++) {

            db.collection("DummyData").document(dummyData.get(i).date.toString()).set(dummyData.get(i))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("addDataToFirestoreOnce", "DocumentSnapshot written with ID: " + "?");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("addDataToFirestoreOnce", "Error adding document", e);
                        }
                    });
        }
    }


    private void addDataToFirestoreContinously(DataObject obj) {
            db.collection("DummyData").document(obj.date.toString()).set(obj)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("addDataToFirestoreOnce", "DocumentSnapshot written with ID: " + "?");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("addDataToFirestoreOnce", "Error adding document", e);
                        }
                    });
    }


}


