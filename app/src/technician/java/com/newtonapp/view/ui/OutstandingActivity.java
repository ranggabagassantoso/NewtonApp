package com.newtonapp.view.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.newtonapp.R;
import com.newtonapp.data.database.entity.Customer;
import com.newtonapp.data.network.APIHelper;
import com.newtonapp.data.network.pojo.request.OutstandingRequestModel;
import com.newtonapp.data.network.pojo.request.TakeJobRequestModel;
import com.newtonapp.data.network.pojo.request.TrackingRequestModel;
import com.newtonapp.data.network.pojo.response.OutstandingResponseModel;
import com.newtonapp.data.network.pojo.response.TrackingResponseModel;
import com.newtonapp.model.rvmodel.OutstandingRvModelNew;
import com.newtonapp.utility.CommonUtil;
import com.newtonapp.utility.Constants;
import com.newtonapp.utility.DebugUtil;
import com.newtonapp.utility.NetworkUtil;
import com.newtonapp.utility.PermissionUtil;
import com.newtonapp.view.adapter.rvadapter.OutstandingRvAdapter;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;

public class OutstandingActivity extends BaseActivity {

    private static final String TAG = OutstandingActivity.class.getSimpleName();
    private Toolbar toolbar;
    private AppCompatImageView ivFailedLogo;
    private AppCompatTextView tvFailedMessage;
    private LinearLayout llFailedBody;
    private RecyclerView rvOutstandingList;
    private OutstandingRvAdapter outstandingRvAdapter;
    private ArrayList<OutstandingRvModelNew> outstandingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outstanding);
        initView();
        prepareRecyclerView();
    }

    @Override
    public Activity onCreateGetCurrentActivity() {
        return this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOnGoingProblemAvailbility();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void online() {
        showNormalMode();
        if (outstandingList.isEmpty()) { // re-download outstanding list if currently empty
            checkOnGoingProblemAvailbility();
        }
    }

    @Override
    public void offline() {
        showNoInternetConnectionView();
    }

    private void initView() {
        toolbar = findViewById(R.id.header_layout_toolbar);
        llFailedBody = findViewById(R.id.failed_layout_body);
        ivFailedLogo = findViewById(R.id.body_iv_failed_logo);
        tvFailedMessage = findViewById(R.id.body_tv_failed_text);
        rvOutstandingList = findViewById(R.id.outstanding_rv_tasklist);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.screen_outstanding_list);
    }

    private void prepareRecyclerView() {
        outstandingList = new ArrayList<>();
        outstandingRvAdapter = new OutstandingRvAdapter(this, outstandingList);
        outstandingRvAdapter.setOnItemClickListener(this::takingJobConfirmation);
        outstandingRvAdapter.setOnCallPicListener(phoneNumber -> CommonUtil.dial(this, phoneNumber));
        rvOutstandingList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        rvOutstandingList.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        rvOutstandingList.setItemAnimator(new DefaultItemAnimator());
        rvOutstandingList.setAdapter(outstandingRvAdapter);
    }

    private void checkOnGoingProblemAvailbility() {
        showMessageDialog(getString(R.string.progress_getting_latest_tracking));
        TrackingRequestModel formBody = new TrackingRequestModel();
        formBody.setToken(loginToken.toString());
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

                                    if (response.getStatus() == 1) {
                                        onSuccessTracking(response);
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

    private void onSuccessTracking(TrackingResponseModel response) {
        if (response.getData() != null) { // jika ada ongoing problem, tidak bisa mengambil job
            outstandingRvAdapter.setOnItemClickListener(data -> showUnableTakingOutstandingJob());
            rvOutstandingList.setAdapter(outstandingRvAdapter);
        }
        downloadOutstandingJoblist();
    }

    private void takingJobConfirmation(OutstandingRvModelNew outstanding) {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi pengambilan tugas")
                .setMessage("Lanjutkan pengambilan tugas untuk customer " + outstanding.getCustomer().getIdCust() + " ?")
                .setPositiveButton(getString(android.R.string.yes), (dialogInterface, i) -> {
                    // confirm assignment
                    takingOutstandingJob(outstanding);
                })
                .setNegativeButton(getString(android.R.string.cancel), (dialogInterface, i) -> { })
                .show();
    }

    private void showUnableTakingOutstandingJob() {
        Toast.makeText(this, getString(R.string.error_unable_taking_job), Toast.LENGTH_LONG).show();
    }

    private void downloadOutstandingJoblist() {
        OutstandingRequestModel formBody = new OutstandingRequestModel();
        formBody.setToken(loginToken.toString());
        compositeDisposable.add(
                APIHelper.getOutstandingJoblist(formBody)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                response -> {
                                    hideDialog();
                                    if (response == null) {
                                        throw new NullPointerException(getString(R.string.error_null_response));
                                    }

                                    if (response.getStatus() == 1) {
                                        onSuccessDownloadOutstandinglist(response);
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

    private void onSuccessDownloadOutstandinglist(OutstandingResponseModel response) {
        if (response.getData() != null) { // data outstanding list
            populateDataFromNetwork(response.getData());
        } else showNoItemView();
    }

    private void populateDataFromNetwork(List<Customer> data) {
        showNormalMode();
        outstandingList.clear();
        for (Customer item : data) {
            outstandingList.add(new OutstandingRvModelNew(item));
        }
        outstandingRvAdapter.setData(outstandingList);
    }

    private void takingOutstandingJob(OutstandingRvModelNew outstanding) {
        String idProblem = outstanding.getCustomer().getProblems().get(0).getIdProblem();
        DebugUtil.d("taking job with idprob: " + idProblem);
        showMessageDialog(getString(R.string.progress_taking_outstanding_job));
        TakeJobRequestModel formBody = new TakeJobRequestModel();
        formBody.setProb(idProblem);
        formBody.setToken(loginToken.toString());
        compositeDisposable.add(
                APIHelper.takingOutstandingJob(formBody)
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
                                        onSuccessTakingOutstandingJob(outstanding.getCustomer());
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

    private void onSuccessTakingOutstandingJob(Customer customer) {
        /** deprecated line
        customer.getProblems().get(0).setStatusComplain(Constants.FLAG_START_PROGRESS);
        setOngoingCustomerProblem(customer); **/
        navigateTo(this, SolvingActivity.class);
        finish();
    }

    private void setNormalMode() {
        llFailedBody.setVisibility(View.GONE);
        rvOutstandingList.setVisibility(View.VISIBLE);

    }

    private void setFailedMode() {
        llFailedBody.setVisibility(View.VISIBLE);
        rvOutstandingList.setVisibility(View.VISIBLE);
    }

    private void showNormalMode() {
        setNormalMode();
        outstandingRvAdapter.setOnItemClickListener(this::takingJobConfirmation);
        rvOutstandingList.setAdapter(outstandingRvAdapter);
    }

    private void showNoItemView() {
        setFailedMode();
        ivFailedLogo.setImageDrawable(getResources().getDrawable(R.drawable.ic_empty));
        tvFailedMessage.setText(getString(R.string.success_message_empty_data_item));
    }

    private void showNoInternetConnectionView() {
        setFailedMode();
        ivFailedLogo.setImageDrawable(getResources().getDrawable(R.drawable.ic_network_unavailable));
        tvFailedMessage.setText(getString(R.string.error_no_internet_connection));
        outstandingRvAdapter.setOnItemClickListener(null);
        rvOutstandingList.setAdapter(outstandingRvAdapter);
    }

    @AfterPermissionGranted(Constants.RC_READ_PHONE_STATE)
    private void requestDialPhone() {
        if (PermissionUtil.hasPermission(this, PermissionUtil.READ_PHONE_STATE_PERMISSION)) {

        } else {
            PermissionUtil.requestPermission(this, getString(R.string.warning_message_rationale_read_phone_state), Constants.RC_READ_PHONE_STATE, PermissionUtil.READ_PHONE_STATE_PERMISSION);
        }
    }


}