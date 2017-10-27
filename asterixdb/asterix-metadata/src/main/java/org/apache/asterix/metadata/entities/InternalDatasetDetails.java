/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.asterix.metadata.entities;

import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import org.apache.asterix.builders.IARecordBuilder;
import org.apache.asterix.builders.OrderedListBuilder;
import org.apache.asterix.builders.RecordBuilder;
import org.apache.asterix.common.config.DatasetConfig.DatasetType;
import org.apache.asterix.formats.nontagged.AqlSerializerDeserializerProvider;
import org.apache.asterix.metadata.IDatasetDetails;
import org.apache.asterix.metadata.bootstrap.MetadataRecordTypes;
import org.apache.asterix.om.base.ABoolean;
import org.apache.asterix.om.base.AInt8;
import org.apache.asterix.om.base.AMutableInt8;
import org.apache.asterix.om.base.AMutableString;
import org.apache.asterix.om.base.AString;
import org.apache.asterix.om.types.AOrderedListType;
import org.apache.asterix.om.types.ARecordType;
import org.apache.asterix.om.types.BuiltinType;
import org.apache.asterix.om.types.IAType;
import org.apache.hyracks.api.dataflow.value.ISerializerDeserializer;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;

public class InternalDatasetDetails implements IDatasetDetails {

    private static final long serialVersionUID = 1L;

    public enum FileStructure {
        BTREE
    };

    public enum PartitioningStrategy {
        HASH
    };

    private final FileStructure fileStructure;
    private final PartitioningStrategy partitioningStrategy;
    private final List<List<String>> partitioningKeys;
    private final List<List<String>> primaryKeys;
    private final List<IAType> primaryKeyTypes;
    private final boolean autogenerated;
    private final boolean temp;
    private long lastAccessTime;
    private final List<String> filterField;
    private final List<Integer> keySourceIndicators;

    public static final String FILTER_FIELD_NAME = "FilterField";
    public static final String KEY_FILD_SOURCE_INDICATOR_FIELD_NAME = "KeySourceIndicator";

