/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.contacts.group;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.interactions.GroupDeletionDialogFragment;
import com.android.contacts.list.ContactTileAdapter;
import com.android.contacts.list.ContactTileAdapter.DisplayType;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.TextView;
import android.media.RingtoneManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

/**
 * Displays the details of a group and shows a list of actions possible for the group.
 */
public class GroupDetailFragment extends Fragment implements OnScrollListener {

    public static interface Listener {
        /**
         * The group title has been loaded
         */
        public void onGroupTitleUpdated(String title);

        /**
         * The number of group members has been determined
         */
        public void onGroupSizeUpdated(String size);

        /**
         * The account type and dataset have been determined.
         */
        public void onAccountTypeUpdated(String accountTypeString, String dataSet);

        /**
         * User decided to go to Edit-Mode
         */
        public void onEditRequested(Uri groupUri);

        /**
         * Contact is selected and should launch details page
         */
        public void onContactSelected(Uri contactUri);
    }

    private static final String TAG = "GroupDetailFragment";
    private static final String GROUP_RINGTONE_FILENAME = "group_ringtone_file.txt";

    private static final int LOADER_METADATA = 0;
    private static final int LOADER_MEMBERS = 1;
    private static final int REQUEST_CODE_PICK_RINGTONE = 1;

    private Context mContext;

    private View mRootView;
    private ViewGroup mGroupSourceViewContainer;
    private View mGroupSourceView;
    private TextView mGroupTitle;
    private TextView mGroupSize;
    private ListView mMemberListView;
    private View mEmptyView;

    private Listener mListener;

    private ContactTileAdapter mAdapter;
    private ContactPhotoManager mPhotoManager;
    private AccountTypeManager mAccountTypeManager;

    private Uri mGroupUri;
    private long mGroupId;
    private String mGroupName;
    private String mAccountTypeString;
    private String mDataSet;
    private boolean mIsReadOnly;

    private boolean mShowGroupActionInActionBar;
    private boolean mOptionsMenuGroupDeletable;
    private boolean mOptionsMenuGroupPresent;
    private boolean mCloseActivityAfterDelete;
    private String mCustomRingtone;

    public GroupDetailFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mAccountTypeManager = AccountTypeManager.getInstance(mContext);

        Resources res = getResources();
        int columnCount = res.getInteger(R.integer.contact_tile_column_count);

        mAdapter = new ContactTileAdapter(activity, mContactTileListener, columnCount,
                DisplayType.GROUP_MEMBERS);

        configurePhotoLoader();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        setHasOptionsMenu(true);
        mRootView = inflater.inflate(R.layout.group_detail_fragment, container, false);
        mGroupTitle = (TextView) mRootView.findViewById(R.id.group_title);
        mGroupSize = (TextView) mRootView.findViewById(R.id.group_size);
        mGroupSourceViewContainer = (ViewGroup) mRootView.findViewById(
                R.id.group_source_view_container);
        mEmptyView = mRootView.findViewById(android.R.id.empty);
        mMemberListView = (ListView) mRootView.findViewById(android.R.id.list);
        mMemberListView.setAdapter(mAdapter);

