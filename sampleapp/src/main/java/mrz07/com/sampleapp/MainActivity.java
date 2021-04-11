package mrz07.com.sampleapp;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import emzy07.com.sampleapp.R;
import mrz07.com.betterratedialog.BetterRateDialog;
import mrz07.com.betterratedialog.NegativeReviewListener;
import mrz07.com.betterratedialog.ReviewListener;
import mrz07.com.betterratedialog.ShowMode;


public class MainActivity extends AppCompatActivity implements NegativeReviewListener, ReviewListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BetterRateDialog betterRateDialog = new BetterRateDialog(this, "your@mail.com");
        betterRateDialog.setRateText("Your custom text")
                .setTitle("Your custom title")
                .setForceMode(false)
                .setUpperBound(2)
                .setNegativeReviewListener(this)
                .setReviewListener(this)
                .setGooglePlayInAppReviewMode(true)
                .showAfter(ShowMode.LAUNCH_TIMES, 2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNegativeReview(int stars) {
        Log.d(TAG, "Negative review " + stars);
    }

    @Override
    public void onReview(int stars) {
        Log.d(TAG, "Review " + stars);
    }
}
