package naneos.analyze;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private List<dataObject> dummyData;
    private ListView listView;
    private Button btn_syncOnce;
    private ArrayAdapter<dataObject> adapter;
    private Random rand;
    private FirebaseFirestore db;
    private Date currentTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        rand = new Random();
        listView = (ListView) findViewById(R.id.lv_main);
        btn_syncOnce = (Button) findViewById(R.id.btn_syncOnce);
        dummyData = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dummyData);
        listView.setAdapter(adapter);
        currentTime = Calendar.getInstance().getTime();

        for (int i = 0; i < 1000; i++) {
            dataObject obj = new dataObject(i, currentTime, rand.nextInt(1000000));
            dummyData.add(obj);
        }

        adapter.notifyDataSetChanged();

        btn_syncOnce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncDataOnce();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void syncDataOnce() {
        for (int i = 0; i < dummyData.size(); i++) {
            db.collection("DummyData").add(dummyData.get(i))
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d("syncDataOnce", "DocumentSnapshot written with ID: " + documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("syncDataOnce", "Error adding document", e);
                        }
                    });
        }
    }


}


