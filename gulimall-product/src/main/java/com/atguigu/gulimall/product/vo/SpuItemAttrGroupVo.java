package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/16 11:13
 */
@Data
public class SpuItemAttrGroupVo {

    private String groupName;
    private List<SpuBaseAttrVo> attrs;

}
