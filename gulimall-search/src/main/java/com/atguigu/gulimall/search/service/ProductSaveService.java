package com.atguigu.gulimall.search.service;

import com.atguigu.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 16:36
 */
public interface ProductSaveService {
    boolean productStatusUp(List<SkuEsModel> esModels) throws IOException;
}
