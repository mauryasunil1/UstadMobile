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

    private static final int MAXIMUM_CONNECTION_RETRY_COUNT=1;

    int connectionRetryCount=0;

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
                connectNextNode(0);
            }
        }).start();
    }

    private void connectNextNode(int index) {
        if(index < networkNodeList.size()) {
            currentNode = index;
            if(isUseBluetooth() && networkNodeList.get(currentNode).getDeviceBluetoothMacAddress() != null) {
                String bluetoothAddr = networkNodeList.get(currentNode).getDeviceBluetoothMacAddress();
                networkManager.connectBluetooth(bluetoothAddr, this);
            }else if(isUseHttp() && networkNodeList.get(currentNode).getDeviceIpAddress() != null) {
                //TODO: Handle status acquisition over http
                connectNextNode(index+1);
            }else {
                //skip it - not possible to query this node with the current setup
                connectNextNode(index+1);
            }
        }else {
            networkManager.handleTaskCompleted(this);
        }
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
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            outputStream.write(queryStr.getBytes());
            response = reader.readLine();
            if(response.startsWith(BluetoothServer.CMD_ENTRY_STATUS_FEEDBACK)) {
                response=response.substring((BluetoothServer.CMD_ENTRY_STATUS_FEEDBACK.length()+1),response.length());

                for(String status: response.split(CMD_SEPARATOR)){
                    boolean responseStatus= status.equals("1");
                    entryIdStatusList.add(responseStatus);
                }

                networkManager.handleEntriesStatusUpdate(networkNodeList.get(currentNode), entryIdList,entryIdStatusList);
            }else {
                System.out.print("Feedback "+response);
            }
        }catch(IOException e) {
            e.printStackTrace();
        }finally {
            if(reader != null){
                try {reader.close();}
                catch(IOException e){}
            }
            UMIOUtils.closeInputStream(inputStream);
            UMIOUtils.closeOutputStream(outputStream);
            networkManager.disconnectBluetooth();
        }

        connectNextNode(currentNode+1);
    }

    @Override
    public void onConnectionFailed(String bluetoothAddress) {

        if(networkNodeList.get(currentNode).getDeviceBluetoothMacAddress().equals(bluetoothAddress)
                && connectionRetryCount < MAXIMUM_CONNECTION_RETRY_COUNT){
            connectionRetryCount++;
            try{
                Thread.sleep(NetworkManager.WAITING_TIME_BEFORE_RETRY);
            } catch (InterruptedException e) {}
            connectNextNode(currentNode);
        }else{
            connectionRetryCount=0;
            connectNextNode(currentNode+1);
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
