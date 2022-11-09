package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GuliMallElasticsearchConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysql.cj.QueryBindings;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {

    @Resource
    private RestHighLevelClient client;

    @Test
    public void contextLoads() {

        System.out.println(client);
    }

    /**
     * 添加数据ES
     */
    @Test
    public void createIndex() throws IOException {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.index("users");
        //索引的id
        indexRequest.id("1");

        User user = new User();
        user.setAge(18);
        user.setUserName("张三");
        user.setGender("男");
        //要保存的内容
        String jsonString = JSON.toJSONString(user);

        indexRequest.source(jsonString, XContentType.JSON);

        //响应数据
        IndexResponse response = client.index(indexRequest, GuliMallElasticsearchConfig.COMMON_OPTIONS);

        System.out.println(response);
    }

    @Test
    public void getIndex() throws IOException {
        GetRequest getRequest = new GetRequest();
        getRequest.index("users");
        getRequest.id("1");


        GetResponse response = client.get(getRequest, GuliMallElasticsearchConfig.COMMON_OPTIONS);

        System.out.println(response);


    }

    @Test
    public void searchIndex() throws IOException {
        //创建查询对象
        SearchRequest request = new SearchRequest();
        request.indices("users");
        //构造查询执行对象
        SearchSourceBuilder builder = new SearchSourceBuilder();

        builder.query(QueryBuilders.matchQuery("address", "mill"));

        //构造聚合条件
        //按照年龄的值聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age");
        builder.aggregation(ageAgg);
        //计算平均值
        AvgAggregationBuilder avg = AggregationBuilders.avg("balanceAvg").field("balance");
        builder.aggregation(avg);


        //检索数据源
        SearchRequest source = request.source(builder);
        //返回的数据
        SearchResponse response = client.search(source, GuliMallElasticsearchConfig.COMMON_OPTIONS);

        //分析数据
        SearchHits hits = response.getHits();

        SearchHit[] searchHits = hits.getHits();

        for (SearchHit hit : searchHits) {

            String sourceAsString = hit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println("account = " + account);

        }

        Aggregations aggregations = response.getAggregations();



    }


    @Data
    @ToString
   static class Account {
        private int accountNumber;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }

    @Data
    class User {
        private String userName;

        private String gender;

        private Integer age;


    }

}
