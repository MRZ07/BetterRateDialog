package mrz07.com.betterratedialog;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class BetterRateDialog implements DialogInterface.OnClickListener {

    private final static String SP_NUM_OF_ACCESS = "numOfAccess";
    private final static String SP_MAX_DATE = "maxDate";
    private final static String PATTERN = "yyyy/MM/dd";
    private static final String SP_DISABLED = "disabled";
    //private static final String TAG = BetterRateDialog.class.getSimpleName();
    private final Context context;
    private boolean isForceMode = false;
    private final SharedPreferences sharedPrefs;
    private String defaultTitle;
    private String defaultText;
    private String supportEmail;
    private TextView contentTextView;
    private RatingBar ratingBar;
    private String title = null;
    private String rateText = null;
    private AlertDialog alertDialog;
    private View dialogView;
    private int upperBound = 4;
    private NegativeReviewListener negativeReviewListener;
    private ReviewListener reviewListener;
    private InAppReviewListener inAppReviewEventListener;
    private int starColor;
    private String positiveButtonText;
    private String negativeButtonText;
    private String neutralButtonText;
    private boolean googlePlayInAppReviewMode = false;

    public BetterRateDialog(Context context, String supportEmail) {
        this.context = context;
        negativeButtonText = context.getString(R.string.BtnLater);
        positiveButtonText = context.getString(R.string.BtnOK);
        neutralButtonText = context.getString(R.string.BtnNever);
        defaultTitle = context.getString(R.string.RateApp);
        defaultText = context.getString(R.string.DefaultText);
        sharedPrefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        this.supportEmail = supportEmail;
    }

    public Activity getActivity(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            } else {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }

        return null;
    }

    private void build() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        dialogView = inflater.inflate(R.layout.stars, null);
        String titleToAdd = (title == null) ? defaultTitle : title;
        String textToAdd = (rateText == null) ? defaultText : rateText;
        contentTextView = dialogView.findViewById(R.id.text_content);
        contentTextView.setText(textToAdd);
        ratingBar = dialogView.findViewById(R.id.ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                if (isForceMode || v >= upperBound) {
                    if (googlePlayInAppReviewMode && !isAmazonDevice()) { // Enable only for Google Play devices if googlePlayInAppReviewMode is enabled.
                        launchInAppReview();
                    } else {
                        openStorePage();
                    }
                    if (reviewListener != null)
                        reviewListener.onReview((int) ratingBar.getRating());
                }
            }
        });

        if (starColor != -1) {
            LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
            stars.getDrawable(1).setColorFilter(starColor, PorterDuff.Mode.SRC_ATOP);
            stars.getDrawable(2).setColorFilter(starColor, PorterDuff.Mode.SRC_ATOP);
        }

        builder.setTitle(titleToAdd)
                .setView(dialogView);

        if (negativeButtonText != null && !negativeButtonText.isEmpty())
            builder.setNegativeButton(negativeButtonText, this);
        if (positiveButtonText != null && !positiveButtonText.isEmpty())
            builder.setPositiveButton(positiveButtonText, this);
        if (neutralButtonText != null && !neutralButtonText.isEmpty())
            builder.setNeutralButton(neutralButtonText, this);
        alertDialog = builder.create();
    }

    private void disable() {
        SharedPreferences shared = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putBoolean(SP_DISABLED, true);
        editor.apply();
    }

    private static boolean isAmazonDevice() {
        return android.os.Build.MANUFACTURER.equalsIgnoreCase("Amazon");
    }

    private void openStorePage() {
        final String appPackageName = context.getPackageName();

        if (isAmazonDevice()) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("amzn://apps/android?p=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.amazon.com/gp/mas/dl/android?p=" + appPackageName)));
            }
        } else {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        }
    }

    private void sendEmail() {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{supportEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Report (" + context.getPackageName() + ")");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    private boolean isMaxDateEmpty() {
        if (!sharedPrefs.contains(SP_MAX_DATE)) {
            return true;
        } else return TextUtils.isEmpty(sharedPrefs.getString(SP_MAX_DATE, ""));
    }

    public void show() {
        boolean disabled = sharedPrefs.getBoolean(SP_DISABLED, false);
        if (!disabled) {
            build();
            alertDialog.show();
        }
    }

    public void showAfter(ShowMode showMode, int number) {
        build();
        SharedPreferences.Editor editor = sharedPrefs.edit();

        switch (showMode) {
            case DAYS:
                launchAfterNDays(number, editor);
                break;
            case LAUNCH_TIMES:
                launchAfterNumberOfLaunches(number, editor);
                break;
        }
    }

    private void launchAfterNDays(int number, SharedPreferences.Editor editor) {
        Date maxDate;
        if (isMaxDateEmpty()) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, number);

            SimpleDateFormat formatter = new SimpleDateFormat(PATTERN, Locale.US);
            maxDate = c.getTime();
            editor.putString(SP_MAX_DATE, formatter.format(maxDate));
            editor.apply();
        } else {
            try {
                maxDate = new SimpleDateFormat(PATTERN, Locale.US).parse(sharedPrefs.getString(SP_MAX_DATE, ""));

                long diffInMillie = Math.abs((new Date()).getTime() - maxDate.getTime());
                long diff = TimeUnit.DAYS.convert(diffInMillie, TimeUnit.MILLISECONDS);

                if (diff >= number) {
                    show();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void launchAfterNumberOfLaunches(int numberOfAccess, SharedPreferences.Editor editor) {
        int numOfAccess = sharedPrefs.getInt(SP_NUM_OF_ACCESS, 0);
        editor.putInt(SP_NUM_OF_ACCESS, numOfAccess + 1);
        editor.apply();
        if (numOfAccess + 1 >= numberOfAccess) {
            show();
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case DialogInterface.BUTTON_POSITIVE:
                if (ratingBar.getRating() < upperBound) {
                    if (negativeReviewListener == null) {
                        sendEmail();
                    } else {
                        negativeReviewListener.onNegativeReview((int) ratingBar.getRating());
                    }
                } else if (!isForceMode) {
                    openStorePage();
                }
                disable();
                if (reviewListener != null) {
                    reviewListener.onReview((int) ratingBar.getRating());
                }
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                disable();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt(SP_NUM_OF_ACCESS, 0);
                if (sharedPrefs.contains(SP_MAX_DATE)) {
                    editor.putString(SP_MAX_DATE, "");
                }
                editor.apply();
                break;
        }
        alertDialog.hide();
    }

    public BetterRateDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    public BetterRateDialog setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
        return this;
    }

    public BetterRateDialog setRateText(String rateText) {
        this.rateText = rateText;
        return this;
    }

    public BetterRateDialog setStarColor(int color) {
        starColor = color;
        return this;
    }

    public BetterRateDialog setPositiveButtonText(String positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
        return this;
    }

    public BetterRateDialog setNegativeButtonText(String negativeButtonText) {
        this.negativeButtonText = negativeButtonText;
        return this;
    }

    public BetterRateDialog setNeutralButton(String neutralButtonText) {
        this.neutralButtonText = neutralButtonText;
        return this;
    }

    /**
     * Set to true if you want to send the user directly to the market
     *
     * @param isForceMode
     * @return
     */
    public BetterRateDialog setForceMode(boolean isForceMode) {
        this.isForceMode = isForceMode;
        return this;
    }

    /**
     * Set the upper bound for the rating.
     * If the rating is >= of the bound, the market is opened.
     *
     * @param bound the upper bound
     * @return the dialog
     */
    public BetterRateDialog setUpperBound(int bound) {
        this.upperBound = bound;
        return this;
    }

    /**
     * Set a custom listener if you want to OVERRIDE the default "send email" action when the user gives a negative review
     *
     * @param listener
     * @return
     */
    public BetterRateDialog setNegativeReviewListener(NegativeReviewListener listener) {
        this.negativeReviewListener = listener;
        return this;
    }

    /**
     * Set a listener to get notified when a review (positive or negative) is issued, for example for tracking purposes
     *
     * @param listener
     * @return
     */
    public BetterRateDialog setReviewListener(ReviewListener listener) {
        this.reviewListener = listener;
        return this;
    }

    /**
     * Enable in-app review popup in Google Play
     *
     * @param googlePlayInAppReviewMode
     * @return
     */
    public BetterRateDialog setGooglePlayInAppReviewMode(boolean googlePlayInAppReviewMode) {
        this.googlePlayInAppReviewMode = googlePlayInAppReviewMode;
        return this;
    }

    /**
     * Set a listener to get notified when in app review flow completed, for example for tracking purposes
     * <p>
     * Note that, The API does not indicate whether the user reviewed or not, or even whether the review dialog was shown
     *
     * @param inAppReviewEventListener
     * @return
     */
    public BetterRateDialog setInAppReviewEventListener(InAppReviewListener inAppReviewEventListener) {
        this.inAppReviewEventListener = inAppReviewEventListener;
        return this;
    }

    private void launchInAppReview() {
        ReviewManager manager = ReviewManagerFactory.create(context);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();

                Task<Void> flow = manager.launchReviewFlow(getActivity(context), reviewInfo);
                flow.addOnCompleteListener(task2 -> {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                    if (inAppReviewEventListener != null) {
                        inAppReviewEventListener.onInAppReviewComplete();
                    }
                });

            } else {
                // There was some problem, log or handle the error code.
                @ReviewErrorCode int reviewErrorCode = ((ReviewException) Objects.requireNonNull(task.getException())).getErrorCode();
                System.err.println("launchInAppReview -> reviewErrorCode: " + reviewErrorCode);
                openStorePage();
            }
        });
    }
}
