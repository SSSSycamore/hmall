package com.hmall.search.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.domain.dto.ItemDTO;

import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.dto.ItemAggDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create("192.168.100.128:9200")));
    private final String BRAND_AGG = "brand_agg";
    private final String CATEGORY_AGG = "category_agg";
    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        // 分页查询
        SearchRequest request = new SearchRequest("items");
        // 参数校验与默认值
        int pageNo = Optional.ofNullable(query.getPageNo()).orElse(1);
        int pageSize = Optional.ofNullable(query.getPageSize()).orElse(10);
        if (pageNo < 1) pageNo = 1;
        if (pageSize > 100 || pageSize < 1) pageSize = 10;

        // 动态构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (StringUtils.isNotBlank(query.getKey())) {
            boolQuery.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        // 价格范围
        RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
        if (query.getMinPrice() != null) priceRange.gte(query.getMinPrice());
        if (query.getMaxPrice() != null) priceRange.lte(query.getMaxPrice());
        if (query.getMinPrice() != null || query.getMaxPrice() != null) {
            boolQuery.filter(priceRange);
        }
        // 分类和品牌
        if (StringUtils.isNotBlank(query.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (StringUtils.isNotBlank(query.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        // 排序
        if (StringUtils.isBlank(query.getSortBy())){
            query.setSortBy("updateTime");
        }
        // 构建请求
        request.source()
                .query(boolQuery)
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .sort(query.getSortBy(), query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 解析结果
        SearchHits searchHits = response.getHits();
        long total = searchHits.getTotalHits().value;
        SearchHit[] hits = searchHits.getHits();
        List<ItemDTO> itemDTOList = new ArrayList<>();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            ItemDoc itemDoc = JSONUtil.toBean(sourceAsString, ItemDoc.class);
            ItemDTO itemDTO = BeanUtil.copyProperties(itemDoc, ItemDTO.class);
            itemDTOList.add(itemDTO);
        }
        return new PageDTO<>(total,Long.valueOf(query.getPageNo()), itemDTOList);
    }

    @ApiOperation("搜索商品")
    @GetMapping("/{id}")
    public ItemDTO search(@PathVariable("id") Long id) throws IOException {
        GetRequest request = new GetRequest("items", id.toString());
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String json = response.getSourceAsString();
        ItemDoc bean = JSONUtil.toBean(json, ItemDoc.class);
        return BeanUtil.copyProperties(bean, ItemDTO.class);
    }

    @ApiOperation("过滤项搜索")
    @PostMapping("/filters")
    public ItemAggDTO filter(@RequestBody ItemPageQuery query) throws IOException {
        // 分页查询
        SearchRequest request = new SearchRequest("items");
        // 参数校验与默认值
        int pageNo = Optional.ofNullable(query.getPageNo()).orElse(1);
        int pageSize = Optional.ofNullable(query.getPageSize()).orElse(10);
        if (pageNo < 1) pageNo = 1;
        if (pageSize > 100 || pageSize < 1) pageSize = 10;

        // 动态构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (StringUtils.isNotBlank(query.getKey())) {
            boolQuery.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        // 价格范围
        RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
        if (query.getMinPrice() != null) priceRange.gte(query.getMinPrice());
        if (query.getMaxPrice() != null) priceRange.lte(query.getMaxPrice());
        if (query.getMinPrice() != null || query.getMaxPrice() != null) {
            boolQuery.filter(priceRange);
        }
        // 分类和品牌
        if (StringUtils.isNotBlank(query.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (StringUtils.isNotBlank(query.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }

        // 构建请求
        request.source()
                .query(boolQuery)
                .size(0);

        request.source().aggregation(AggregationBuilders.terms(BRAND_AGG).field("brand").size(20));
        request.source().aggregation(AggregationBuilders.terms(CATEGORY_AGG).field("category").size(20));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 解析结果
        Aggregations aggregations = response.getAggregations();
        Terms brandAgg = (Terms) aggregations.get(BRAND_AGG);
        Terms categoryAgg = (Terms) aggregations.get(CATEGORY_AGG);
        List<String> brandNames = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            String key = bucket.getKeyAsString();
            brandNames.add(key);
        }
        for (Terms.Bucket bucket : categoryAgg.getBuckets()) {
            String key = bucket.getKeyAsString();
            categoryNames.add(key);
        }
        return new ItemAggDTO(categoryNames, brandNames);
    }
}
