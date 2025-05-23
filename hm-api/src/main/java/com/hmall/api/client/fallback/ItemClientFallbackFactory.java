package com.hmall.api.client.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.domain.dto.ItemDTO;
import com.hmall.api.domain.dto.OrderDetailDTO;
import com.hmall.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.List;

@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient(){
            @Override
            public List<ItemDTO> getItemsByIds(Collection<Long> ids) {
                log.error("异常信息:{}", cause);
                return CollUtils.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("异常信息:{}", cause);
                throw new RuntimeException(cause);
            }

            @Override
            public ItemDTO queryItemById(Long id) {
                log.error("异常信息:{}", cause);
                return null;
            }
        };
    }
}
