package com.pubnative.phoneupdatelocale;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import java.util.ArrayList;

public class UpdateContactFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public String[] PROJECTION_DETAILS = new String[]
            {ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id,
                                         @Nullable Bundle args) {

        return new CursorLoader(
                getActivity(),
                ContactsContract.CommonDataKinds.
                        Phone.CONTENT_URI,
                PROJECTION_DETAILS,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader,
                               Cursor data) {
        if (loader.getId() == 0) {
            if (data != null) {
                while (!data.isClosed() && data.moveToNext()) {
                    long contactId = data.getLong(0);
                    String name = data.getString(1);
                    String phone = data.getString(2);
                    if (phone.startsWith("+") || phone.startsWith("00")) {
                        continue;
                    }
                    if (phone.startsWith("0")) {
                        phone = "+20" + phone.substring(1);
                    }
                    updateContact(name, String.valueOf(contactId), phone);
                }
                data.close();
                Log.d("Done", "Done");
            }
            LoaderManager.getInstance(UpdateContactFragment.this)
                    .initLoader(1, null, this);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    public void updateContact(String name, String rawContactId, String number) {
        ContentResolver contentResolver = getActivity().getContentResolver();

        String where = ContactsContract.Data.RAW_CONTACT_ID + " = ? AND "
                + ContactsContract.Data.MIMETYPE + " = ?";

        String[] nameParams = new String[]{rawContactId,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};
        String[] numberParams = new String[]{rawContactId,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE};

        final ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        if (!name.isEmpty()) {
            ops.add(android.content.ContentProviderOperation.newUpdate(
                    android.provider.ContactsContract.Data.CONTENT_URI)
                    .withSelection(where, nameParams)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                            name).build());
        }
        if (!number.isEmpty()) {
            ops.add(android.content.ContentProviderOperation.newUpdate(
                    android.provider.ContactsContract.Data.CONTENT_URI)
                    .withSelection(where, numberParams)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    .build());
        }
        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
