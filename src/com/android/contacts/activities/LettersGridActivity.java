/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.activities;

import android.app.Activity;
import com.android.contacts.R;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.content.Context;
import android.view.ViewGroup;
import android.content.Intent;

 class MyAdapter extends BaseAdapter {

    private Context context;
    private String l;
    static final String[] numbers = new String[] { 
			"A", "B", "C", "D", "E",
			"F", "G", "H", "I", "J",
			"K", "L", "M", "N", "O",
			"P", "Q", "R", "S", "T",
			"U", "V", "W", "X", "Y", "Z"};

    public MyAdapter(Context context,String letters) {
        this.context = context;
        l=letters;
    }

    public int getCount() {
        return l.length();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
	TextView tv;
            if(convertView==null){
                tv=new TextView(context);
                tv.setLayoutParams(new GridView.LayoutParams(80,80));
            }else{
                tv=(TextView)convertView;
            }
            tv.setTextSize(32);
            tv.setText(l.substring(position,position+1));
            tv.setGravity(0x11);
        return tv;
    }
}

public class LettersGridActivity extends Activity {
 
	GridView gridView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String letters= getIntent().getStringExtra("letters");
		
		setContentView(R.layout.contact_letters_grid);
 
		gridView = (GridView) findViewById(R.id.LettersGridView);

		gridView.setAdapter(new MyAdapter(this,letters));
 
		gridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
				int position, long id) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra("result", Integer.toString(position));
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		});
 
	}
}

