package sg.lifecare.medicare.ui;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by wanping on 4/7/16.
 */
public class CheckDeviceActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.chart_view);

    }


   /* private class checkGotDevice extends AsyncTask<Void, Void, Void>
    {
        private boolean gotDevices = false;

        @Override
        protected void onPreExecute()
        {
            *//*
            loadingView.setVisibility(View.VISIBLE);
            loading.setVisibility(View.VISIBLE);
            notAvailable.setVisibility(View.GONE);*//*
        }

        @Override
        protected Void doInBackground(Void... paramVarArgs)
        {
            String entityId = "0";
            String gatewayId = LifeCareHandler.getInstance()
                    .getMyDeviceID(CheckDeviceActivity.this);

            JSONArray result = LifeCareHandler.getInstance()
                    .getConnectedDeviceList(entityId, gatewayId);
            if(result != null && result.length() > 0)
            {
                gotDevices = true;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void imageUrl)
        {
            if(gotDevices == true)
            {
                Intent intent = new Intent(CheckDeviceActivity.this,
                        MeasurementDeviceListActivity.class);
                startActivity(intent);
            }
            else
            {
                checkIsGateway task = new checkIsGateway();
                task.execute();
            }
        }
    }*/
}