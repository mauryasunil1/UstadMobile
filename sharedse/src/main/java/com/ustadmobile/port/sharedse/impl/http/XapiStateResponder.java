package com.ustadmobile.port.sharedse.impl.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ustadmobile.core.contentformats.xapi.Actor;
import com.ustadmobile.core.contentformats.xapi.State;
import com.ustadmobile.core.contentformats.xapi.endpoints.StateEndpoint;
import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.util.UMIOUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

public class XapiStateResponder implements RouterNanoHTTPD.UriResponder {

    public static final int PARAM_APPREPO_INDEX = 0;
    Type contentMapToken = new TypeToken<HashMap<String, Object>>() {
    }.getType();


    @Override
    public NanoHTTPD.Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response put(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        UmAppDatabase repo = uriResource.initParameter(PARAM_APPREPO_INDEX, UmAppDatabase.class);

        byte[] content;
        FileInputStream fin = null;
        ByteArrayOutputStream bout = null;
        String tmpFileName;
        try {

            Map<String, String> map = new HashMap<>();
            session.parseBody(map);
            if (map.containsKey("content")) {
                tmpFileName = map.get("content");
                fin = new FileInputStream(tmpFileName);
                bout = new ByteArrayOutputStream();
                UMIOUtils.readFully(fin, bout);
                bout.flush();
                content = bout.toByteArray();
            } else {
                content = session.getQueryParameterString().getBytes();
            }
            Map<String, List<String>> queryParams = session.getParameters();
            String activityId = queryParams.get("activityId").get(0);
            String agentJson = queryParams.get("agent").get(0);
            String stateId = queryParams.get("stateId").get(0);

            Gson gson = new Gson();
            Actor agent = gson.fromJson(agentJson, Actor.class);
            String contentJson = new String(content);
            HashMap<String, Object> contentMap;
            contentMap = gson.fromJson(contentJson, contentMapToken);

            State state = new State(stateId, agent, activityId, contentMap);
            StateEndpoint endpoint = new StateEndpoint(repo, gson);
            endpoint.storeState(state);

            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NO_CONTENT,
                    "application/octet", null);

        } catch (IOException | NanoHTTPD.ResponseException | NullPointerException e) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                    "application/octet", e.getMessage());
        } finally {
            UMIOUtils.closeQuietly(fin);
            UMIOUtils.closeQuietly(bout);

        }

    }

    @Override
    public NanoHTTPD.Response post(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        UmAppDatabase repo = uriResource.initParameter(PARAM_APPREPO_INDEX, UmAppDatabase.class);

        try {

            Map<String, List<String>> queryParams = session.getParameters();

            if (queryParams.containsKey("method")) {
                String method = queryParams.get("method").get(0);
                if (method.equalsIgnoreCase("put")) {
                    return put(uriResource, urlParams, session);
                } else if (method.equalsIgnoreCase("get")) {
                    return get(uriResource, urlParams, session);
                }
            }

            Map<String, String> map = new HashMap<>();
            session.parseBody(map);
            String stateString = session.getQueryParameterString();
            String activityId = queryParams.get("activityId").get(0);
            String agentJson = queryParams.get("agent").get(0);
            String stateId = queryParams.get("stateId").get(0);

            Gson gson = new Gson();
            Actor agent = gson.fromJson(agentJson, Actor.class);
            HashMap<String, Object> contentMap;
            contentMap = gson.fromJson(stateString, contentMapToken);

            State state = new State(stateId, agent, activityId, contentMap);
            StateEndpoint endpoint = new StateEndpoint(repo, gson);
            endpoint.storeState(state);


            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.NO_CONTENT,
                    "application/octet", null);

        } catch (IOException | NanoHTTPD.ResponseException | NullPointerException e) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                    "application/octet", e.getMessage());
        }
    }

    @Override
    public NanoHTTPD.Response delete(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response other(String method, RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
        return null;
    }
}
