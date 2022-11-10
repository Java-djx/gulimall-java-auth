package com.atguigu.gulimall.search.controller;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 16:31
 * 像ES中保存数据模型
 */
@RestController
@RequestMapping("/search/save")
@Slf4j
public class ElasticSaveController {


    @Autowired
    private ProductSaveService productSaveService;


    //上架商品
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> esModels) {
        boolean b=false;
        try {
            b=productSaveService.productStatusUp(esModels);
        } catch (Exception e) {
            log.error("ElasticSaveController商品上架错误,原因:{}",e);
            return R.error(BizCodeEnume.PRODUCT_EXCEPTION.getCode(),BizCodeEnume.PRODUCT_EXCEPTION.getMsg());
        }
        if (!b){
            return R.ok();
        }else{
            return R.error(BizCodeEnume.PRODUCT_EXCEPTION.getCode(),BizCodeEnume.PRODUCT_EXCEPTION.getMsg());
        }
    }

}
