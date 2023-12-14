package com.alinesno.infra.base.search.vector.service.impl;

import com.alinesno.infra.base.search.vector.dto.CollectFieldType;
import com.alinesno.infra.base.search.vector.dto.InsertField;
import com.alinesno.infra.base.search.vector.service.IMilvusDataService;
import com.alinesno.infra.base.search.vector.utils.EmbeddingClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MilvusDataServiceImpl implements IMilvusDataService {

    @Autowired
    private MilvusServiceClient milvusServiceClient;

    @Autowired
    private EmbeddingClient embeddingClient ;

    /**
     * 创建包含指定字段类型的集合的参数构建方法。
     * @param collectionName 集合名称。
     * @param description 集合描述。
     * @param shardsNum 分片数量。
     * @param fieldType 字段类型列表。
     */
    @Override
    public void buildCreateCollectionParam(String collectionName,
                                           String description,
                                           int shardsNum,
                                           CollectFieldType fieldType) {

        FieldType ft = null ;

        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription(description)
                .withShardsNum(shardsNum)
                .addFieldType(ft)
//                .withEnableDynamicField(true)
                .build();

        milvusServiceClient.createCollection(createCollectionReq) ;
    }

    /**
     * 执行Milvus插入操作的方法。
     * @param collectionName 集合名称。
     * @param partitionName 分区名称。
     * @param fields 字段数据
     */
    @Override
    public void insertData(String collectionName, String partitionName, List<InsertField> fields) {

        List<InsertParam.Field> insertField = new ArrayList<>();

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withPartitionName(partitionName)
                .withFields(insertField)
                .build();

        milvusServiceClient.insert(insertParam);
    }

    /**
     * 执行Milvus删除操作的方法。
     * @param collectionName 集合名称。
     * @param deleteExpr 删除表达式。
     */
    @Override
    public void deleteData(String collectionName, String deleteExpr) {
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(deleteExpr)
                .build();
        milvusServiceClient.delete(deleteParam);
    }

    @Override
    public void doEmbedding(String msg) {

        List<String> sentenceList = new ArrayList<>();
        sentenceList.add(msg);

        this.save(sentenceList);
    }

    @Override
    public void save(List<String> sentenceList){

        List<Integer> contentWordCount = new ArrayList<>();
        List<List<Float>> contentVector;

        for(String str : sentenceList){
            contentWordCount.add(str.length());
        }
        contentVector = embeddingClient.doEmbedding(sentenceList);

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("content", sentenceList));
        fields.add(new InsertParam.Field("content_word_count", contentWordCount));
        fields.add(new InsertParam.Field("content_vector", contentVector));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName("pdf_data")
                .withFields(fields)
                .build();

        //插入数据
        milvusServiceClient.insert(insertParam);
    }

}
