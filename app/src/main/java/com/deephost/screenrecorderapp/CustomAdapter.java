package com.deephost.screenrecorderapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.deephost.screenrecorderapp.MainActivity.arraylistString;

public class CustomAdapter extends ArrayAdapter<RecordingDatasetList> {
    
    List<RecordingDatasetList> bl;
    Context context;
    int br;
    File file;
    public CustomAdapter(Context bc, int br, List<RecordingDatasetList> bl) {
        super(bc, br, bl);
        this.context = bc;
        this.br = br;
        this.bl = bl;
    }
    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(br, null, false);
        TextView ab = view.findViewById(R.id.ab);
        TextView ac = view.findViewById(R.id.ac);
        TextView ad = view.findViewById(R.id.ad);
        final ImageView bd = view.findViewById(R.id.bd);
        bd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(getContext(),bd);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.item_delete:


                                bdd(position);

                                return true;

                            case R.id.item_share:
                                bss(position);
                                return true;
                        }
                        popupMenu.dismiss();

                        return true;
                    }
                });

                popupMenu.show();
            }
        });


        RecordingDatasetList A = bl.get(position);

        //adding values to the list item

        AssetManager assetManager = context.getAssets();

        try {
            InputStream inputStream = assetManager.open(A.getaa());

            Drawable drawable = Drawable.createFromStream(inputStream,"");

            ImageView imageView = (ImageView) view.findViewById(R.id.aa);

            imageView.setImageDrawable(drawable);

        } catch (IOException e) {
            e.printStackTrace();
        }


        ab.setText(A.getab());
        ac.setText(A.getac());
        ad.setText(A.getad());




        return view;
    }

    private void bss(final  int position) {
        File file = new File(Environment.getExternalStorageDirectory() + "/Screen Recording/"+ arraylistString.get(position));
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("video/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, arraylistString.get(position));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share with"));
    }

    private void bdd(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirm Delete ?");
        builder.setMessage("Are you sure you want delete this !");

        builder.setIcon(R.drawable.ic_delete_black_24dp);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                file = new File(Environment.getExternalStorageDirectory() + "/Screen Recording/"+ arraylistString.get(position));
                file.delete();
                bl.remove(position);
                notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}