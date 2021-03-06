package com.newtonapp.utility;

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork;
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.InternetObservingSettings;
import com.google.gson.JsonSyntaxException;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.newtonapp.BuildConfig;
import com.newtonapp.data.network.BaseAPIConfig;

import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class NetworkUtil {

    public static Disposable checkInternetConnection(Consumer<Boolean> connectionSubscriber) {
        InternetObservingSettings internetObservingSettings = InternetObservingSettings.builder()
                .host(BaseAPIConfig.API_BASE_URL)
                .build();

        return ReactiveNetwork
                .observeInternetConnectivity()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectionSubscriber);
    }

    public static String handleApiError(Throwable error) {

        final int API_STATUS_CODE_LOCAL_ERROR = 0;
        String errorMessage;
        if (error instanceof HttpException) {
            switch (((HttpException) error).code()) {
                case HttpsURLConnection.HTTP_UNAUTHORIZED:
                    errorMessage = "Unauthorized User";
                    break;
                case HttpsURLConnection.HTTP_FORBIDDEN:
                    errorMessage = "Forbidden (Code: 404)";
                    break;
                case HttpsURLConnection.HTTP_INTERNAL_ERROR:
                    errorMessage = "Server Problem (Code: 500)";
                    break;
                case HttpsURLConnection.HTTP_BAD_REQUEST:
                    errorMessage = "Bad Request (Code: 400)";
                    break;
                case API_STATUS_CODE_LOCAL_ERROR:
                    errorMessage = "API status code local error (Code: 0)";
                    break;
                default:
                    errorMessage = error.getLocalizedMessage();
            }
        } else if (error instanceof JsonSyntaxException) {
            errorMessage = "Something Went Wrong API is not responding properly! (Code: 666)";
        } else if (error instanceof UnknownHostException) {
            errorMessage = "Slow or no internet connection";
        } else{
            errorMessage = error.getMessage();
        }

        DebugUtil.e("ERROR: " + errorMessage, error);
        if (!BuildConfig.DEBUG) errorMessage = "Couldn't retrieve data";
        return errorMessage;
    }


}
