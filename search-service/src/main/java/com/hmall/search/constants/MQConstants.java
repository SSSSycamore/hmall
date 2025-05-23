package com.hmall.search.constants;

public interface MQConstants {
    String SEARCH_EXCHANGE_NAME = "search.direct";
    String INDEX_QUEUE_NAME = "search.item.index.queue";
    String DELETE_QUEUE_NAME = "search.item.delete.queue";
    String INDEX_KEY = "item.index";
    String DELETE_KEY = "item.delete";
}
