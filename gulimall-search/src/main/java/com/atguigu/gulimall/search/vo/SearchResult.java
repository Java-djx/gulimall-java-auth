package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/13 18:11
 * 检索页 响应结果
 */
@Data
public class SearchResult {

    /**
     * 查询到商品数据
     */
    private List<SkuEsModel> products;

    /**
     * 以下是分页消息
     */
    private Integer pageNum;//当前页码
    private Long total;//总记录数
    private Integer totalPages;//总页码

    private List<BrandVo> brands;//查询结果中 涉及的品牌消息
    private List<AttrVo> attrs;//查询结果中 涉及的属性消息
    private List<CatalogVo> catalogs;//查询结果中 涉及的分类消息

    //============ 以上是返回给页面的所有消息 =====================

    /**
     * 品牌静态内部类
     */
    @Data
    public static class BrandVo {

        private Long brandId;

        private String brandName;

        private String brandImg;
    }

    /**
     * 属性数据
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttrVo {

        private Long attrId;

        private String attrName;

        private List<String>  attrValue;
    }

    /**
     * 分类数据
     */
    @Data
    public static class CatalogVo {

        private Long catalogId;

        private String catalogName;
    }


}