        return mRootView;
    }

    public void loadGroup(Uri groupUri) {
        mGroupUri= groupUri;
        startGroupMetadataLoader();
    }

    public void setQuickContact(boolean enableQuickContact) {
        mAdapter.enableQuickContact(enableQuickContact);
    }

    private void configurePhotoLoader() {
        if (mContext != null) {
            if (mPhotoManager == null) {
                mPhotoManager = ContactPhotoManager.getInstance(mContext);
            }
            if (mMemberListView != null) {
                mMemberListView.setOnScrollListener(this);
            }
            if (mAdapter != null) {
                mAdapter.setPhotoLoader(mPhotoManager);
            }
        }
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    public void setShowGroupSourceInActionBar(boolean show) {
        mShowGroupActionInActionBar = show;
    }

    /**
     * Start the loader to retrieve the metadata for this group.
     */
    private void startGroupMetadataLoader() {
        getLoaderManager().restartLoader(LOADER_METADATA, null, mGroupMetadataLoaderListener);
    }

    /**
     * Start the loader to retrieve the list of group members.
     */
    private void startGroupMembersLoader() {
        getLoaderManager().restartLoader(LOADER_MEMBERS, null, mGroupMemberListLoaderListener);
    }

    private final ContactTileAdapter.Listener mContactTileListener =
            new ContactTileAdapter.Listener() {

        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            mListener.onContactSelected(contactUri);
        }
    };

    /**
     * The listener for the group metadata loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetadataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new GroupMetaDataLoader(mContext, mGroupUri);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            data.moveToPosition(-1);
            if (data.moveToNext()) {
                boolean deleted = data.getInt(GroupMetaDataLoader.DELETED) == 1;
                if (!deleted) {
                    bindGroupMetaData(data);

                    // Retrieve the list of members
                    startGroupMembersLoader();
                    return;
                }
            }
            updateSize(-1);
            updateTitle(null);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The listener for the group members list loader
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return GroupMemberLoader.constructLoaderForGroupDetailQuery(mContext, mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            updateSize(data.getCount());
            mAdapter.setContactCursor(data);
            mMemberListView.setEmptyView(mEmptyView);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    private void bindGroupMetaData(Cursor cursor) {
        cursor.moveToPosition(-1);
        if (cursor.moveToNext()) {
            mAccountTypeString = cursor.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            mDataSet = cursor.getString(GroupMetaDataLoader.DATA_SET);
            mGroupId = cursor.getLong(GroupMetaDataLoader.GROUP_ID);
            mGroupName = cursor.getString(GroupMetaDataLoader.TITLE);
            mIsReadOnly = cursor.getInt(GroupMetaDataLoader.IS_READ_ONLY) == 1;
            mCustomRingtone = getGroupCustomRingtone(mGroupId);
            updateTitle(mGroupName);
            // Must call invalidate so that the option menu will get updated
            getActivity().invalidateOptionsMenu ();

            final String accountTypeString = cursor.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            final String dataSet = cursor.getString(GroupMetaDataLoader.DATA_SET);
            updateAccountType(accountTypeString, dataSet);
        }
    }

    private void updateTitle(String title) {
        if (mGroupTitle != null) {
            mGroupTitle.setText(title);
        } else {
            mListener.onGroupTitleUpdated(title);
        }
    }

    /**
     * Display the count of the number of group members.
     * @param size of the group (can be -1 if no size could be determined)
     */
    private void updateSize(int size) {
        String groupSizeString;
        if (size == -1) {
            groupSizeString = null;
        } else {
            String groupSizeTemplateString = getResources().getQuantityString(
                    R.plurals.num_contacts_in_group, size);
            AccountType accountType = mAccountTypeManager.getAccountType(mAccountTypeString,
                    mDataSet);
            groupSizeString = String.format(groupSizeTemplateString, size,
                    accountType.getDisplayLabel(mContext));
        }

        if (mGroupSize != null) {
            mGroupSize.setText(groupSizeString);
        } else {
            mListener.onGroupSizeUpdated(groupSizeString);
        }
    }

    /**
     * Once the account type, group source action, and group source URI have been determined
     * (based on the result from the {@link Loader}), then we can display this to the user in 1 of
     * 2 ways depending on screen size and orientation: either as a button in the action bar or as
     * a button in a static header on the page.
     */
    private void updateAccountType(final String accountTypeString, final String dataSet) {

        // If the group action should be shown in the action bar, then pass the data to the
        // listener who will take care of setting up the view and click listener. There is nothing
        // else to be done by this {@link Fragment}.
        if (mShowGroupActionInActionBar) {
            mListener.onAccountTypeUpdated(accountTypeString, dataSet);
            return;
        }

        final AccountTypeManager manager = AccountTypeManager.getInstance(getActivity());
        final AccountType accountType =
                manager.getAccountType(accountTypeString, dataSet);

        // Otherwise, if the {@link Fragment} needs to create and setup the button, then first
        // verify that there is a valid action.
        if (!TextUtils.isEmpty(accountType.getViewGroupActivity())) {
            if (mGroupSourceView == null) {
                mGroupSourceView = GroupDetailDisplayUtils.getNewGroupSourceView(mContext);
                // Figure out how to add the view to the fragment.
                // If there is a static header with a container for the group source view, insert
                // the view there.
                if (mGroupSourceViewContainer != null) {
                    mGroupSourceViewContainer.addView(mGroupSourceView);
                }
            }

            // Rebind the data since this action can change if the loader returns updated data
            mGroupSourceView.setVisibility(View.VISIBLE);
            GroupDetailDisplayUtils.bindGroupSourceView(mContext, mGroupSourceView,
                    accountTypeString, dataSet);
            mGroupSourceView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Uri uri = ContentUris.withAppendedId(Groups.CONTENT_URI, mGroupId);
                    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setClassName(accountType.resPackageName,
                            accountType.getViewGroupActivity());
                    startActivity(intent);
                }
            });
        } else if (mGroupSourceView != null) {
            mGroupSourceView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mPhotoManager.pause();
        } else {
            mPhotoManager.resume();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.view_group, menu);
    }

    public boolean isOptionsMenuChanged() {
        return mOptionsMenuGroupDeletable != isGroupDeletable() &&
                mOptionsMenuGroupPresent != isGroupPresent();
    }

    public boolean isGroupDeletable() {
        return mGroupUri != null && !mIsReadOnly;
    }

    public boolean isGroupPresent() {
        return mGroupUri != null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mOptionsMenuGroupDeletable = isGroupDeletable() && isVisible();
        mOptionsMenuGroupPresent = isGroupPresent() && isVisible();

        final MenuItem editMenu = menu.findItem(R.id.menu_edit_group);
        editMenu.setVisible(mOptionsMenuGroupPresent);

        final MenuItem deleteMenu = menu.findItem(R.id.menu_delete_group);
        deleteMenu.setVisible(mOptionsMenuGroupDeletable);

	final MenuItem optionsRingtone = menu.findItem(R.id.menu_set_ringtone);
        if (optionsRingtone != null) {
            optionsRingtone.setVisible(mOptionsMenuGroupPresent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit_group: {
                if (mListener != null) mListener.onEditRequested(mGroupUri);
                break;
            }
            case R.id.menu_delete_group: {
                GroupDeletionDialogFragment.show(getFragmentManager(), mGroupId, mGroupName,
                        mCloseActivityAfterDelete);
                return true;
            }
	    case R.id.menu_set_ringtone: {
                doPickRingtone();
                return true;
            }
        }
        return false;
    }

    public void closeActivityAfterDelete(boolean closeActivity) {
        mCloseActivityAfterDelete = closeActivity;
    }

    public long getGroupId() {
        return mGroupId;
    }

    private void doPickRingtone() {

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        // Allow user to pick 'Default'
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        // Show only ringtones
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        // Don't show 'Silent'
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);

        Uri ringtoneUri;
        if (mCustomRingtone != null) {
            ringtoneUri = Uri.parse(mCustomRingtone);
        } else {
            // Otherwise pick default ringtone Uri so that something is selected.
            ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        // Put checkmark next to the current ringtone for this contact
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);

        // Launch!
        startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_PICK_RINGTONE: {
                Uri pickedUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                handleRingtonePicked(pickedUri);
                break;
            }
        }
    }

    private void handleRingtonePicked(Uri pickedUri) {
        if (pickedUri == null || RingtoneManager.isDefault(pickedUri)) {
            mCustomRingtone = null;
        } else {
            mCustomRingtone = pickedUri.toString();
        }
        // write file with custom ringtones
        setGroupCustomRingtone(mGroupId);
    }

    public String getGroupCustomRingtone(long group_id) {
	// TODO read file and return custom ringtone for this group
	try
	{
		FileInputStream fis = mContext.openFileInput(GROUP_RINGTONE_FILENAME);
		StringBuffer fileContent = new StringBuffer("");

		byte[] buffer = new byte[1024];
		int length;
		while ((length = fis.read(buffer)) != -1) {
		    fileContent.append(new String(buffer).replace("\n","#"));
		}
		fis.close();
		if(group_id==-99) return fileContent.toString().trim();
		else
		{
		    String splitted[]=fileContent.toString().trim().split("#");
		    String Ringtone="";
		    for(int i=0; i<splitted.length; i++)
		    {
		        String vals[]=splitted[i].split("-");
    		    if(vals[0].compareTo(String.format("GROUP_%d",group_id))==0) {Ringtone=vals[1];break;}
		    }
		    return Ringtone;
		}
	}
	catch (FileNotFoundException e) {
        return "";

	    } catch (IOException e) {
		return "";

	    }
    }
    
    public void setGroupCustomRingtone(long group_id) {
    // TODO read file and return custom ringtone for this group
        String File_Cont=getGroupCustomRingtone(-99);
        String Curr_Tone=String.format("GROUP_%d-%s#", group_id,mCustomRingtone);
        StringBuffer file=new StringBuffer("");
        if(File_Cont!="")
        {
            String splitted[]=File_Cont.toString().trim().split("#");
            StringBuffer outbuffer=new StringBuffer("");
            for(int i=0; i<splitted.length; i++)
            {
                String vals[]=splitted[i].split("-");
                if(vals[0].compareTo(String.format("GROUP_%d",group_id))==0)
                {
                    splitted[i]="";
                    if(mCustomRingtone!=null) splitted[i]=Curr_Tone.substring(0,Curr_Tone.length()-1);
                    Curr_Tone="";
                }
                outbuffer.append(splitted[i]);
                if(splitted[i].isEmpty()==false) outbuffer.append("#");
            }
            File_Cont="";
            File_Cont=outbuffer.toString();
        }
        file.append(File_Cont);
        file.append(Curr_Tone);
    try
    {
        FileOutputStream fos = mContext.openFileOutput(GROUP_RINGTONE_FILENAME, Context.MODE_WORLD_READABLE); //openFileOutput underlined red
        try {
          fos.write(file.toString().trim().replace("#","\n").getBytes());
          fos.close();
        } catch (IOException e) {
          return;
        }
    }catch (IOException e) {
        return;
      }
    }
}
