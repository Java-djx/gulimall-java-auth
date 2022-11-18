package com.atguigu.gulimall.member.service.impl;

import com.atguigu.common.utils.RRException;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneException;
import com.atguigu.gulimall.member.exception.UsernameException;
import com.atguigu.gulimall.member.service.MemberLevelService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelService memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegisterVo vo) {
        MemberDao baseMapper = this.baseMapper;
        MemberEntity entity = new MemberEntity();

        MemberLevelEntity memberLevelEntity = memberLevelService.getOne(new QueryWrapper<MemberLevelEntity>().eq("default_status", 1));
        // 设置会员等级
        entity.setLevelId(memberLevelEntity.getId());
        //检查用户名和手机号码
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());
        //设置手机号码和用户名 密码
        entity.setUsername(vo.getUserName());
        entity.setPassword(vo.getPassword());
        entity.setMobile(vo.getPhone());
        //加密存储
        String password = vo.getPassword();
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        //加盐加密
        String encode = bCryptPasswordEncoder.encode(password);
        entity.setPassword(encode);
        //默认消息
        entity.setCreateTime(new Date());
        entity.setGender(0);
        entity.setIntegration(0);
        baseMapper.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneException {

        Integer phoneCount = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));

        if (phoneCount > 0) {
            throw new PhoneException();
        }

    }

    @Override
    public void checkUserNameUnique(String userName) throws UsernameException {

        Integer usernameCount = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (usernameCount > 0) {
            throw new UsernameException();
        }
    }


    @Override
    public MemberEntity login(MemberLoginVo vo) {

        String loginacct = vo.getLoginacct();

        String password = vo.getPassword();
        //取数据库解密
        MemberEntity one = this.getOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));
        if (one == null) {
            //登录失败
            return null;
        } else {
            //获取得数据的密文
            String onePassword = one.getPassword();
            //根据页面的密码对比
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //进行密码匹配登录成功
            boolean matches = passwordEncoder.matches(password, onePassword);
            if (matches) {
                return one;
            } else {
                return null;
            }
        }
    }

}