package com.mego.fizoalarm.main;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mego.fizoalarm.R;
import com.mego.fizoalarm.databinding.FragmentMathRingingBinding;
import com.mego.fizoalarm.pojo.challenges.MathChallenge;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MathRingingFragment extends Fragment {

    private FragmentMathRingingBinding binding;

    private MathChallenge mMathChallengeConfig;
    private List<Pair<String,Integer>> mProblemsDataList;
    private List<TextInputLayout> textInputsList = new ArrayList<>();

    private RingingFragment.Callbacks mCallbacks;

    public MathRingingFragment() {
        // Required empty public constructor
    }

/*
    public static MathRingingFragment newInstance(ChallengeConfig challengeConfig) {
        if (! (challengeConfig instanceof MathChallengeConfig) )
            throw new IllegalStateException("Not a math challenge in MathRingingFragment .");
        MathRingingFragment fragment = new MathRingingFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MATH_CONFIG, (MathChallengeConfig)challengeConfig);
        fragment.setArguments(args);
        return fragment;
    }
*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMathChallengeConfig = (MathChallenge) getArguments().getSerializable(RingingActivity.ARG_CHALLENGE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentMathRingingBinding.inflate(inflater, container, false);
        assignProblemsList();

        for ( Pair<String,Integer> problem : mProblemsDataList) {

            MaterialCardView cardView = (MaterialCardView) inflater.inflate(R.layout.card_math_problem, container, false );

            TextView problemTextView = cardView.findViewById(R.id.problem_text);
            problemTextView.setId(View.generateViewId());
            problemTextView.setText(problem.first);


            TextInputLayout problemSolutionTextLayout = cardView.findViewById(R.id.problem_result_textInputLayout);
            problemSolutionTextLayout.setId(View.generateViewId());
            problemSolutionTextLayout.setTag(R.id.math_problem_result, problem.second);
            problemSolutionTextLayout.getEditText()
                    .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View view, boolean b) {
                            if (b) {
                                ((TextInputEditText)view).setText("");
                                problemSolutionTextLayout.setError(null);
                            }
                        }
                    });


            textInputsList.add(problemSolutionTextLayout);

            binding.problemsCardsContainer.addView(cardView);

        }


        binding.checkDismissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                boolean isAllCorrect = true;

                for (TextInputLayout solutionTextLayout : textInputsList ) {

                    int input = tryParse( solutionTextLayout.getEditText().getText().toString() );
                    if (input == (int)solutionTextLayout.getTag(R.id.math_problem_result) )
                        solutionTextLayout.setError(null);
                    else {
                        isAllCorrect = false;
                        solutionTextLayout.setError( getString(R.string.wrong) );
                    }
                }
                if (isAllCorrect)
                    mCallbacks.dismissAlarm();
            }
        });

        return binding.getRoot();
    }

    private void assignProblemsList() {

        switch ( mMathChallengeConfig.getDifficulty() ) {
            case MathChallenge.DIFFICULTY_EASY:
                mProblemsDataList = generateEasyMathProblems(mMathChallengeConfig.getProblemCount());
                break;
            case MathChallenge.DIFFICULTY_NORMAL:
                mProblemsDataList = generateNormalMathProblems(mMathChallengeConfig.getProblemCount());
                break;
            case MathChallenge.DIFFICULTY_HARD:
                mProblemsDataList = generateHardMathProblems(mMathChallengeConfig.getProblemCount());
                break;
            default:
                IllegalArgumentException ex;
                ex = new IllegalArgumentException("Unknown difficulty argument, Saved Difficulty Value = '"+mMathChallengeConfig.getDifficulty()+"'");
                mMathChallengeConfig.setDifficulty(MathChallenge.DIFFICULTY_EASY);
                mCallbacks.correctToDefaultChallengeAndReportProblem(mMathChallengeConfig, ex);
                assignProblemsList();
        }

    }

    private List<Pair<String,Integer>> generateEasyMathProblems(int numberOfProblems) {

        List< Pair<String,Integer> > easyProblemsList = new ArrayList<>();
        int a,b;
        Random random = new Random();
        for (int i = 0; i < numberOfProblems; i++) {

            a = random.nextInt(11);
            b = random.nextInt(11);
            String problemText = a + " * " + b + " = " ;

            easyProblemsList.add( new Pair<>(problemText, a*b) );
        }
        return easyProblemsList;
    }

    private List<Pair<String,Integer>> generateNormalMathProblems(int numberOfProblems) {

        List< Pair<String,Integer> > normalProblemsList = new ArrayList<>();
        int a,b,c;
        Random random = new Random();
        for (int i = 0; i < numberOfProblems; i++) {

            a = random.nextInt(11);
            b = random.nextInt(11);
            c = random.nextInt(11);
            String problemText = "( "+ a + " * " + b + " ) + " + c + " = " ;

            normalProblemsList.add( new Pair<>(problemText, (a*b)+c) );
        }
        return normalProblemsList;
    }

    private List<Pair<String,Integer>> generateHardMathProblems(int numberOfProblems) {

        List< Pair<String,Integer> > hardProblemsList = new ArrayList<>();
        int a,b,c,d;
        Random random = new Random();
        for (int i = 0; i < numberOfProblems; i++) {

            a = random.nextInt(11);
            b = random.nextInt(11);
            c = random.nextInt(11);
            d = random.nextInt(11);
            String problemText = "( "+ a + " * " + b + " ) + ( " + c + " * " + d + " ) = ";

            hardProblemsList.add( new Pair<>(problemText, (a*b)+(c*d) ) );
        }
        return hardProblemsList;
    }

    private int tryParse(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RingingFragment.Callbacks)
            mCallbacks = (RingingFragment.Callbacks) context;
        else
            throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

}