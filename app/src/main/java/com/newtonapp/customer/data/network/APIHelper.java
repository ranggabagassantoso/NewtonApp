package com.newtonapp.customer.data.network;

import com.newtonapp.customer.data.network.pojo.request.ComplainRequestModel;
import com.newtonapp.customer.data.network.pojo.response.ComplainResponseModel;

import io.reactivex.Observable;

public class APIHelper {

    /**
     * name : send complain
     * type : post
     * */
    public static Observable<ComplainResponseModel> sendComplain(ComplainRequestModel complainBody) {
        return APIConfig.createService(APIClient.class).sendComplain(complainBody);
    }
}
