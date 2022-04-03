package com.mego.fizoalarm.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.mego.fizoalarm.databinding.FragmentOnboardingBinding;
import com.mego.fizoalarm.pojo.Onboarding;

public class OnboardingFragment extends Fragment {

    private static final String ARG_ONBOARDING_ITEM = "com.mego.hellalarm.main.OnboardingFragment.ARG_ONBOARDING_ITEM";

    private FragmentOnboardingBinding binding;

    private Onboarding mOnboarding;

    public OnboardingFragment() {

    }


    public static OnboardingFragment newInstance(Onboarding onboarding) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ONBOARDING_ITEM, onboarding);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mOnboarding = (Onboarding) getArguments().getSerializable(ARG_ONBOARDING_ITEM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentOnboardingBinding.inflate(inflater, container, false);

        binding.onboardingTitle.setText( mOnboarding.getTitle() );
        binding.onboardingDescription.setText( mOnboarding.getDescription() );
        binding.onboardingImage.setImageResource( mOnboarding.getImage() );
        binding.onboardingPermissionBtn.setVisibility( mOnboarding.getPermissionBtnVisibility() );

        if (mOnboarding.getCallable() != null)
            binding.onboardingPermissionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        mOnboarding.getCallable().call();
                    } catch (Exception e) {

                    }
                }
            });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

}