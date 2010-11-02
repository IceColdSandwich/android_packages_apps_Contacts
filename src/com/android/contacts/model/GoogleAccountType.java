/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.model;

import com.android.contacts.R;
import com.google.android.collect.Lists;

import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.view.inputmethod.EditorInfo;

public class GoogleAccountType extends FallbackAccountType {
    public static final String ACCOUNT_TYPE = "com.google";
    protected static final int FLAGS_RELATION = EditorInfo.TYPE_CLASS_TEXT
    | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS | EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME;

    public GoogleAccountType(String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.resPackageName = null;
        this.summaryResPackageName = resPackageName;
    }

    @Override
    protected void inflate(Context context, int inflateLevel) {
        super.inflate(context, inflateLevel);

        inflateRelation(context, inflateLevel);
    }

    @Override
    protected DataKind inflatePhone(Context context, int inflateLevel) {
        final DataKind kind = super.inflatePhone(context, BaseAccountType.LEVEL_MIMETYPES);

        if (inflateLevel >= BaseAccountType.LEVEL_CONSTRAINTS) {
            kind.typeColumn = Phone.TYPE;
            kind.typeList = Lists.newArrayList();
            kind.typeList.add(buildPhoneType(Phone.TYPE_HOME));
            kind.typeList.add(buildPhoneType(Phone.TYPE_MOBILE));
            kind.typeList.add(buildPhoneType(Phone.TYPE_WORK));
            kind.typeList.add(buildPhoneType(Phone.TYPE_FAX_WORK).setSecondary(true));
            kind.typeList.add(buildPhoneType(Phone.TYPE_FAX_HOME).setSecondary(true));
            kind.typeList.add(buildPhoneType(Phone.TYPE_PAGER).setSecondary(true));
            kind.typeList.add(buildPhoneType(Phone.TYPE_OTHER));
            kind.typeList.add(buildPhoneType(Phone.TYPE_CUSTOM).setSecondary(true).setCustomColumn(
                    Phone.LABEL));

            kind.fieldList = Lists.newArrayList();
            kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));
        }

        return kind;
    }

    @Override
    protected DataKind inflateEmail(Context context, int inflateLevel) {
        final DataKind kind = super.inflateEmail(context, BaseAccountType.LEVEL_MIMETYPES);

        if (inflateLevel >= BaseAccountType.LEVEL_CONSTRAINTS) {
            kind.typeColumn = Email.TYPE;
            kind.typeList = Lists.newArrayList();
            kind.typeList.add(buildEmailType(Email.TYPE_HOME));
            kind.typeList.add(buildEmailType(Email.TYPE_WORK));
            kind.typeList.add(buildEmailType(Email.TYPE_OTHER));
            kind.typeList.add(buildEmailType(Email.TYPE_CUSTOM).setSecondary(true).setCustomColumn(
                    Email.LABEL));

            kind.fieldList = Lists.newArrayList();
            kind.fieldList.add(new EditField(Email.DATA, R.string.emailLabelsGroup, FLAGS_EMAIL));
        }

        return kind;
    }

    protected DataKind inflateRelation(Context context, int inflateLevel) {
        DataKind kind = getKindForMimetype(Relation.CONTENT_ITEM_TYPE);
        if (kind == null) {
            kind = addKind(new DataKind(Relation.CONTENT_ITEM_TYPE,
                    R.string.relationLabelsGroup, -1, 160, true));
            kind.actionHeader = new RelationActionInflater();
            kind.actionBody = new SimpleInflater(Relation.NAME);

            kind.typeColumn = Relation.TYPE;
            kind.typeList = Lists.newArrayList();
            kind.typeList.add(buildRelationType(Relation.TYPE_ASSISTANT));
            kind.typeList.add(buildRelationType(Relation.TYPE_BROTHER));
            kind.typeList.add(buildRelationType(Relation.TYPE_CHILD));
            kind.typeList.add(buildRelationType(Relation.TYPE_DOMESTIC_PARTNER));
            kind.typeList.add(buildRelationType(Relation.TYPE_FATHER));
            kind.typeList.add(buildRelationType(Relation.TYPE_FRIEND));
            kind.typeList.add(buildRelationType(Relation.TYPE_MANAGER));
            kind.typeList.add(buildRelationType(Relation.TYPE_MOTHER));
            kind.typeList.add(buildRelationType(Relation.TYPE_PARENT));
            kind.typeList.add(buildRelationType(Relation.TYPE_PARTNER));
            kind.typeList.add(buildRelationType(Relation.TYPE_REFERRED_BY));
            kind.typeList.add(buildRelationType(Relation.TYPE_RELATIVE));
            kind.typeList.add(buildRelationType(Relation.TYPE_SISTER));
            kind.typeList.add(buildRelationType(Relation.TYPE_SPOUSE));
            kind.typeList.add(buildRelationType(Relation.TYPE_CUSTOM).setSecondary(true)
                    .setCustomColumn(Relation.LABEL));

            kind.defaultValues = new ContentValues();
            kind.defaultValues.put(Relation.TYPE, Relation.TYPE_SPOUSE);

            kind.fieldList = Lists.newArrayList();
            kind.fieldList.add(new EditField(Relation.DATA, R.string.relationLabelsGroup,
                    FLAGS_RELATION));
        }

        return kind;
    }

    @Override
    public int getHeaderColor(Context context) {
        return 0xff89c2c2;
    }

    @Override
    public int getSideBarColor(Context context) {
        return 0xff5bb4b4;
    }
}