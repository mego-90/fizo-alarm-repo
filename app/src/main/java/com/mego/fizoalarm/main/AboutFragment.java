package com.mego.fizoalarm.main;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

//import com.google.android.play.core.review.ReviewInfo;
//import com.google.android.play.core.review.ReviewManager;
//import com.google.android.play.core.review.ReviewManagerFactory;
//import com.google.android.play.core.tasks.Task;
import com.mego.fizoalarm.BuildConfig;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentAboutBinding;

import java.util.List;

public class AboutFragment extends Fragment {

    public static String FACEBOOK_URL = "https://www.facebook.com/fizo.alarm";
    public static String FACEBOOK_PAGE_ID = "fizo.alarm";
    private final String PRIVACY_POLICY_URL = "https://docs.google.com/document/d/1PdqLA284Zm21eIA3IKrXjdHeefJGukMj/edit?usp=sharing&ouid=108306703144972047162&rtpof=true&sd=true";

    private FragmentAboutBinding binding;
    //private ReviewManager manager;
    //private ReviewInfo reviewInfo;
    private Button rateAppBtn;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentAboutBinding.inflate(inflater, container, false);

        /*
        rateAppBtn = view.findViewById(R.id.about_rate_app_btn);
        rateAppBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Task<Void> flow = manager.launchReviewFlow(requireActivity(), reviewInfo);
                flow.addOnCompleteListener(task -> {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                });

            }
        });

        requestReviewInfo();

         */

        // OK Button
        binding.okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(AboutFragment.this)
                        .navigateUp();
            }
        });

        binding.versionNumber.setText(BuildConfig.VERSION_NAME);

        binding.facebookIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFacebookPage();
            }
        });

        binding.instagramIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openInstagramPage();
            }
        });

        binding.privacyPolicyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPrivacyPolicy();
            }
        });

        return binding.getRoot();
    }

    private void openFacebookPage() {

        Intent facebookIntent = new Intent(Intent.ACTION_VIEW);
/*
        if ( isFacebookInstalled() ) {
            String facebookUrl = getFacebookPageURL( requireActivity().getApplicationContext() );
            facebookIntent.setData(Uri.parse(facebookUrl));
            startActivity(facebookIntent);

  */     // } else {
            facebookIntent.setData(Uri.parse(FACEBOOK_URL));
       // }

        startActivity(facebookIntent);


        /*
        final String urlFb = "fb://page/"+"fizo.alarm";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(urlFb));

        // If a Facebook app is installed, use it. Otherwise, launch
        // a browser
        final PackageManager packageManager = requireActivity().getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() == 0) {
            final String urlBrowser = "https://www.facebook.com/fizo.alarm";
            intent.setData(Uri.parse(urlBrowser));
        }

        startActivity(intent);
        */
    }

    public boolean isFacebookInstalled() {
        try {
            requireActivity().getApplicationContext().getPackageManager().getApplicationInfo("com.facebook.katana", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public String getFacebookPageURL(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            int versionCode = packageManager.getPackageInfo("com.facebook.orca", 0).versionCode;
            if (versionCode >= 3002850) { //newer versions of fb app
                return "fb://faceweb/f?href=" + FACEBOOK_URL;
            } else { //older versions of fb app
                return "fb://page/" + FACEBOOK_PAGE_ID;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return FACEBOOK_URL; //normal web url
        }
    }


    private void openInstagramPage() {
        Uri uri = Uri.parse("http://instagram.com/_u/fizo.alarm");
        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

        likeIng.setPackage("com.instagram.android");

        try {
            startActivity(likeIng);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://instagram.com/fizo.alarm")));
        }
    }

    public void openPrivacyPolicy() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL));
        startActivity(intent);
    }



/*
    private void requestReviewInfo() {
        manager = ReviewManagerFactory.create(requireActivity());
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We can get the ReviewInfo object
                reviewInfo = task.getResult();
                rateAppBtn.setVisibility( View.VISIBLE );
            } else {
                // There was some problem, log or handle the error code.
                if ( task.getException() != null )
                    FirebaseCrashlytics.getInstance().recordException( task.getException() );
            }
        });
    }
*/

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

}