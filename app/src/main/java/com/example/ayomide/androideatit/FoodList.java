package com.example.ayomide.androideatit;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.ayomide.androideatit.Common.Common;
import com.example.ayomide.androideatit.Database.Database;
import com.example.ayomide.androideatit.Interface.ItemClickListener;
import com.example.ayomide.androideatit.Model.Food;
import com.example.ayomide.androideatit.Utils.UniversalImageLoader;
import com.example.ayomide.androideatit.ViewHolder.FoodViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class FoodList extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    DatabaseReference foodList;
    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter, searchAdapter;

    String categoryId = "";

    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    Database localDB;

    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        //init Firebase
        foodList = FirebaseDatabase.getInstance().getReference("Food");

        localDB = new Database( this );

        recyclerView = (RecyclerView) findViewById(R.id.recycler_food);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        swipeRefreshLayout = findViewById( R.id.swipe_layout );
        swipeRefreshLayout.setColorSchemeResources( R.color.colorPrimary,
                android.R.color.holo_blue_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_green_dark);
        swipeRefreshLayout.setOnRefreshListener( new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(getIntent() != null)
                    categoryId = getIntent().getStringExtra("CategoryId");
                if(!categoryId.isEmpty())
                {
                    if(Common.isConnectedToTheInternet(getBaseContext()))
                        loadListFood(categoryId);
                    else {
                        Toast.makeText(FoodList.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        } );

        swipeRefreshLayout.post( new Runnable() {
            @Override
            public void run() {
                if(getIntent() != null)
                    categoryId = getIntent().getStringExtra("CategoryId");
                if(!categoryId.isEmpty())
                {
                    if(Common.isConnectedToTheInternet(getBaseContext()))
                        loadListFood(categoryId);
                    else {
                        Toast.makeText(FoodList.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                //Search
                materialSearchBar = findViewById(R.id.searchBar);
                materialSearchBar.setHint("Enter your food");
                //materialSearchBar.setSpeechMode(false); no need for this since it's already defined in the xml

                loadSuggest();

                materialSearchBar.setCardViewElevation( 10 );
                materialSearchBar.addTextChangeListener( new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        //when user type their text, we will change the suggest list

                        List<String> suggest = new ArrayList<String>();
                        for (String search:suggest)
                        {
                            if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                                suggest.add(search);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                } );
                materialSearchBar.setOnSearchActionListener( new MaterialSearchBar.OnSearchActionListener() {
                    @Override
                    public void onSearchStateChanged(boolean enabled) {
                        //when search bar is closed
                        //Restore original adapter
                        if(!enabled)
                            recyclerView.setAdapter(adapter);
                    }

                    @Override
                    public void onSearchConfirmed(CharSequence text) {
                        //when search finish
                        //show result of search adapter
                        startSearch(text);
                    }

                    @Override
                    public void onButtonClicked(int buttonCode) {

                    }
                } );
            }
        } );


    }

    private void startSearch(CharSequence text) {
        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(
                Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                foodList.orderByChild("name").equalTo(text.toString()) //Compare name
        ) {
            @Override
            protected void populateViewHolder(final FoodViewHolder viewHolder, final Food model, final int position) {

                viewHolder.food_name.setText(model.getName());

                try {
                    UniversalImageLoader.setImage(model.getImage(), viewHolder.food_image, null, "");
                    Log.d("Photo: ", model.getImage() );
                }catch (Exception e){
                    //...
                }

                if (localDB.isFavorite( adapter.getRef( position ).getKey() ))
                    viewHolder.fav_image.setImageResource( R.drawable.ic_favorite_black_24dp );

                viewHolder.fav_image.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!localDB.isFavorite( adapter.getRef( position ).getKey() ))
                        {
                            localDB.addToFavorites( adapter.getRef( position ).getKey() );
                            viewHolder.fav_image.setImageResource( R.drawable.ic_favorite_black_24dp );
                            Toast.makeText(FoodList.this, model.getName()+" was added to Favourites", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            localDB.removeFromFavorites( adapter.getRef( position ).getKey() );
                            viewHolder.fav_image.setImageResource( R.drawable.ic_favorite_border_black_24dp );
                            Toast.makeText(FoodList.this, model.getName()+" was removed from Favourites", Toast.LENGTH_SHORT).show();
                        }
                    }
                } );

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Intent foodDetail = new Intent(FoodList.this, FoodDetail.class);
                        foodDetail.putExtra("foodId", searchAdapter.getRef(position).getKey()); //Send Food Id to new activity
                        startActivity(foodDetail);
                    }
                });
            }
        };

        recyclerView.setAdapter(searchAdapter); //set adapter for recyclerView in search result
    }


    private void loadSuggest() {
        foodList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener( new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot postSnapshot : dataSnapshot.getChildren())
                        {
                            Food item = postSnapshot.getValue(Food.class);
                            suggestList.add(item.getName()); //Add name of food to suggest list
                        }

                        materialSearchBar.setLastSuggestions(suggestList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadListFood(String categoryId) {

        //Like: Select * from Foods where MenuId
        adapter =  new FirebaseRecyclerAdapter<Food, FoodViewHolder>(Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                foodList.orderByChild("menuId").equalTo(categoryId) // like : Select * from Foods where MenuId = CategoryId
        ) {
            @Override
            protected void populateViewHolder(final FoodViewHolder viewHolder, final Food model, final int position) {

                viewHolder.food_name.setText(model.getName());
                viewHolder.food_price.setText( String.format( "N %s", model.getPrice() ) );
                try {
                    UniversalImageLoader.setImage(model.getImage(), viewHolder.food_image, null, "");
                    Log.d("Photo: ", model.getImage() );
                }catch (Exception e){
                    //...
                }

                viewHolder.btn_share.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String subject = "CHOW";
                        String food = viewHolder.food_name.getText().toString();
                        String text = "Checkout the amazing "+food+" in CHOW";
                        Intent intent = new Intent();
                        intent.setType( "message/rfc2822" ); //message/rfc2822 is a mime type for email messages
                        intent.setAction(  Intent.ACTION_SEND  );
                        intent.putExtra( Intent.EXTRA_SUBJECT, subject );
                        intent.putExtra( Intent.EXTRA_TEXT, text );
                        startActivity( intent );
                    }
                } );

                if (localDB.isFavorite( adapter.getRef( position ).getKey() ))
                    viewHolder.fav_image.setImageResource( R.drawable.ic_favorite_black_24dp );

                viewHolder.fav_image.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!localDB.isFavorite( adapter.getRef( position ).getKey() ))
                        {
                            localDB.addToFavorites( adapter.getRef( position ).getKey() );
                            viewHolder.fav_image.setImageResource( R.drawable.ic_favorite_black_24dp );
                            Toast.makeText( FoodList.this, model.getName()+" was added to Favourites", Toast.LENGTH_SHORT ).show();
                        }
                        else
                        {
                            localDB.removeFromFavorites( adapter.getRef( position ).getKey() );
                            viewHolder.fav_image.setImageResource( R.drawable.ic_favorite_border_black_24dp );
                            Toast.makeText( FoodList.this, model.getName()+" was removed from Favourites", Toast.LENGTH_SHORT ).show();
                        }
                    }
                } );

                final Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        Intent foodDetail = new Intent(FoodList.this, FoodDetail.class);
                        foodDetail.putExtra("foodId", adapter.getRef(position).getKey()); //Send Food Id to new activity
                        startActivity(foodDetail);
                    }
                });
            }
        };

        //Set Adapter
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setRefreshing( false );
    }


}
