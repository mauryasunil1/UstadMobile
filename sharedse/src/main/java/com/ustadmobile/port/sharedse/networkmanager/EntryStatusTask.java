package com.ustadmobile.port.sharedse.networkmanager;

import com.ustadmobile.core.util.UMIOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.ustadmobile.port.sharedse.networkmanager.BluetoothServer.CMD_SEPARATOR;

/**
 * Created by kileha3 on 09/05/2017.
 */

public class EntryStatusTask extends NetworkTask implements BluetoothConnectionHandler{

    private List<String> entryIdList;
    private List<NetworkNode> networkNodeList;

    private int currentNode;

    public EntryStatusTask(List<String> entryIdList, List<NetworkNode> networkNodeList, NetworkManager networkManager){
        super(networkManager);
        this.entryIdList = entryIdList;
        this.networkNodeList = networkNodeList;
    }
    @Override
    public void start() {
        currentNode = 0;
        new Thread(new Runnable() {
            public void run() {
                if(isUseBluetooth()){
                    connectNextBluetoothNode();
                }

                if(isUseHttp()){
                    connectNextHttpNode();
                }
            }
        }).start();
    }

    private void connectNextBluetoothNode() {
        if(currentNode < networkNodeList.size()) {
            String bluetoothAddr = networkNodeList.get(currentNode).getDeviceBluetoothMacAddress();
            networkManager.connectBluetooth(bluetoothAddr, this);
        }else {
            networkManager.handleTaskCompleted(this);
        }

    }

    private void connectNextHttpNode(){
        //TODO: handle all http status check
    }


    @Override
    public void cancel() {

    }

    @Override
    public void onConnected(InputStream inputStream, OutputStream outputStream) {
        String queryStr = BluetoothServer.CMD_ENTRY_STATUS_QUERY + ' ';
        List<Boolean> entryIdStatusList=new ArrayList<>();
        for(int i = 0; i < entryIdList.size(); i++){
            try { queryStr += URLEncoder.encode(entryIdList.get(i), "UTF-8"); }
            catch(UnsupportedEncodingException ignored) {}//what device doesn't have UTF-8?

            if(i < entryIdList.size() - 1)
                queryStr += CMD_SEPARATOR;
        }

        queryStr += '\n';
        String response=null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            outputStream.write(queryStr.getBytes());
            response = reader.readLine();
            if(response.startsWith(BluetoothServer.CMD_ENTRY_STATUS_FEEDBACK)) {
                response=response.substring((BluetoothServer.CMD_ENTRY_STATUS_FEEDBACK.length()+1),response.length());

                for(String status: response.split(CMD_SEPARATOR)){
                    boolean responseStatus= status.equals("1");
                    entryIdStatusList.add(responseStatus);
                }

                networkManager.handleEntriesStatusUpdate(networkNodeList.get(currentNode), entryIdList,entryIdStatusList);
                managerTaskListener.handleTaskCompleted(this);
                currentNode++;

            }else {
                System.out.print("Feedback "+response);
            }
        }catch(IOException e) {
            e.printStackTrace();
        }finally {
            if(response!=null){
                UMIOUtils.closeInputStream(inputStream);
                UMIOUtils.closeOutputStream(outputStream);
                networkManager.disconnectBluetooth();
            }
        }
    }

    @Override
    public int getQueueId() {
        return NetworkManager.QUEUE_ENTRY_STATUS;
    }

    @Override
    public int getTaskId() {
        return 0;
    }

    @Override
    public int getTaskType() {
        return 0;
    }

}
