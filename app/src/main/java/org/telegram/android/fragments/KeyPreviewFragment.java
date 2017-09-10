package org.telegram.android.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.model.User;
import org.telegram.android.media.Optimizer;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 16.10.13
 * Time: 17:58
 */
public class KeyPreviewFragment extends TelegramFragment {
    private byte[] hash;
    private int uid;

    public KeyPreviewFragment(int uid, byte[] hash) {
        this.hash = hash;
        this.uid = uid;
    }

    public KeyPreviewFragment() {

    }

    private Bitmap createBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        int[] colors = new int[]{
                0xffffffff,
                0xffd5e6f3,
                0xff2d5775,
                0xff2f99c9};
        for (int i = 0; i < 64; i++) {
            int index = (hash[i / 4] >> (2 * (i % 4))) & 0x3;
            bitmap.setPixel(i % 8, i / 8, colors[index]);
        }
        return bitmap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = wrap(inflater).inflate(R.layout.key_fragment, container, false);

        Bitmap bitmap = createBitmap();

        int width = getResources().getDisplayMetrics().widthPixels - getPx(64);

        ((ImageView) view.findViewById(R.id.keyImage)).setImageBitmap(Bitmap.createScaledBitmap(bitmap, width, width, false));

        User user = application.getEngine().getUser(uid);

        ((TextView) view.findViewById(R.id.infoMessage1)).setText(html(getStringSafe(R.string.st_secret_key_info1).replace("{name}", "<b>" + user.getFirstName() + "</b>")));

        ((TextView) view.findViewById(R.id.infoMessage2)).setText(html(getStringSafe(R.string.st_secret_key_info2).replace("{name}", "<b>" + user.getFirstName() + "</b>")));

        Linkify.addLinks((TextView) view.findViewById(R.id.infoMessage3), Linkify.WEB_URLS);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.code_menu, menu);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_secret_key_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.shareCode) {
            try {
                Bitmap forSharing = Bitmap.createScaledBitmap(createBitmap(), 128, 128, false);
                String fileName = getTempExternalFile("key.jpg");
                Optimizer.save(forSharing, fileName);
                startPickerActivity(new Intent(Intent.ACTION_SEND)
                        .setType("image/jpeg")
                        .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(fileName))));
            } catch (Exception e) {
                Toast.makeText(getActivity(), R.string.st_error_unknown, Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
