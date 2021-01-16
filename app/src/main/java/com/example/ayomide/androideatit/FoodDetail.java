package com.example.ayomide.androideatit;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.example.ayomide.androideatit.Common.Common;
import com.example.ayomide.androideatit.Database.Database;
import com.example.ayomide.androideatit.Model.Food;
import com.example.ayomide.androideatit.Model.Order;
import com.example.ayomide.androideatit.Model.Rating;
import com.example.ayomide.androideatit.Utils.UniversalImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.stepstone.apprating.AppRatingDialog;
import com.stepstone.apprating.listener.RatingDialogListener;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class FoodDetail extends AppCompatActivity implements RatingDialogListener {

    TextView food_name, food_price, food_description;
    ImageView food_image;
    ElegantNumberButton numberButton;
    CollapsingToolbarLayout collapsingToolbarLayout;
    FloatingActionButton fabCart, btnRating;
    RatingBar ratingBar;

    DatabaseReference foods, ratingTbl;

    String foodId = "";

    Food currentFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_food_detail );

        //Firebase
        foods = FirebaseDatabase.getInstance().getReference("Food");
        ratingTbl = FirebaseDatabase.getInstance().getReference("Rating");

        //init view
        numberButton = findViewById(R.id.number_button);
        fabCart = findViewById(R.id.btn_cart);
        btnRating = findViewById( R.id.btn_rating );

        btnRating.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRatingDialog();
            }
        } );

        fabCart.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Common.isConnectedToTheInternet(getBaseContext())) {
                    new Database( getBaseContext() ).addToCart( new Order(
                            foodId,
                            currentFood.getName(),
                            numberButton.getNumber(),
                            currentFood.getPrice(),
                            currentFood.getDiscount()
                    ) );

                    Toast.makeText( FoodDetail.this, "added to cart", Toast.LENGTH_LONG ).show();
                }
                else
                   Toast.makeText(FoodDetail.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            }
        });

        food_image = findViewById(R.id.img_food);
        food_name = findViewById(R.id.food_name);
        food_price = findViewById(R.id.food_price);
        food_description = findViewById(R.id.food_description);

        collapsingToolbarLayout = findViewById(R.id.collapsing);

        //Get Food Id from Intent
        if(getIntent() != null)
            foodId = getIntent().getStringExtra("foodId");
        if(!foodId.isEmpty())
        {
            if(Common.isConnectedToTheInternet(getBaseContext()))
            {
                getFoodDetails(foodId);
                getFoodRating(foodId);
            }
            else {
                Toast.makeText(FoodDetail.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    private void showRatingDialog()
    {
        new AppRatingDialog.Builder()
                .setPositiveButtonText("Submit")
                .setNegativeButtonText("Cancel")
                .setNoteDescriptions(Arrays.asList("Very Bad", "Not good", "Quite ok", "Very Good", "Excellent !!!"))
                .setDefaultRating(1)
                .setTitle("Did you like the food?")
                .setDescription("Let us know what you think")
                .setTitleTextColor( R.color.colorPrimary )
                .setDescriptionTextColor( R.color.colorPrimary )
                .setHint( "Please write your comment here" )
                .setHintTextColor( R.color.colorAccent )
                .setCommentTextColor( R.color.white )
                .setCommentBackgroundColor( R.color.colorPrimaryDark )
                .setWindowAnimation( R.style.RatingDialogFadeAnimation )
                .create( FoodDetail.this )
                .show();
    }

    private void getFoodDetails(String foodId)
    {
        foods.child(foodId).addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentFood = dataSnapshot.getValue(Food.class);

                try {
                    UniversalImageLoader.setImage(currentFood.getImage(), food_image, null, "");
                    Log.d("Photo: ", currentFood.getImage() );
                }catch (Exception e){
                    //...
                }


                food_name.setText(currentFood.getName());

                food_price.setText(currentFood.getPrice());

                food_description.setText(currentFood.getDescription());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //..
            }
        });
    }

    private void getFoodRating(String foodId)
    {
        Query foodRating = ratingTbl.orderByChild("foodId").equalTo( foodId );
        foodRating.addValueEventListener( new ValueEventListener() {
            int count=0, sum=0;
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren())
                {
                    Rating item = postSnapshot.getValue(Rating.class);
                    sum+=Integer.parseInt( item.getRateValue() );
                    count++;
                }
                if(count != 0)
                {
                    float average = sum/count;
                    ratingBar.setRating(average);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        } );
    }

    @Override
    public void onNegativeButtonClicked() {
        //..
    }

    @Override
    public void onPositiveButtonClicked(int value, @NotNull String comment) {
        //Getting rating and upload
        final Rating rating = new Rating(Common.currentUser.getPhone(),
                foodId,
                String.valueOf(value),
                comment);

        ratingTbl.child(Common.currentUser.getPhone()).child( foodId ).addListenerForSingleValueEvent( new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child( Common.currentUser.getPhone()).child( foodId ).exists())
                {
                    //remove old value
                    ratingTbl.child( Common.currentUser.getPhone() ).child( foodId ).removeValue();
                    //update with new value
                    ratingTbl.child( Common.currentUser.getPhone() ).child( foodId ).setValue( rating );
                    Toast.makeText(FoodDetail.this,"Thank you for the feedback", Toast.LENGTH_SHORT).show();
                    finish();
                }
                else
                {
                    //update with new value
                    ratingTbl.child( Common.currentUser.getPhone() ).child( foodId ).setValue( rating );
                    Toast.makeText(FoodDetail.this,"Thank you for the feedback", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        } );
    }
}
