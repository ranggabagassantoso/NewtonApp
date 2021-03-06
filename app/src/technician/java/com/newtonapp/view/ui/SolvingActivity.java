package com.newtonapp.view.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;

import com.newtonapp.R;
import com.newtonapp.data.database.entity.Customer;
import com.newtonapp.data.database.entity.Problem;
import com.newtonapp.data.database.entity.Solving;
import com.newtonapp.data.network.APIHelper;
import com.newtonapp.data.network.pojo.request.HoldRequestModel;
import com.newtonapp.data.network.pojo.request.KunjunganRequestModel;
import com.newtonapp.data.network.pojo.request.SolvedRequestModel;
import com.newtonapp.data.network.pojo.request.TrackingRequestModel;
import com.newtonapp.data.network.pojo.response.TrackingResponseModel;
import com.newtonapp.utility.CommonUtil;
import com.newtonapp.utility.Constants;
import com.newtonapp.utility.DebugUtil;
import com.newtonapp.utility.NetworkUtil;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * page: Solving
 * normal scenario:
 * 1. check problem availbility (problem tracking) from server {@link #checkOnGoingProblemAvailbility()}
 * 2. check problem's status complain {@link #onSuccessTracking(TrackingResponseModel)}
 * 3. set how page should be shown {@link #onSuccessTracking(TrackingResponseModel)}
 * 4. verify (cek kunjungan) customer's problem {@link #verify()}
 * 5. if verify problem succeed, then {@link #setSolvingMode()}
 * 6. solved customer's problem {@link #onSolved()}
 * 7. if solved problem succeed, navigate to {@link ApprovalActivity}
 * 8. if hold problem succeed, navigate back to {@link DashboardActivity}
 * */
public class SolvingActivity extends BaseActivity {

    private static final String TAG = SolvingActivity.class.getSimpleName();
    private Toolbar toolbar;
    private LinearLayout llSolvingBodyKunjungan, llSolvingBodySolving;
    private AppCompatEditText etIdCustomer;
    private AppCompatEditText etIdPrinter;
    private AppCompatEditText etSolvingNote;
    private AppCompatSpinner spSolvingOption;
    private AppCompatButton btnVerify;
    private AppCompatButton btnSolved;
    private AppCompatButton btnHold;
    private AppCompatImageButton imgBtnScan;

    private Customer customer;
    private String idCustomer;
    private String idPrinter;
    private String solvingNote;
    private String solvingOption;
    private String[] arrSolvingOptions;
    private boolean isOpeningCamera = false;
    private VIEW_MODE CURRENT_MODE = VIEW_MODE.KUNJUNGAN_MODE;

    enum VIEW_MODE {
        BLOCK_MODE,
        KUNJUNGAN_MODE,
        SOLVING_MODE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solving);
        initView();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isOpeningCamera) checkOnGoingProblemAvailbility();
        else isOpeningCamera = false;
    }

    @Override
    public Activity onCreateGetCurrentActivity() {
        return this;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String qrcodeValue = data != null ? data.getStringExtra(Constants.EXTRA_INTENT_RESULT_SCAN) : null;

        Log.d(TAG, "requestCode : " + requestCode);
        Log.d(TAG, "resultCode : " + resultCode);
        Log.d(TAG, "data barcode : " + qrcodeValue);
        if (requestCode == Constants.RC_SCAN_BARCODE && resultCode == RESULT_OK) {
            etIdPrinter.setText(qrcodeValue);
        }
    }

    private void initView() {
        toolbar = findViewById(R.id.header_layout_toolbar);
        llSolvingBodyKunjungan = findViewById(R.id.solving_layout_body_kunjungan);
        llSolvingBodySolving = findViewById(R.id.solving_layout_body_solving);
        spSolvingOption = findViewById(R.id.solving_sp_options);
        etIdCustomer = findViewById(R.id.solving_et_idcustomer);
        etIdPrinter = findViewById(R.id.solving_et_idprinter);
        etSolvingNote = findViewById(R.id.solving_et_note);
        btnVerify = findViewById(R.id.solving_btn_verify);
        btnSolved = findViewById(R.id.solving_btn_solved);
        btnHold = findViewById(R.id.solving_btn_hold);
        imgBtnScan = findViewById(R.id.solving_imgbtn_camera);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.screen_solving);

        arrSolvingOptions = getResources().getStringArray(R.array.solving_options);
        ArrayAdapter<String> spSolvingOptionsAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, arrSolvingOptions);
        spSolvingOption.setAdapter(spSolvingOptionsAdapter);

        setMode();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setListener() {
        btnVerify.setOnClickListener(view -> onVerify());
        btnSolved.setOnClickListener(view -> onSolved());
        btnHold.setOnClickListener(view -> onHold());
        imgBtnScan.setOnClickListener(view -> onOpenCameraStarScanning());
        etIdCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                idCustomer = editable.toString();
                setMode();
            }
        });
        etIdPrinter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                idPrinter = editable.toString();
                setMode();
            }
        });

        spSolvingOption.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                solvingOption = adapterView.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        /*etIdPrinter.setOnTouchListener((view, event) -> {
            final int DRAWABLE_LEFT = 0;
            final int DRAWABLE_TOP = 1;
            final int DRAWABLE_RIGHT = 2;
            final int DRAWABLE_BOTTOM = 3;

            if(event.getAction() == MotionEvent.ACTION_UP) {
                if(event.getRawX() >= (etIdPrinter.getRight() - etIdPrinter.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    // your action here
                    isOpeningCamera = true;
                    Intent openCameraIntent = new Intent(this, CameraPreviewActivity.class);
                    startActivityForResult(openCameraIntent, Constants.RC_SCAN_BARCODE);
                    return true;
                }
            }
            return false;
        });*/

        etSolvingNote.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                solvingNote = editable.toString();
            }
        });
    }

    private void checkOnGoingProblemAvailbility() {
        showMessageDialog(getString(R.string.progress_getting_latest_tracking));
        TrackingRequestModel formBody = new TrackingRequestModel();
        formBody.setToken(loginToken.toString());
        DebugUtil.d("tracking token: " + loginToken.toString());
        compositeDisposable.add(
                APIHelper.track(formBody)
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribeOn(Schedulers.io())
                         .subscribe(
                                 response -> {
                                     hideDialog();
                                     if (response == null) {
                                         throw new NullPointerException(getString(R.string.error_null_response));
                                     }

                                     DebugUtil.d("response: " + response.toString());
                                     if (response.getStatus() == 1) {
                                         //Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                         onSuccessTracking(response);
                                     } else {
                                         Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
                                         CURRENT_MODE = VIEW_MODE.BLOCK_MODE;
                                         setMode();
                                     }
                                 }, error -> {
                                     hideDialog();
                                     CURRENT_MODE = VIEW_MODE.BLOCK_MODE;
                                     setMode();
                                     String errorMessage = NetworkUtil.handleApiError(error);
                                     Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                 }
                         )
        );
    }

    private void onSuccessTracking(TrackingResponseModel response) {
        if (response.getData() != null) {
            customer = response.getData().get(0);
            if (customer != null) {
                Problem problem = customer.getProblems().get(0);
                if (problem != null) {
                    String idCustomer = customer.getIdCust();
                    String idPrinter  = problem.getIdProduk();
                    String solvingNote = "";
                    Solving solving = problem.getSolving();
                    if (solving != null) {
                        int posSolvingOption = CommonUtil.getIndex(solving.getSolvingOption(), arrSolvingOptions);
                        spSolvingOption.setSelection(posSolvingOption);
                        if (!TextUtils.isEmpty(problem.getSolving().getSolvingNote()))
                            solvingNote = problem.getSolving().getSolvingNote();
                    }

                    DebugUtil.d("id problem: " + customer.getProblems().get(0).getIdProblem());
                    DebugUtil.d("id customer: " + customer.getIdCust());
                    DebugUtil.d("id printer: " + customer.getProblems().get(0).getIdProduk());
                    DebugUtil.d("status complain: " + problem.getStatusComplain());
                    switch (problem.getStatusComplain()) {
                        case Constants.FLAG_START_PROGRESS: // sudah ambil job, perlu "checkin" / "kunjungan"
                            CURRENT_MODE = VIEW_MODE.KUNJUNGAN_MODE;
                            break;
                        case Constants.FLAG_KUNJUNGAN: // sudah melakukan kunjungan, tinggal solving
                        case Constants.FLAG_HOLD: // problem status dihold
                            CURRENT_MODE = VIEW_MODE.SOLVING_MODE;
                            etIdCustomer.setText(idCustomer);
                            etIdPrinter.setText(idPrinter);
                            etSolvingNote.setText(solvingNote);
                            break;
                        case Constants.FLAG_SOLVED: // sudah klik "solved", pindah ke "Finishing"
                            finishing(customer);
                            break;
                        default:
                            //Toast.makeText(this, getString(R.string.success_message_no_ongoing_problem), Toast.LENGTH_LONG).show();
                            break;
                    }

                    setMode();
                    return;
                }
            }
        }

        //Toast.makeText(this, getString(R.string.success_message_no_ongoing_problem), Toast.LENGTH_LONG).show();
    }

    // verifikasi sebagai kunjungan
    private void onVerify() {
        if (!TextUtils.isEmpty(idCustomer) && !TextUtils.isEmpty(idPrinter)) {
            if (customer != null &&
                    idCustomer.equals(customer.getIdCust()) &&
                    idPrinter.equals(customer.getProblems().get(0).getIdProduk())) {
                verify();
            } else Toast.makeText(this, getString(R.string.error_problem_not_found), Toast.LENGTH_SHORT).show();
        } else Toast.makeText(this, getString(R.string.error_blank_fields), Toast.LENGTH_LONG).show();
    }

    private void onSolved() {
        if (isValid()) {
            solved(idCustomer, idPrinter, solvingOption, solvingNote);
        }
        else Toast.makeText(this, getString(R.string.error_blank_fields), Toast.LENGTH_LONG).show();
    }

    private void onHold() {
        if (isValid()) {
            hold(idCustomer, idPrinter, solvingOption, solvingNote);
        }
        else Toast.makeText(this, getString(R.string.error_blank_fields), Toast.LENGTH_LONG).show();
    }

    private void onOpenCameraStarScanning() {
        isOpeningCamera = true;
        Intent openCameraIntent = new Intent(this, CameraPreviewActivity.class);
        startActivityForResult(openCameraIntent, Constants.RC_SCAN_BARCODE);
    }

    private void verify() {
        if (customer != null) {
            showMessageDialog(getString(R.string.progress_verifying_kunjungan));
            KunjunganRequestModel formBody = new KunjunganRequestModel();
            formBody.setToken(loginToken.toString());
            formBody.setProb(customer.getProblems().get(0).getIdProblem());
            compositeDisposable.add(
                    APIHelper.kunjunganSolvingin(formBody)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    response -> {
                                        hideDialog();
                                        if (response == null) {
                                            throw new NullPointerException(getString(R.string.error_null_response));
                                        }

                                        if (response.getStatus() == 1) {
                                            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                            onSuccessVerifyKunjungan(customer);
                                        } else {
                                            Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }, error -> {
                                        hideDialog();
                                        String errorMessage = NetworkUtil.handleApiError(error);
                                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                    }
                            )
            );
        }
    }

    private void solved(String idCustomer, String idPrinter, String solvingOption, String solvingNote) {
        if (customer != null) {
            showMessageDialog(getString(R.string.progress_solved));
            SolvedRequestModel formBody = new SolvedRequestModel();
            formBody.setProb(customer.getProblems().get(0).getIdProblem());
            formBody.setIdCust(idCustomer);
            formBody.setSn(idPrinter);
            formBody.setSolveOpt(solvingOption);
            formBody.setNote(solvingNote);
            formBody.setToken(loginToken.toString());
            compositeDisposable.add(
                    APIHelper.solved(formBody)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    response -> {
                                        hideDialog();
                                        if (response == null) {
                                            throw new NullPointerException(getString(R.string.error_null_response));
                                        }

                                        if (response.getStatus() == 1) {
                                            Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                            Solving solving = new Solving();
                                            solving.setSolvingOption(solvingOption);
                                            solving.setSolvingNote(solvingNote);
                                            onSuccessSolved(customer, solving);
                                        } else {
                                            Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    }, error -> {
                                        hideDialog();
                                        String errorMessage = NetworkUtil.handleApiError(error);
                                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                    }
                            )
            );
        }
    }

    private void hold(String idCustomer, String idPrinter, String solvingOption, String solvingNote) {
        if (customer != null) {
            showMessageDialog(getString(R.string.progress_hold));
            HoldRequestModel formBody = new HoldRequestModel();
            formBody.setProb(customer.getProblems().get(0).getIdProblem());
            formBody.setIdCust(idCustomer);
            formBody.setSn(idPrinter);
            formBody.setSolveOpt(solvingOption);
            formBody.setNote(solvingNote);
            formBody.setToken(loginToken.toString());
            compositeDisposable.add(
                    APIHelper.hold(formBody)
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribeOn(Schedulers.io())
                             .subscribe(
                                     response -> {
                                         hideDialog();
                                         if (response == null) {
                                             throw new NullPointerException(getString(R.string.error_null_response));
                                         }

                                         if (response.getStatus() == 1) {
                                             Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                                             Solving solving = new Solving();
                                             solving.setSolvingOption(solvingOption);
                                             solving.setSolvingNote(solvingNote);
                                             onSuccessHold(customer, solving);
                                         } else {
                                             Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
                                         }
                                     }, error -> {
                                         hideDialog();
                                         String errorMessage = NetworkUtil.handleApiError(error);
                                         Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                                     }
                             )
            );
        }
    }

    private void onSuccessVerifyKunjungan(Customer customer) {
        setSolvingMode();
    }

    private void onSuccessSolved(Customer customer, Solving solving) {
        customer.getProblems().get(0).setSolving(solving);
        finishing(customer);
    }

    private void onSuccessHold(Customer customer, Solving solving) {
        finish(); // back to main menu
    }

    private void finishing(Customer customer) {
        setOngoingCustomerProblem(customer);
        navigateTo(this, ApprovalActivity.class);
        finish();
    }

    private boolean isValid() {
        return !TextUtils.isEmpty(idCustomer) &&
                !TextUtils.isEmpty(idPrinter) &&
                !TextUtils.isEmpty(solvingOption) &&
                !TextUtils.isEmpty(solvingNote);
    }

    private boolean isKunjunganFieldFilled() {
        return !TextUtils.isEmpty(idCustomer) &&
                !TextUtils.isEmpty(idPrinter);
    }

    private void setMode() {
        switch (CURRENT_MODE) {
            case BLOCK_MODE: setBlockMode(); break;
            case KUNJUNGAN_MODE: setKunjunganMode(); break;
            case SOLVING_MODE: setSolvingMode(); break;
            default: setKunjunganMode(); break;
        }
    }

    private void setBlockMode() {
        llSolvingBodySolving.setVisibility(View.GONE);
        notifyDisabledInputKunjungan();
    }

    private void setKunjunganMode() {
        llSolvingBodySolving.setVisibility(View.GONE);
        notifyEnabledInputKunjungan();
        if (!isKunjunganFieldFilled()) btnVerify.setEnabled(false);
    }

    private void setSolvingMode() {
        llSolvingBodySolving.setVisibility(View.VISIBLE);
        notifyDisabledInputKunjungan();
    }

    private void notifyEnabledInputKunjungan() {
        etIdCustomer.setEnabled(true);
        //etIdPrinter.setEnabled(true);
        btnVerify.setEnabled(true);
    }

    private void notifyEnabledSolving() {
        spSolvingOption.setEnabled(true);
        etSolvingNote.setEnabled(true);
        btnSolved.setEnabled(true);
        btnHold.setEnabled(true);
    }

    private void notifyDisabledInputKunjungan() {
        etIdCustomer.setEnabled(false);
        //etIdPrinter.setEnabled(false);
        btnVerify.setEnabled(false);
    }

    private void notifyDisabledSolving() {
        spSolvingOption.setEnabled(false);
        etSolvingNote.setEnabled(false);
        btnSolved.setEnabled(false);
        btnHold.setEnabled(false);
    }
}
