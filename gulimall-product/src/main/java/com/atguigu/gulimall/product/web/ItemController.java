package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.ExecutionException;


/**
 * @version 1.0
 * @author: djx
 * @createTime: 2022/11/15 19:08
 */
@Controller
@Slf4j
public class ItemController {

    @Autowired
    private SkuInfoService skuInfoService;


    /**
     * 展示sku的详情
     *
     * @param skuId
     * @return
     */
    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model) {

        log.info("正在进入{}编号的详情页面", skuId);
        SkuItemVo vo = null;
        try {
            vo = skuInfoService.item(skuId);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        model.addAttribute("item",vo);
        return "item";

    }

}
