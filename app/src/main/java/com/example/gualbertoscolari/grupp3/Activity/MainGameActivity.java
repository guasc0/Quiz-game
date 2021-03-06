package com.example.gualbertoscolari.grupp3.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gualbertoscolari.grupp3.Logic.DbHelper;
import com.example.gualbertoscolari.grupp3.Logic.GameLogic;
import com.example.gualbertoscolari.grupp3.Logic.Profile;
import com.example.gualbertoscolari.grupp3.R;

import static com.example.gualbertoscolari.grupp3.Activity.ResultActivity.CORRECT_ANS_P1;
import static com.example.gualbertoscolari.grupp3.Activity.ResultActivity.CORRECT_ANS_P2;
import static com.example.gualbertoscolari.grupp3.Activity.ResultActivity.FIRST_PROFILE;
import static com.example.gualbertoscolari.grupp3.Activity.ResultActivity.SCORE_PLAYER1;
import static com.example.gualbertoscolari.grupp3.Activity.ResultActivity.SCORE_PLAYER2;
import static com.example.gualbertoscolari.grupp3.Activity.ResultActivity.SECOND_PROFILE;
import static com.example.gualbertoscolari.grupp3.Activity.ResultActivity.TIME_PLAYED_PLAYER1;
import static com.example.gualbertoscolari.grupp3.Activity.ResultActivity.TIME_PLAYED_PLAYER2;

//Metoden skall skapa upp ett gamelogic objekt som innehåller 10 frågor.
//Skall visa upp 1 fråga och 4 svar. Skall visa en timer från gamelogic.
//
public class MainGameActivity extends AppCompatActivity {

    public final static String CATEGORY = "chosen category";
    public final static String PLAYERS = "number of players";
    public final static String FIRSTPROFILE = "name of the player 1";
    public final static String SECONDPROFILE = "name of the player 2";
    private final Handler handler = new Handler();
    private int numberOfPlayers;
    private int correctAnsP1 = 0;
    private int correctAnsP2 = 0;
    private String smsQ;
    private String optA;
    private String optB;
    private String optC;
    private String optD;
    private long pausTime;
    private boolean resume;
    private GameLogic g1;
    private ImageView questionFrame;
    private TextView playerName;
    private TextView questiontv;
    private String chosenCat;
    private Button optABtn;
    private Button optBBtn;
    private Button optCBtn;
    private Button optDBtn;
    private Button smsBtn;
    private TextView cat;
    private TextView timerTV;
    private CountDownTimer timer;
    private ProgressBar progressbar;
    private int scoreValue;
    private int currentTime = 0;
    private int currentTime2 = 0;
    private int timePlayed = 0;
    private int timePlayed2 = 0;
    private boolean quit;

    private int gameRound = 1;

