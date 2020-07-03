package com.mohamedtaha.imagine.firebasedatabase;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import static com.mohamedtaha.imagine.firebasedatabase.DealAdapter.CURRENT_DEAL;
import static com.mohamedtaha.imagine.firebasedatabase.FirebaseUtil.DEAL_PICTURES;

public class DealActivity extends AppCompatActivity {
    public static final String TRAVEL_DEAL = "traveldeal";
    public static final String INSERT_PICTURE = "insert_image";
    public static final int NUMBER_PUCTIRE_RETREVE = 42;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private EditText title, price, description;
    private Button uploadImage;
    private Intent intent;
    private TravelDeal deal;
    private ImageView upload_image_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intent = getIntent();
        FirebaseUtil.openFbReference(TRAVEL_DEAL,this);
        firebaseDatabase = FirebaseUtil.firebaseDatabase;
        databaseReference = FirebaseUtil.databaseReference;
        title = findViewById(R.id.et_title);
        price = findViewById(R.id.et_price);
        description = findViewById(R.id.et_description);
        uploadImage = findViewById(R.id.upload_image);
        upload_image_view = findViewById(R.id.upload_image_view);
        TravelDeal currentDeal = (TravelDeal) intent.getSerializableExtra(CURRENT_DEAL);
        if (currentDeal == null) {
            currentDeal = new TravelDeal();
        }
        //else {
            this.deal = currentDeal;
            title.setText(deal.getTitle());
            price.setText(deal.getPrice());
            description.setText(deal.getDescription());
            showImage(deal.getImageUrl());
            uploadImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/jpeg");
                    intent.putExtra(intent.EXTRA_LOCAL_ONLY,true);
                    startActivityForResult(intent.createChooser(intent, INSERT_PICTURE), NUMBER_PUCTIRE_RETREVE);
                }
            });

       // }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NUMBER_PUCTIRE_RETREVE && resultCode == RESULT_OK){
            Uri imageUri = data.getData();
            StorageReference reference = FirebaseUtil.storageReference.child(DEAL_PICTURES);
            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    String url = uri.toString();
                    String picture_name =uri.getPath();
                    deal.setImageUrl(url);
                    deal.setImageName(picture_name);
                    Log.d("setImageUrl", url);
                    Log.d("setImageName", picture_name);
                    showImage(url);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d("onFailure", exception.getMessage());
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.save_menu, menu);
        if (FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enabledEditTexts(true);
            uploadImage.setVisibility(View.VISIBLE);

        }else {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enabledEditTexts(false);
            uploadImage.setVisibility(View.GONE);

        }
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.save_menu:
                saveDeal();
                // Toast.makeText(this, "Deal saved", Toast.LENGTH_SHORT).show();
                clearTexts();
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                backToList();

                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void saveDeal() {
        deal.setTitle(title.getText().toString().trim());
        deal.setPrice(price.getText().toString().trim());
        deal.setDescription(description.getText().toString().trim());
        if (deal.getId() == null){
            databaseReference.push().setValue(deal);
        }else {
            databaseReference.child(deal.getId()).setValue(deal);
        }
    }
    private void deleteDeal(){
        if (deal == null){
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_SHORT).show();
            return;
        }else {
            databaseReference.child(deal.getId()).removeValue();
            Toast.makeText(this, "Deal Deleted", Toast.LENGTH_SHORT).show();
            if (deal.getImageName() != null && deal.getImageName().isEmpty() == false){
                StorageReference picRef = FirebaseUtil.firebaseStorage.getReference().child(deal.getImageName());
                picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("onSuccess", "image deleted");

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("onFailure", e.getMessage());
                    }
                });
            }
        }
    }
    private void backToList(){
        Intent intent = new Intent(DealActivity.this,ListActivity.class);
        startActivity(intent);
    }

    private void clearTexts() {
        title.setText("");
        price.setText("");
        description.setText("");
        title.requestFocus();
    }
    private void enabledEditTexts(boolean isEnabled){
        title.setEnabled(isEnabled);
        price.setEnabled(isEnabled);
        description.setEnabled(isEnabled);


    }
    private void showImage(String url){
        if (url !=null && url.isEmpty() == false){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.with(this)
                    .load(url)
                    .resize(width,width*2/3)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(upload_image_view);
        }
    }
}