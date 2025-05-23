package com.hmall.search.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.domain.dto.ItemDTO;
import com.hmall.search.constants.MQConstants;
import com.hmall.search.domain.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ItemListener {
    private final ItemClient itemClient;
    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create("192.168.100.128:9200")));
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MQConstants.INDEX_QUEUE_NAME),
            exchange = @Exchange(value = MQConstants.SEARCH_EXCHANGE_NAME,type = "direct"),
            key = MQConstants.INDEX_KEY)
    )
    public void listenItemIndex(Long id) throws IOException {
        log.info("接收到商品服务发来的信息,{}号商品插入或修改",id);
        ItemDTO itemDTO = itemClient.queryItemById(id);
        if (itemDTO == null) {
            log.error("商品服务发来的商品信息不存在,{}",id);
            return;
        }
        ItemDoc itemDoc = BeanUtil.copyProperties(itemDTO, ItemDoc.class);
        String jsonStr = JSONUtil.toJsonStr(itemDoc);
        IndexRequest request = new IndexRequest("items").id(id.toString()).source(jsonStr, XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
        log.info("我检测到商品服务给我发来的新增或修改商品信息,我被触发然后开始存入信息:{}",jsonStr);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MQConstants.DELETE_QUEUE_NAME),
            exchange = @Exchange(value = MQConstants.SEARCH_EXCHANGE_NAME,type = "direct"),
            key = MQConstants.DELETE_KEY)
    )
    public void listenItemDelete(Long id) throws IOException {
        log.info("接收到商品服务发来的信息,{}号商品被删除",id);
        DeleteRequest request = new DeleteRequest("items", id.toString());
        client.delete(request, RequestOptions.DEFAULT);
        log.info("我检测到商品服务给我发来的删除商品信息,我被触发然后删除了{}号商品",id);
    }
}