    private MediaPlayer mp;
    private MediaPlayer mp2;
    private MediaPlayer clock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);
        Bundle extras = getIntent().getExtras();

        chosenCat = extras.getString(CATEGORY);
        String p1Name = extras.getString(FIRSTPROFILE);
        String p2Name = extras.getString(SECONDPROFILE);
        numberOfPlayers = extras.getInt(String.valueOf(PLAYERS));
        cat = (TextView) findViewById(R.id.chosen_category);
        timerTV = (TextView) findViewById(R.id.timer_tv);
        progressbar = (ProgressBar) findViewById(R.id.progressbar);
        progressbar.setScaleY(4f);
        mp = MediaPlayer.create(this, R.raw.fail);
        mp2 = MediaPlayer.create(this, R.raw.correct_answer);


        if (numberOfPlayers == 1) {
            g1 = new GameLogic(p1Name, chosenCat, numberOfPlayers, this);
        } else {
            g1 = new GameLogic(p1Name, p2Name, chosenCat, numberOfPlayers, this);
        }

        playerName = (TextView) findViewById(R.id.profile_name);
        questiontv = (TextView) findViewById(R.id.question_tv);
        optABtn = (Button) findViewById(R.id.answer_btn_a);
        optBBtn = (Button) findViewById(R.id.answer_btn_b);
        optCBtn = (Button) findViewById(R.id.answer_btn_c);
        optDBtn = (Button) findViewById(R.id.answer_btn_d);
        smsBtn = (Button) findViewById(R.id.send_question);
        hideQuestion();
        smsBtn.setVisibility(View.GONE);
        questionFrame = (ImageView) findViewById(R.id.question_frame);
        optABtn.setEnabled(false);

        if (numberOfPlayers == 2) {
            smsBtn.setVisibility(View.GONE);
            smsBtn.setEnabled(false);
        }
        loadQuestionFrame();
        getReadyDialog();
    }

    /**
     * Method for increasing score, numberOfAnsweredQ.
     *
     * @param optString Takes a string and increases score if the guess is correct.
     */
    private void onButtonGuess(String optString) {
        // Ska användas OnClick på alla knappar när användaren gissar.
        // Ska kolla om den intrykta knappens text är lika med frågans correctAnswer.
        timer.cancel();
        if (!quit) {
            currentTime += timePlayed;
            currentTime2 += timePlayed2;

            if (g1.checkCorrectAnswer(optString)) {
                //Ifall man svarar rätt händer detta
                g1.increaseScore(scoreValue);

                if (g1.getCurrentPlayer() == g1.getP1()) {
                    correctAnsP1++;
                } else {
                    correctAnsP2++;
                }
            }
            g1.increaseNrOfAnsweredQuestion();
            g1.changePlayer();

            if (g1.getNumberOfAnsweredQ() == 10) {
                clock.stop();

                //Du har svarat på alla frågor , du tas till resultskärmen.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        goToResult();
                        finish();
                    }
                }, 1000); // 1000 milliseconds = 1 second
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (numberOfPlayers == 2) {
                            hideQuestion();
                            AlertDialog diabox = AskOption();
                            diabox.show();
                        } else {
                            displayQuestion();
                            if (!quit) {
                                resetTimer(10000);
                            }
                        }
                    }
                }, 1000); // 1000 milliseconds = 1 second
            }
        }
    }

    /**
     * @param time resets the timer
     */
    public void resetTimer(final long time) {
        clock = MediaPlayer.create(this, R.raw.clock);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                clock.setLooping(true);
                clock.start();
                timer = new CountDownTimer(time, 10) {
                    public void onTick(long millisUntilFinished) {
                        timerTV.setText(getString(R.string.timer_points) + millisUntilFinished / 100);
                        scoreValue = (int) (millisUntilFinished / 100);
                        timePlayed = 10 - ((int) (millisUntilFinished / 1000));

                        if (g1.getCurrentPlayer() == g1.getP2()) {
                            timePlayed2 = 10 - ((int) (millisUntilFinished / 1000));
                        }

                        int progress = (int) (millisUntilFinished / 100);
                        progressbar.setProgress(progress);
                        pausTime = millisUntilFinished;
                    }

                    public void onFinish() {
                        clock.reset();
                        progressbar.setProgress(0);
                        timerTV.setText("0");
                        onButtonGuess("");
                    }
                }.start();
            }
        }, 1000); // 1000 milliseconds = 1 second
    }

    /**
     * takes the player to result activity with intends
     */
    public void goToResult() {


        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(CATEGORY, chosenCat);
        intent.putExtra(PLAYERS, String.valueOf(numberOfPlayers));
        intent.putExtra(TIME_PLAYED_PLAYER1, currentTime);
        intent.putExtra(FIRST_PROFILE, g1.getP1().getName());
        intent.putExtra(SCORE_PLAYER1, String.valueOf(g1.getP1().getScore()));
        intent.putExtra(CORRECT_ANS_P1, correctAnsP1);
        checkHighscore(g1.getP1());

        if (numberOfPlayers == 2) {
            intent.putExtra(SECOND_PROFILE, g1.getP2().getName());
            intent.putExtra(SCORE_PLAYER2, String.valueOf(g1.getP2().getScore()));
            intent.putExtra(TIME_PLAYED_PLAYER2, currentTime2);
            intent.putExtra(CORRECT_ANS_P2, correctAnsP2);
            checkHighscore(g1.getP2());
        }
        startActivity(intent);
        finish();
    }

    /**
     * @param view handels options for button pressed
     */
    public void btnPressed(View view) {
        String buttonText = ((Button) view).getText().toString();
        Button button = (Button) findViewById(view.getId());
        smsBtn.setEnabled(false);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                smsBtn.setEnabled(true);
            }
        }, 2000);


        if (g1.checkCorrectAnswer(buttonText)) {
            button.setBackgroundDrawable(getResources().getDrawable(R.drawable.correctanswerbutton));
            mp2.start();
            clock.reset();

        } else {
            button.setBackgroundDrawable(getResources().getDrawable(R.drawable.wronganswerbutton));
            mp.start();
            clock.reset();
        }
        disableButtons();
        onButtonGuess(buttonText);
    }

    /**
     * graphics for the question frame
     */
    private void loadQuestionFrame() {

        switch (chosenCat) {
            case "Sport":
                questionFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.sportruta));
                break;

            case "Samhälle":
                questionFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.samhallruta));
                break;

            case "Kultur/Nöje":
                questionFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.kulturnojeruta));
                break;

            case "Historia":
                questionFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.historiaruta));
                break;

            case "Natur":
                questionFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.naturruta));
                break;

            default:
                questionFrame.setBackgroundDrawable(getResources().getDrawable(R.drawable.blandat));
        }
    }

    /**
     * number of rounds played
     */
    private void setRound() {
        TextView qAnswered;

        if (numberOfPlayers == 1) {
            qAnswered = (TextView) findViewById(R.id.questions_answered_tv);
            qAnswered.setText(getString(R.string.setround) + gameRound + "/10");
            gameRound++;

        } else if (numberOfPlayers == 2) {
            qAnswered = (TextView) findViewById(R.id.questions_answered_tv);
            qAnswered.setText(getString(R.string.setround) + gameRound + "/20");
            gameRound++;
        }

    }

    /**
     * @param profile1 updates the highscore for player
     */
    public void checkHighscore(Profile profile1) {
        DbHelper db = new DbHelper(this);
        db.updateHighScore(profile1, chosenCat);
        db.close();
    }

    @Override
    public void onBackPressed() {
        quit = true;
        clock.reset();
        Intent intent = new Intent(this, GameSettingsActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * @return resturns a alogbox and ask the player to click ok
     */
    private AlertDialog AskOption() {
        AlertDialog myQuittingDialogBox = new AlertDialog.Builder(this, R.style.dialogTheme)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {

                        displayQuestion();
                        if (!quit) {
                            resetTimer(10000);
                        }
                        dialog.dismiss();
                    }
                })
                .create();
        final TextView dialogView = new TextView(this);
        dialogView.setGravity(Gravity.CENTER_HORIZONTAL);
        dialogView.setTextSize(25);
        dialogView.setPadding(30, 30, 30, 30);
        dialogView.setText(getString(R.string.ask_option) + g1.getCurrentPlayer().getName() +
                getString(R.string.ask_option_start));
        myQuittingDialogBox.setView(dialogView);
        return myQuittingDialogBox;
    }

    /**
     * @return show a dialogbox countdown
     */
    private AlertDialog getReadyDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.dialogTheme).create();
        final TextView dialogView = new TextView(this);
        dialogView.setGravity(Gravity.CENTER_HORIZONTAL);
        dialogView.setTextSize(25);
        dialogView.setPadding(30, 30, 30, 30);
        alertDialog.setView(dialogView);
        alertDialog.show();

        alertDialog.setCancelable(false);

        new CountDownTimer(4000, 1) {
            @Override
            public void onTick(long millisUntilFinished) {
                dialogView.setText(getString(R.string.get_ready_dialog) + g1.getCurrentPlayer().getName() + "!" + "\n" + (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                alertDialog.dismiss();
                progressbar.setProgress(0);
                displayQuestion();
                if (!quit) {
                    resetTimer(10000);
                }
            }
        }.start();
        return alertDialog;
    }

    /**
     * displays a question
     */
    public void displayQuestion() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showQuestion();
                enableButtons();
                playerName.setText(g1.getCurrentPlayer().getName());
                questiontv.setText(g1.getQuestion().getQUESTION());
                cat.setText(chosenCat);
                //Randomize options
                optABtn.setText(g1.getQuestion().getOPTA());
                optBBtn.setText(g1.getQuestion().getOPTB());
                optCBtn.setText(g1.getQuestion().getOPTC());
                optDBtn.setText(g1.getQuestion().getOPTD());
                optABtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.standardcustombutton));
                optBBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.standardcustombutton));
                optCBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.standardcustombutton));
                optDBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.standardcustombutton));
                setRound();
                if (numberOfPlayers == 2) {
                    smsBtn.setVisibility(View.GONE);
                }
            }
        }, 1000);// 1000 milliseconds = 1 second
    }

    /**
     * @param view sends sms view
     */
    public void smsSend(View view) {
        hideQuestion();
        timer.cancel();
        clock.pause();
        resume = false;

        disableButtons();
        smsBtn.setEnabled(false);

        questiontv.setText(g1.getQuestion().getQUESTION());
        smsQ = questiontv.getText().toString();
        optA = optABtn.getText().toString();
        optB = optBBtn.getText().toString();
        optC = optCBtn.getText().toString();
        optD = optDBtn.getText().toString();


        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialogTheme);
        builder.setTitle(R.string.sms_send_nr);
        builder.setCancelable(false);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);


        builder.setPositiveButton(R.string.sms_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String test;
                test = input.getText().toString();


                if (test.charAt(0) != '0' || test.length() != 10) {
                    Toast.makeText(getApplicationContext(), R.string.sms_toast, Toast.LENGTH_SHORT).show();
                    input.setText("");

                    resume = true;
                    timerResume();
                    clock.start();

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            enableButtons();
                            smsBtn.setEnabled(true);
                            showQuestion();
                        }
                    }, 1000);

                } else {
                    String finalNumber = new StringBuilder(test).deleteCharAt(0).toString();
                    Log.d(getString(R.string.sms_telefon), finalNumber);
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage("+46" + finalNumber, null, getString(R.string.sms_onclick_question) + smsQ + " A : " +
                            optA + " B : " + optB + " C : " + optC + " D : " + optD, null, null);

                    dialog.dismiss();
                    resume = true;
                    timerResume();
                    clock.start();
                    smsBtn.setVisibility(View.GONE);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            enableButtons();
                            showQuestion();
                        }
                    }, 1000);
                }
            }
        });

        builder.setNegativeButton(R.string.sms_avbryt, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                resume = true;
                timerResume();
                clock.start();


                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        enableButtons();
                        showQuestion();
                        smsBtn.setEnabled(true);
                    }
                }, 1000);
            }
        });

        builder.show();
    }

    /**
     * resumes the timer on position it was stopped
     */
    private void timerResume() {
        if (resume && !quit) {
            resetTimer(pausTime);
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!(g1.getNumberOfAnsweredQ() == 10 || quit)) {
            System.exit(0);
        }
    }

    /**
     * enables buttons
     */
    public void enableButtons() {
        optABtn.setEnabled(true);
        optBBtn.setEnabled(true);
        optCBtn.setEnabled(true);
        optDBtn.setEnabled(true);
    }

    /**
     * disables buttons
     */
    public void disableButtons() {
        optABtn.setEnabled(false);
        optBBtn.setEnabled(false);
        optCBtn.setEnabled(false);
        optDBtn.setEnabled(false);
    }

    /**
     * shows the question
     */
    public void showQuestion() {
        optABtn.setVisibility(View.VISIBLE);
        optBBtn.setVisibility(View.VISIBLE);
        optCBtn.setVisibility(View.VISIBLE);
        optDBtn.setVisibility(View.VISIBLE);
        smsBtn.setVisibility(View.VISIBLE);
        questiontv.setVisibility(View.VISIBLE);
    }

    /**
     * hides the question
     */
    public void hideQuestion() {
        optABtn.setVisibility(View.GONE);
        optBBtn.setVisibility(View.GONE);
        optCBtn.setVisibility(View.GONE);
        optDBtn.setVisibility(View.GONE);
        smsBtn.setVisibility(View.GONE);
        questiontv.setVisibility(View.GONE);
    }
}
