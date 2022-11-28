package com.atguigu.common.exception;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/24 20:05
 */
public class NoStockException extends RuntimeException {

    private Long skuId;

    public NoStockException(Long skuId) {
        super("商品id为:" + skuId + "没有足够的库存了");
    }

    public NoStockException(String msg) {
        super(msg);
    }



    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
