package com.atguigu.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 21:28
 * 二级分类VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Catalog2Vo {


    private String catalog1Id; //一级父分类

    private List<Catelog3Vo> catalog3List;//三级子分类

    private String id;

    private String name;
    /**
     * 三级分类 内部类的形式
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Catelog3Vo{

        private String catalog2Id;//二级分类id

        private String id;
        private String name;
    }

}
