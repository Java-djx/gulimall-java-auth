package com.atguigu.gulimall.search.service;

import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/12 21:10
 */
public interface MallSearchService {

    /**
     * @param searchParam 检索的所有参数
     * @return 检索的结果 包含页面的所有消息
     */
   public SearchResult search(SearchParam searchParam);

}
