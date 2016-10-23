/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.example.Edi.myapplication.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import javax.inject.Named;

/**
 * An endpoint class we are exposing
 */
@Api(
        name = "myApi",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "backend.myapplication.Edi.example.com",
                ownerName = "backend.myapplication.Edi.example.com",
                packagePath = ""
        )
)
public class MyEndpoint {

    /**
     * A simple endpoint method that takes a name and says Hi back
     */
    @ApiMethod(name = "getJson")
    public MyBean GetJson(@Named("site") String site) {

        MobileData mobileData = new MobileData();
        MyBean response = new MyBean();
        
        if (site.equals("EMAG"))
            response.setData(mobileData.getEMAG());
        else if (site.equals("Cel"))
            response.setData(mobileData.getCel());
        else if (site.equals("MediaGalaxy"))
            response.setData(mobileData.getMediaGalaxy());
        else if (site.equals("QuickMobile"))
            response.setData(mobileData.getQuickMobile());

        return response;
    }

}
