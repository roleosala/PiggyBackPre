package ph.edu.addu.richardleosala.piggyback;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import ph.edu.addu.richardleosala.piggyback.Database.DatabaseHelper;

public class LoadMessages extends AppCompatActivity {
    DatabaseHelper myDb;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_messages);
        myDb = new DatabaseHelper(this);
        __init__();
        populate();
    }

    private void __init__() {
        listView = findViewById(R.id.msgListView);
    }

    public void populate(){
        listView.setAdapter(null);
        ArrayList<String> listNumber = new ArrayList<>();
        Cursor data = myDb.getAllData();
        if(data.getCount() == 0){
            Toast.makeText(this, "No Messages to show.", Toast.LENGTH_SHORT).show();
        }else{
            while (data.moveToNext()){
                //boolean add = listNum.add(data.getString(1));
                listNumber.add(data.getString(2));
                ListAdapter listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listNumber);
                listView.setAdapter(listAdapter);
            }
        }
    }
}
