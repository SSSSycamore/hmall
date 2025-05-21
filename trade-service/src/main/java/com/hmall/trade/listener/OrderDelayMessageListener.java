package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.domain.dto.PayOrderDTO;
import com.hmall.trade.constants.MQConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderDelayMessageListener {
    private final IOrderService orderService;
    private final PayClient payClient;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE_NAME, type = "direct", delayed = "true"),
            key = MQConstants.DELAY_ORDER_KEY)
    )
    public void listenOrderDelayMessage(Long orderId){
        // 1.查询本地订单
        Order order = orderService.getById(orderId);
        // 2.检测订单状态
        if (order == null || order.getStatus() != 1) {
            // 订单不存在或者订单状态不是未支付，直接返回
            return;
        }
        // 3.如果订单未支付，查询支付流水
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        // 4.判断是否支付
        // 5.如果未支付，关闭订单，恢复库存
        if (payOrderDTO == null || payOrderDTO.getStatus() != 3) {
            // 关闭订单，恢复库存
            orderService.cancelOrder(orderId);
        }else {// 6.如果已支付，修改订单状态
            orderService.markOrderPaySuccess(orderId);
        }
    }
}
