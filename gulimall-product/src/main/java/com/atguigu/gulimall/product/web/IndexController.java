package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/9 21:10
 */
@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 跳转到首页查询所有 一级分类
     * @return
     */
    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

     List<CategoryEntity> categoryEntities= categoryService.getLavel1Categorys();

        model.addAttribute("categorys",categoryEntities);

        return "index";
    }

    @GetMapping("/index/json/catalog")
    @ResponseBody
    public Map<String,  List<Catalog2Vo>> getCatelogJson(){

        Map<String,List<Catalog2Vo>> categoryEntities=categoryService.getCatelogJson();

        return categoryEntities;
    }

}
