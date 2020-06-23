/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse.starter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {

  public void redirectActivity(){
    if (ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")){
      Intent intent = new Intent(getApplicationContext(), RiderActivity.class);
      startActivity(intent);
    } else {
      Intent intent = new Intent(getApplicationContext(), ViewRequestsActivity.class);
      startActivity(intent);
    }
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public void getStarted(View view){
    Switch userType = (Switch) findViewById(R.id.userType);
    changeState(view);
    String userT = "rider";
    if (userType.isChecked()){
      userT = "rider";
    } else {
      userT = "driver";
    }
    ParseUser.getCurrentUser().put("riderOrDriver", userT);
      ParseUser.getCurrentUser().saveInBackground(new SaveCallback(){
        @Override
        public void done(ParseException e){
          redirectActivity();
        }
      });
  }
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public void changeState(View view){
    Switch userType = (Switch) findViewById(R.id.userType);
    if (userType.isChecked()){
      TextView t = (TextView) findViewById(R.id.switchState);
      t.setText("Rider");
    } else {
      TextView t = (TextView) findViewById(R.id.switchState);
      t.setText("Driver");
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
      getSupportActionBar().hide();
      if (ParseUser.getCurrentUser() == null){
        ParseAnonymousUtils.logIn(new LogInCallback() {
          @Override
          public void done(ParseUser user, ParseException e) {
            if (e == null){
              Log.i("Info", "Anonymous login succesful");

            }else {
              Log.i("Info", "Anonymous login failed");
            }
          }
        });
      } else {
        if (ParseUser.getCurrentUser().get("riderOrDriver") != null){
          Log.i("Info", "Redirecting as " + ParseUser.getCurrentUser().get("riderOrDriver"));
          redirectActivity();
        }
      }
    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }

}