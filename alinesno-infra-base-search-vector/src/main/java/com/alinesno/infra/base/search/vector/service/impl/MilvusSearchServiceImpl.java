package com.alinesno.infra.base.search.vector.service.impl;

import com.alinesno.infra.base.search.vector.dto.PDFDataDto;
import com.alinesno.infra.base.search.vector.service.IMilvusSearchService;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.partition.ShowPartitionsParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 此处功能代码参考 <a href="https://juejin.cn/post/7251501842986762301">SpringBoot整合Milvus</a>
 */
@Slf4j
@Service
public class MilvusSearchServiceImpl implements IMilvusSearchService {

    @Autowired
    private MilvusServiceClient milvusServiceClient;

    @Override
    public List<PDFDataDto> search(List<List<Float>> search_vectors) {
        milvusServiceClient.loadCollection(
                LoadCollectionParam.newBuilder()
                        .withCollectionName("pdf_data")
                        .build()
        );

        final Integer SEARCH_K = 4;
        final String SEARCH_PARAM = "{\"nprobe\":10}";

        List<String> ids = List.of("id");
        List<String> contents = List.of("content");
        List<String> contentWordCounts = List.of("content_word_count");

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName("pdf_data")
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withOutFields(ids)
                .withOutFields(contents)
                .withOutFields(contentWordCounts)
                .withTopK(SEARCH_K)
                .withVectors(search_vectors)
                .withVectorFieldName("content_vector")
                .withParams(SEARCH_PARAM)
                .build();

        R<SearchResults> respSearch = milvusServiceClient.search(searchParam);
        List<PDFDataDto> pdfDataList = new ArrayList<>();

        if(respSearch.getStatus() == R.Status.Success.getCode()){
            SearchResults resp = respSearch.getData();
            //判断是否查到结果
            if(!resp.hasResults()){
                return new ArrayList<>();
            }
            for (int i = 0; i < search_vectors.size(); ++i) {
                SearchResultsWrapper wrapperSearch = new SearchResultsWrapper(resp.getResults());
                List<Long> id = (List<Long>) wrapperSearch.getFieldData("id", 0);
                List<String> content = (List<String>) wrapperSearch.getFieldData("content", 0);
                List<Integer> contentWordCount = (List<Integer>) wrapperSearch.getFieldData("content_word_count", 0);
                PDFDataDto pdfData = new PDFDataDto(id.get(0),content.get(0),contentWordCount.get(0));
                pdfDataList.add(pdfData);
            }
        }

        milvusServiceClient.releaseCollection(
                ReleaseCollectionParam.newBuilder()
                        .withCollectionName("pdf_data")
                        .build());

        return pdfDataList;
    }

    /**
     * 同步搜索milvus
     * @param collectionName 表名
     * @param vectors 查询向量
     * @param topK 最相似的向量个数
     * @return
     */
    @Override
    public List<Long> search(String collectionName, List<List<Float>> vectors, Integer topK) {

        Assert.notNull(collectionName, "collectionName  is null");
        Assert.notNull(vectors, "vectors is null");
        Assert.notEmpty(vectors, "vectors is empty");
        Assert.notNull(topK, "topK is null");

        int nprobeVectorSize = vectors.get(0).size();

        String paramsInJson = "{'nprobe': " + nprobeVectorSize + "}";
        SearchParam searchParam =
                SearchParam.newBuilder().withCollectionName(collectionName)
                        .withParams(paramsInJson)
                        .withMetricType(MetricType.IP)
                        .withVectors(vectors)
                        .withVectorFieldName("document_content")
                        .withTopK(topK)
                        .build();

        R<SearchResults> searchResultsR = milvusServiceClient.search(searchParam);
        SearchResults searchResultsRData = searchResultsR.getData();
        List<Long> topksList = searchResultsRData.getResults().getIds().getIntId().getDataList();
        return topksList;
    }


    /**
     * 同步搜索milvus，增加过滤条件搜索
     *
     * @param collectionName 表名
     * @param vectors 查询向量
     * @param topK 最相似的向量个数
     * @param exp 过滤条件：status=1
     * @return
     */
    @Override
    public List<Long> search(String collectionName, List<List<Float>> vectors, Integer topK, String exp) {

        Assert.notNull(collectionName, "collectionName  is null");
        Assert.notNull(vectors, "vectors is null");
        Assert.notEmpty(vectors, "vectors is empty");
        Assert.notNull(topK, "topK is null");
        Assert.notNull(exp, "exp is null");

        int nprobeVectorSize = vectors.get(0).size();
        String paramsInJson = "{'nprobe': " + nprobeVectorSize + "}";

        SearchParam searchParam =
                SearchParam.newBuilder().withCollectionName(collectionName)
                        .withParams(paramsInJson)
                        .withMetricType(MetricType.IP)
                        .withVectors(vectors)
                        .withExpr(exp)
                        .withVectorFieldName("embedding")
                        .withTopK(topK)
                        .build();

        R<SearchResults> searchResultsR = milvusServiceClient.search(searchParam);
        log.debug("searchResultsR = {}" , searchResultsR);

        SearchResults searchResultsRData = searchResultsR.getData();

        return searchResultsRData.getResults().getIds().getIntId().getDataList();
    }


    /**
     * 异步搜索milvus
     *
     * @param collectionName 表名
     * @param vectors 查询向量
     * @param partitionList 最相似的向量个数
     * @param topK
     * @return
     */
    @Override
    public List<Long> searchAsync(String collectionName, List<List<Float>> vectors,
                                  List<String> partitionList, Integer topK) throws ExecutionException, InterruptedException {

        Assert.notNull(collectionName, "collectionName  is null");
        Assert.notNull(vectors, "vectors is null");
        Assert.notEmpty(vectors, "vectors is empty");
        Assert.notNull(partitionList, "partitionList is null");
        Assert.notEmpty(partitionList, "partitionList is empty");
        Assert.notNull(topK, "topK is null");
        int nprobeVectorSize = vectors.get(0).size();
        String paramsInJson = "{'nprobe': " + nprobeVectorSize + "}";
        SearchParam searchParam =
                SearchParam.newBuilder().withCollectionName(collectionName)
                        .withParams(paramsInJson)
                        .withVectors(vectors)
                        .withTopK(topK)
                        .withPartitionNames(partitionList)
                        .build();
        ListenableFuture<R<SearchResults>> listenableFuture = milvusServiceClient.searchAsync(searchParam);

        List<Long> resultIdsList = listenableFuture.get().getData().getResults().getTopksList();

        return resultIdsList;
    }

    /**
     * 获取分区集合
     * @param collectionName 表名
     * @return
     */
    @Override
    public List<String> getPartitionsList(String collectionName) {
        Assert.notNull(collectionName, "collectionName  is null");
        ShowPartitionsParam searchParam = ShowPartitionsParam.newBuilder().withCollectionName(collectionName).build();
        List<ByteString> byteStrings = milvusServiceClient.showPartitions(searchParam).getData().getPartitionNamesList().asByteStringList();
        List<String> partitionList = Lists.newLinkedList();
        byteStrings.forEach(s -> {
            partitionList.add(s.toStringUtf8());
        });
        return partitionList;
    }


}