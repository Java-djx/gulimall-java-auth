package com.atguigu.gulimall.product.vo;

import lombok.Data;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/4 19:37
 */
@Data
public class AttrRespVo extends AttrVo {

    /**
     * 			"catelogName": "手机/数码/手机", //所属分类名字
     * 			"groupName": "主体", //所属分组名字
     * 		    "catelogPath": [2, 34, 225] //分类完整路径
     */


    /**
     * 所属分类名字
     */
    private String catelogName;

    /**
     * 所属分组名字
     */
    private String groupName;


    /**
     * 分类完整路径
     */
    private Long[] catelogPath;


}
