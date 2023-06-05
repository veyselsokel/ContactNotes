package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    private ListView contactsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contactsListView = findViewById(R.id.contactsListView);

        // Rehberden izin al
        requestContactsPermission();
    }

    private void requestContactsPermission() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            // Rehber erişim izni zaten verilmiş
            displayContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // İzin verildi
                displayContacts();
            } else {
                // İzin verilmedi
                Toast.makeText(this, "Rehber erişim izni verilmedi.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayContacts() {
        ArrayList<String> contactList = new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            int contactNameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            while (cursor.moveToNext()) {
                String contactName = cursor.getString(contactNameColumnIndex);
                contactList.add(contactName);
            }
            cursor.close();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, contactList);
        contactsListView.setAdapter(adapter);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedContact = parent.getItemAtPosition(position).toString();
                openNotesDialog(selectedContact);
            }
        });
    }

    private void openNotesDialog(String contactName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_notes, null);
        builder.setView(dialogView);
        builder.setTitle("Notlar - " + contactName);

        final EditText notesEditText = dialogView.findViewById(R.id.notesEditText);

        builder.setPositiveButton("Kaydet", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String notes = notesEditText.getText().toString();
                saveNotesToFirebase(contactName, notes);
                showNotesFromFirebase(contactName);
            }
        });

        builder.setNegativeButton("İptal", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void saveNotesToFirebase(String contactName, String notes) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference notesRef = database.getReference("notes");

            // Notları contactName altında kaydet
            notesRef.child(contactName).setValue(notes)
                    .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Notlar kaydedildi.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Notları kaydetme başarısız oldu.", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Firebase hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showNotesFromFirebase(String contactName) {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference notesRef = database.getReference("notes");

            notesRef.child(contactName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String notes = snapshot.getValue(String.class);
                        Toast.makeText(MainActivity.this, "Notlar: " + notes, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Kaydedilmiş not bulunamadı.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Veritabanı hatası: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Firebase hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
