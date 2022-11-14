package com.atguigu.gulimall.search.controller;

import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.elasticsearch.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/12 20:56
 * 搜索页面控制器
 */
@Controller
public class SearchController {

    @Autowired
    private MallSearchService mallSearchService;

    /**
     * 接受检索条件
     * 处理数据返回数据
     *
     * @return
     */
    @GetMapping({"/list.html", "/"})
    public String PageList(SearchParam param, Model model, HttpServletRequest request) {
        param.set_queryString(request.getQueryString());
        //1、根据页面传递的参数去ES检索结果
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result", result);


        return "list";
    }


}