    public InternalDatasetDetails(FileStructure fileStructure, PartitioningStrategy partitioningStrategy,
            List<List<String>> partitioningKey, List<List<String>> primaryKey, List<Integer> keyFieldIndicators,
            List<IAType> primaryKeyType, boolean autogenerated, List<String> filterField, boolean temp) {
        this.fileStructure = fileStructure;
        this.partitioningStrategy = partitioningStrategy;
        this.partitioningKeys = partitioningKey;
        this.primaryKeys = primaryKey;
        if (keyFieldIndicators == null) {
            // Create a dummy list.
            keyFieldIndicators = new ArrayList<>();
            for (int index = 0; index < partitioningKey.size(); ++index) {
                keyFieldIndicators.add(0);
            }
        }
        this.keySourceIndicators = keyFieldIndicators;
        this.primaryKeyTypes = primaryKeyType;
        this.autogenerated = autogenerated;
        this.filterField = filterField;
        this.temp = temp;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public List<List<String>> getPartitioningKey() {
        return partitioningKeys;
    }

    public boolean isAutogenerated() {
        return autogenerated;
    }

    public List<List<String>> getPrimaryKey() {
        return primaryKeys;
    }

    public List<Integer> getKeySourceIndicator() {
        return keySourceIndicators;
    }

    public List<IAType> getPrimaryKeyType() {
        return primaryKeyTypes;
    }

    public FileStructure getFileStructure() {
        return fileStructure;
    }

    public PartitioningStrategy getPartitioningStrategy() {
        return partitioningStrategy;
    }

    public List<String> getFilterField() {
        return filterField;
    }

    @Override
    public DatasetType getDatasetType() {
        lastAccessTime = System.currentTimeMillis();
        return DatasetType.INTERNAL;
    }

    @Override
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public boolean isTemp() {
        return temp;
    }

    @Override
    public void writeDatasetDetailsRecordType(DataOutput out) throws HyracksDataException {

        IARecordBuilder internalRecordBuilder = new RecordBuilder();
        OrderedListBuilder listBuilder = new OrderedListBuilder();
        ArrayBackedValueStorage fieldValue = new ArrayBackedValueStorage();
        ArrayBackedValueStorage itemValue = new ArrayBackedValueStorage();
        OrderedListBuilder primaryKeyListBuilder = new OrderedListBuilder();
        AOrderedListType stringList = new AOrderedListType(BuiltinType.ASTRING, null);
        AOrderedListType int8List = new AOrderedListType(BuiltinType.AINT8, null);
        AOrderedListType heterogeneousList = new AOrderedListType(BuiltinType.ANY, null);
        internalRecordBuilder.reset(MetadataRecordTypes.INTERNAL_DETAILS_RECORDTYPE);
        AMutableString aString = new AMutableString("");
        AMutableInt8 aInt8 = new AMutableInt8((byte) 0);
        @SuppressWarnings("unchecked")
        ISerializerDeserializer<ABoolean> booleanSerde = AqlSerializerDeserializerProvider.INSTANCE
                .getSerializerDeserializer(BuiltinType.ABOOLEAN);
        @SuppressWarnings("unchecked")
        ISerializerDeserializer<AString> stringSerde = AqlSerializerDeserializerProvider.INSTANCE
                .getSerializerDeserializer(BuiltinType.ASTRING);
        @SuppressWarnings("unchecked")
        ISerializerDeserializer<AInt8> int8Serde = AqlSerializerDeserializerProvider.INSTANCE
                .getSerializerDeserializer(BuiltinType.AINT8);

        // write field 0
        fieldValue.reset();
        aString.setValue(getFileStructure().toString());
        stringSerde.serialize(aString, fieldValue.getDataOutput());
        internalRecordBuilder.addField(MetadataRecordTypes.INTERNAL_DETAILS_ARECORD_FILESTRUCTURE_FIELD_INDEX,
                fieldValue);

        // write field 1
        fieldValue.reset();
        aString.setValue(getPartitioningStrategy().toString());
        stringSerde.serialize(aString, fieldValue.getDataOutput());
        internalRecordBuilder.addField(MetadataRecordTypes.INTERNAL_DETAILS_ARECORD_PARTITIONSTRATEGY_FIELD_INDEX,
                fieldValue);

        // write field 2
        primaryKeyListBuilder.reset((AOrderedListType) MetadataRecordTypes.INTERNAL_DETAILS_RECORDTYPE
                .getFieldTypes()[MetadataRecordTypes.INTERNAL_DETAILS_ARECORD_PARTITIONKEY_FIELD_INDEX]);
        for (List<String> field : partitioningKeys) {
            listBuilder.reset(stringList);
            for (String subField : field) {
                itemValue.reset();
                aString.setValue(subField);
                stringSerde.serialize(aString, itemValue.getDataOutput());
                listBuilder.addItem(itemValue);
            }
            itemValue.reset();
            listBuilder.write(itemValue.getDataOutput(), true);
            primaryKeyListBuilder.addItem(itemValue);
        }
        fieldValue.reset();
        primaryKeyListBuilder.write(fieldValue.getDataOutput(), true);
        internalRecordBuilder.addField(MetadataRecordTypes.INTERNAL_DETAILS_ARECORD_PARTITIONKEY_FIELD_INDEX,
                fieldValue);

        // write field 3
        primaryKeyListBuilder.reset((AOrderedListType) MetadataRecordTypes.INTERNAL_DETAILS_RECORDTYPE
                .getFieldTypes()[MetadataRecordTypes.INTERNAL_DETAILS_ARECORD_PRIMARYKEY_FIELD_INDEX]);
        for (List<String> field : primaryKeys) {
            listBuilder.reset(stringList);
            for (String subField : field) {
                itemValue.reset();
                aString.setValue(subField);
                stringSerde.serialize(aString, itemValue.getDataOutput());
                listBuilder.addItem(itemValue);
            }
            itemValue.reset();
            listBuilder.write(itemValue.getDataOutput(), true);
            primaryKeyListBuilder.addItem(itemValue);
        }
        fieldValue.reset();
        primaryKeyListBuilder.write(fieldValue.getDataOutput(), true);
        internalRecordBuilder.addField(MetadataRecordTypes.INTERNAL_DETAILS_ARECORD_PRIMARYKEY_FIELD_INDEX, fieldValue);

        // write field 4
        fieldValue.reset();
        ABoolean b = isAutogenerated() ? ABoolean.TRUE : ABoolean.FALSE;
        booleanSerde.serialize(b, fieldValue.getDataOutput());
        internalRecordBuilder.addField(MetadataRecordTypes.INTERNAL_DETAILS_ARECORD_AUTOGENERATED_FIELD_INDEX,
                fieldValue);

        // write filter fields if any
        List<String> filterField = getFilterField();
        if (filterField != null) {
            listBuilder.reset(heterogeneousList);
            ArrayBackedValueStorage nameValue = new ArrayBackedValueStorage();
            nameValue.reset();
            aString.setValue(FILTER_FIELD_NAME);
            stringSerde.serialize(aString, nameValue.getDataOutput());
            for (String field : filterField) {
                itemValue.reset();
                aString.setValue(field);
                stringSerde.serialize(aString, itemValue.getDataOutput());
                listBuilder.addItem(itemValue);
            }
            fieldValue.reset();
            listBuilder.write(fieldValue.getDataOutput(), true);
            internalRecordBuilder.addField(nameValue, fieldValue);
        }

        // write key source indicators if any
        List<Integer> keySourceIndicator = getKeySourceIndicator();
        boolean needSerialization = false;
        if (keySourceIndicator != null) {
            for (int source : keySourceIndicator) {
                if (source != 0) {
                    needSerialization = true;
                    break;
                }
            }
        }
        if (needSerialization) {
            listBuilder.reset(int8List);
            ArrayBackedValueStorage nameValue = new ArrayBackedValueStorage();
            nameValue.reset();
            aString.setValue(KEY_FILD_SOURCE_INDICATOR_FIELD_NAME);
            stringSerde.serialize(aString, nameValue.getDataOutput());
            for (int source : keySourceIndicator) {
                itemValue.reset();
                aInt8.setValue((byte) source);
                int8Serde.serialize(aInt8, itemValue.getDataOutput());
                listBuilder.addItem(itemValue);
            }
            fieldValue.reset();
            listBuilder.write(fieldValue.getDataOutput(), true);
            internalRecordBuilder.addField(nameValue, fieldValue);
        }

        internalRecordBuilder.write(out, true);
    }

    protected void writePropertyTypeRecord(String name, String value, DataOutput out, ARecordType recordType)
            throws HyracksDataException {
        IARecordBuilder propertyRecordBuilder = new RecordBuilder();
        ArrayBackedValueStorage fieldValue = new ArrayBackedValueStorage();
        propertyRecordBuilder.reset(recordType);
        AMutableString aString = new AMutableString("");
        @SuppressWarnings("unchecked")
        ISerializerDeserializer<AString> stringSerde = AqlSerializerDeserializerProvider.INSTANCE
                .getSerializerDeserializer(BuiltinType.ASTRING);

        // write field 0
        fieldValue.reset();
        aString.setValue(name);
        stringSerde.serialize(aString, fieldValue.getDataOutput());
        propertyRecordBuilder.addField(0, fieldValue);

        // write field 1
        fieldValue.reset();
        aString.setValue(value);
        stringSerde.serialize(aString, fieldValue.getDataOutput());
        propertyRecordBuilder.addField(1, fieldValue);

        propertyRecordBuilder.write(out, true);
    }

}
