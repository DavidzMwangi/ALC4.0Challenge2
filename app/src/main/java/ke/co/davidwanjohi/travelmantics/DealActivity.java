package ke.co.davidwanjohi.travelmantics;





import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.util.Objects;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private EditText titleTxt;
    private EditText descriptionTxt;
    private EditText priceTxt;
    private ImageView dealImage;
//    private ImageButton dealImage;
    private ProgressDialog progressDialog;
    private Uri imageURI;
    private TravelDeal travelDeal;
    private String downloadUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);

        firebaseDatabase = FirebaseUtil.firebaseDatabase;
        databaseReference = FirebaseUtil.databaseReference;
        storageReference = FirebaseUtil.storageReference;

        progressDialog = new ProgressDialog(this);
        titleTxt = (EditText) findViewById(R.id.txtTitle);
        descriptionTxt = (EditText) findViewById(R.id.txtDescription);
        priceTxt = (EditText) findViewById(R.id.txtPrice);
        dealImage = (ImageView) findViewById(R.id.image);

        Intent intent = getIntent();
        TravelDeal travelDeal = (TravelDeal) intent.getSerializableExtra("TravelDeal");
        if (travelDeal == null) {
            travelDeal = new TravelDeal();
        }
        this.travelDeal = travelDeal;
        titleTxt.setText(travelDeal.getTitle());
        descriptionTxt.setText(travelDeal.getDescription());
        priceTxt.setText(travelDeal.getPrice());

        dealImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        Picasso.with(this)
                    .load(travelDeal.getImageUrl())

                    .into(dealImage);

        Button saveBtn=(Button) findViewById(R.id.save);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDeal();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        CropImage.ActivityResult result = CropImage.getActivityResult(data);
        if (resultCode == RESULT_OK) {

            imageURI =data.getData();
        }

        dealImage.setImageURI(imageURI);

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);


            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);



        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal saved", Toast.LENGTH_LONG).show();
                clean();
//                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal Deleted", Toast.LENGTH_LONG).show();
//                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    private void clean() {
        titleTxt.setText("");
        descriptionTxt.setText("");
        priceTxt.setText("");
//        dealImage.setImageDrawable(getResources().getDrawable(R.drawable.add_deal_image_btn));
        titleTxt.requestFocus();
    }



    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void saveDeal() {
        progressDialog.setMessage("Saving deal...");
        progressDialog.show();

        if (travelDeal.getId() != null) {

            travelDeal.setImageUrl(travelDeal.getImageUrl());
            travelDeal.setTitle(titleTxt.getText().toString());
            travelDeal.setDescription(descriptionTxt.getText().toString());
            travelDeal.setPrice(priceTxt.getText().toString());

            databaseReference.child(travelDeal.getId()).setValue(travelDeal).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getApplicationContext(), "Deal Saved", Toast.LENGTH_LONG).show();
                    clean();
                    startActivity(new Intent(getApplicationContext(), ListActivity.class));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("PushError", e.getMessage());
                }
            });
        } else {
            final StorageReference filePath = storageReference.child("TravelDeal_Images")
                    .child((Objects.requireNonNull(imageURI.getLastPathSegment())));
            filePath.putFile(imageURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                    String imageName = task.getResult().getStorage().getPath();
                    Log.d("imageName", imageName);
                    travelDeal.setImageName(imageName);
                    if (task.isSuccessful()) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                downloadUrl = uri.toString();
                                travelDeal.setImageUrl(downloadUrl);
                                travelDeal.setTitle(titleTxt.getText().toString());
                                travelDeal.setDescription(descriptionTxt.getText().toString());
                                travelDeal.setPrice(priceTxt.getText().toString());

                                if (travelDeal.getId() == null) {
                                    Log.d("checkID", travelDeal.getImageUrl() + "\n\n" + travelDeal.getTitle() + "\n\n" + travelDeal.getDescription());
                                    databaseReference.push().setValue(travelDeal).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(getApplicationContext(), "Deal Saved", Toast.LENGTH_LONG).show();
                                            clean();
                                            startActivity(new Intent(getApplicationContext(), ListActivity.class));
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("PushError", e.getMessage());
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            });

        }
    }

    private void deleteDeal() {
        if (travelDeal.getId() == null) {
            Toast.makeText(getApplicationContext(), "Save the deal before deleting", Toast.LENGTH_SHORT).show();
            return;
        } else {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setMessage("Are you sure you want to delete this deal?");
            alertDialog.setCancelable(true);
            alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, int i) {
                    databaseReference.child(travelDeal.getId()).removeValue();
                    StorageReference picRef = FirebaseUtil.firebaseStorage.getReference().child(travelDeal.getImageName());
                    picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Delete Image", "Successful deletion");
                            Toast.makeText(getApplicationContext(), "Deal Deleted!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), ListActivity.class));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("Delete Image", e.getMessage());
                        }
                    });
                }
            });

            alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            alertDialog.show();

        }

    }

    private void enableEditTexts(boolean state) {
        titleTxt.setEnabled(state);
        descriptionTxt.setEnabled(state);
        priceTxt.setEnabled(state);
        dealImage.setEnabled(state);
    }


}


