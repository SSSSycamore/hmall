package com.hmall.search.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.domain.dto.ItemDTO;

import com.hmall.search.domain.po.ItemDoc;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {
    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create("192.168.100.128:9200")));

    //@ApiOperation("搜索商品")
    //@GetMapping("/list")
    //public PageDTO<ItemDTO> search(ItemPageQuery query) {
    //    // 分页查询
    //    Page<Item> result = itemService.lambdaQuery()
    //            .like(StrUtil.isNotBlank(query.getKey()), Item::getName, query.getKey())
    //            .eq(StrUtil.isNotBlank(query.getBrand()), Item::getBrand, query.getBrand())
    //            .eq(StrUtil.isNotBlank(query.getCategory()), Item::getCategory, query.getCategory())
    //            .eq(Item::getStatus, 1)
    //            .between(query.getMaxPrice() != null, Item::getPrice, query.getMinPrice(), query.getMaxPrice())
    //            .page(query.toMpPage("update_time", false));
    //    // 封装并返回
    //    return PageDTO.of(result, ItemDTO.class);
    //}

    @ApiOperation("搜索商品")
    @GetMapping("/{id}")
    public ItemDTO search(@PathVariable("id") Long id) throws IOException {
        GetRequest request = new GetRequest("items", id.toString());
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String json = response.getSourceAsString();
        ItemDoc bean = JSONUtil.toBean(json, ItemDoc.class);
        return BeanUtil.copyProperties(bean, ItemDTO.class);
    }
}
