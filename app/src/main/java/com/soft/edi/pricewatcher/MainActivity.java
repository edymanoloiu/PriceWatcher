package com.soft.edi.pricewatcher;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Pair;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    HashMap<String, String> prodAsc = new HashMap<>();
    Context currentContext = this;
    final String fileName = "Settings.txt";
    LinkedList<Product> products = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //read settings data
        readSettingsFile();

        //get json objects from server
        getJsonObjectsFromServer();

        //search text event
        setSearchViewEvents();

        //expandable list
        setExpandableList();

        //load product option from stored file
        loadProductOptions();

        buildNotification("test");
    }

    private void prepareListData(String query) {
        if (listDataHeader == null && listDataChild == null) {
            listDataHeader = new ArrayList<>();
            listDataChild = new HashMap<>();
        }

        if (!query.isEmpty() && !listDataHeader.contains(query)) {

            List<String> result = new ArrayList<>();
            listDataHeader.add(query);
            result.add("Delete item!");

            for (String key : prodAsc.keySet()) {
                if (checkQuery(query.toLowerCase(), key.toLowerCase().toLowerCase()))
                    for (Product prod : products)
                        if (prod.getName().equals(key) && !result.contains(prod.toString()))
                            result.add(prod.toString());
            }

            listDataChild.put(listDataHeader.get(listDataHeader.size() - 1), result);
        }
    }

    //check if the name of the product is the same as the query
    private Boolean checkQuery(String query, String name) {
        Boolean isOK = true;
        String[] queryTerms = query.split(" ");
        for (String term : queryTerms)
            isOK = isOK && name.contains(term);

        return isOK;
    }

    private void setSearchViewEvents() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) this.findViewById(R.id.searchView);
        if (null != searchView) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                return true;
            }

            public boolean onQueryTextSubmit(String query) {
                //on search event

                expListView = (ExpandableListView) findViewById(R.id.productList);
                prepareListData(query);
                listAdapter = new ExpandableListAdapter(currentContext, listDataHeader, listDataChild);
                // setting list adapter
                expListView.setAdapter(listAdapter);

                //save configuration
                writeFileToMemory();

                return true;
            }
        };
        searchView.setOnQueryTextListener(queryTextListener);
    }

    private void setExpandableList() {
        expListView = (ExpandableListView) findViewById(R.id.productList);
        prepareListData("");

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
        expListView.setAdapter(listAdapter);

        // Listview on child click listener
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                if (listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition).equals("Delete item!")) {
                    for (Iterator<HashMap.Entry<String, List<String>>> it = listDataChild.entrySet().iterator(); it.hasNext(); ) {
                        HashMap.Entry<String, List<String>> entry = it.next();
                        if (entry.getKey().equals(listDataHeader.get(groupPosition))) {
                            it.remove();
                        }
                    }
                    listDataHeader.remove(groupPosition);
                    listAdapter = new ExpandableListAdapter(currentContext, listDataHeader, listDataChild);
                    expListView.setAdapter(listAdapter);
                    writeFileToMemory();
                } else {
                    String link = "";
                    String productInfo = listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
                    if (productInfo.startsWith("▼ "))
                        productInfo = productInfo.substring(2);
                    else if (productInfo.startsWith("▲ "))
                        productInfo = productInfo.substring(2);
                    for (Product prod : products)
                        if (prod.toString().equals(productInfo)) {
                            link = prod.getLink();
                            break;
                        }

                    //open browser
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                }
                return false;
            }
        });
    }

    private void readSettingsFile() {
        StringBuilder sb = new StringBuilder();
        FileInputStream fis;
        Boolean fileExists = false;
        try {
            String[] files = currentContext.fileList();
            for (int i = 0; i < files.length; i++)
                if (files[i].contains(fileName)) {
                    fileExists = true;
                    break;
                }

            if (fileExists) {
                fis = currentContext.openFileInput(fileName);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(isr);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                HashMap<String, Object> readHash = jsonToMap(new JSONObject(sb.toString()));

                HashMap<String, List<String>> previousList = new HashMap<>();
                ArrayList<String> headers = new ArrayList<>();
                for (String key : readHash.keySet()) {
                    previousList.put(key, (List<String>) readHash.get(key));
                    headers.add(key);
                }

                listDataChild = previousList;
                listDataHeader = headers;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void writeFileToMemory() {
        if (listDataHeader != null) {
            JSONObject obj = new JSONObject(listDataChild);
            String string = obj.toString();
            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private HashMap<String, Object> jsonToMap(JSONObject json) throws JSONException {
        HashMap<String, Object> retMap = new HashMap<String, Object>();

        if (json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }

    private HashMap<String, Object> toMap(JSONObject object) throws JSONException {
        HashMap<String, Object> map = new HashMap<String, Object>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    private void getJsonObjectsFromServer() {
        try {
            String jsonEMAG = new EndpointsAsyncTask().execute(new Pair<Context, String>(this, "EMAG")).get();
            String jsonCel = new EndpointsAsyncTask().execute(new Pair<Context, String>(this, "Cel")).get();
            String jsonMediaGalaxy = new EndpointsAsyncTask().execute(new Pair<Context, String>(this, "MediaGalaxy")).get();
            String jsonQuickMobile = new EndpointsAsyncTask().execute(new Pair<Context, String>(this, "QuickMobile")).get();
            HashMap<String, Integer> nameAndPos = new HashMap<>();

            int i;
            Product prod;
            JSONObject jsonObject;
            LinkedList<JSONArray> mJsonArrays = new LinkedList<>();
            if (!jsonEMAG.equals("nulltest"))
                mJsonArrays.add(new JSONArray(jsonEMAG));
            if (!jsonCel.equals("nulltest"))
                mJsonArrays.add(new JSONArray(jsonCel));
            if (!jsonMediaGalaxy.equals("nulltest"))
                mJsonArrays.add(new JSONArray(jsonMediaGalaxy));
            if (!jsonQuickMobile.equals("nulltest"))
                mJsonArrays.add(new JSONArray(jsonQuickMobile));
            for (JSONArray mJsonArray : mJsonArrays) {
                for (i = 0; i < mJsonArray.length(); i++) {
                    jsonObject = mJsonArray.getJSONObject(i).getJSONObject("Item1");
                    prod = new Product(jsonObject.getString("name"), jsonObject.getString("link"), jsonObject.getString("price"));
                    products.add(prod);
                    nameAndPos.put(jsonObject.getString("name"), i);
                }
            }

            String oldPriceString, newPriceString;
            int newPrice, oldPrice;
            if (listDataChild != null) {
                ArrayList<String> list;
                for (String key : listDataChild.keySet()) {
                    list = new ArrayList<>();
                    for (String name : listDataChild.get(key)) {
                        if (nameAndPos.keySet().contains(name.split("-")[0].trim())) {
                            //get old price
                            oldPriceString = name.split("-")[1].trim();
                            oldPriceString = oldPriceString.substring(0, oldPriceString.indexOf("l")).trim();
                            if (oldPriceString.contains("."))
                                oldPrice = (int) (Float.parseFloat(oldPriceString) * 1000);
                            else
                                oldPrice = Integer.parseInt(oldPriceString);

                            //get new price
                            newPriceString = products.get(nameAndPos.get(name.split("-")[0].trim())).getPrice();
                            newPriceString = newPriceString.substring(0, newPriceString.indexOf("l")).trim();
                            if (newPriceString.contains("."))
                                newPrice = (int) (Float.parseFloat(newPriceString) * 1000);
                            else
                                newPrice = Integer.parseInt(newPriceString);

                            if (newPrice > oldPrice)
                                list.add("\u25B2 " + products.get(nameAndPos.get(name.split("-")[0].trim())).toString());
                            else
                                list.add("\u25BC " + products.get(nameAndPos.get(name.split("-")[0].trim())).toString());
                        } else
                            list.add(name);
                    }

                    listDataChild.put(key, list);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadProductOptions() {
        try {
            //read content from asset file
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(getAssets().open("AllSites.json")));

            String mLine, fileContent = "";
            while ((mLine = reader.readLine()) != null) {
                fileContent += mLine;
            }

            //create local list with products
            int i;
            Product prod;
            JSONObject jsonObject;
            JSONArray mJsonArray = new JSONArray(fileContent);
            for (i = 0; i < mJsonArray.length(); i++) {
                jsonObject = mJsonArray.getJSONObject(i);
                prod = new Product(jsonObject.getString("name"), jsonObject.getString("link"), jsonObject.getString("price"));
                if (!products.contains(prod))
                    products.add(prod);
                if (!prodAsc.keySet().contains(prod.getName())) {
                    if (prod.getLink().toLowerCase().contains("emag"))
                        prodAsc.put(prod.getName(), "EMAG");
                    else if (prod.getLink().toLowerCase().contains("mediagalaxy"))
                        prodAsc.put(prod.getName(), "MediaGalaxy");
                    else if (prod.getLink().toLowerCase().contains("cel"))
                        prodAsc.put(prod.getName(), "Cel");
                    else if (prod.getLink().toLowerCase().contains("quickmobile"))
                        prodAsc.put(prod.getName(), "QuickMobile");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void buildNotification(String notificationMsg) {
        NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this).setSmallIcon(R.drawable.pic).setContentTitle("Updated prices available!").setContentText(notificationMsg);
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        // Sets an ID for the notification
        int mNotificationId = 001;
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }
}
