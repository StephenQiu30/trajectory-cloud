package com.trajectory.cloud.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trajectory.cloud.ai.mapper.ChartMapper;
import com.trajectory.cloud.ai.model.entity.Chart;
import com.trajectory.cloud.ai.service.ChartService;
import com.trajectory.cloud.api.ai.model.dto.ChartQueryRequest;
import com.trajectory.cloud.api.ai.model.vo.ChartVO;
import com.trajectory.cloud.common.common.ErrorCode;
import com.trajectory.cloud.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图表信息服务实现类
 * <p>
 * 负责图表数据的查询、转换和管理，包括图表实体到视图对象的转换。
 * 核心功能包括：
 * 1. <b>查询条件构建</b>：支持多种查询条件的动态组合，如图表名称、类型、状态、用户ID等
 * 2. <b>视图对象转换</b>：将图表实体转换为脱敏的视图对象，保护敏感数据
 * 3. <b>分页数据转换</b>：支持分页数据的批量转换，保持分页元数据的一致性
 * </p>
 *
 * @author StephenQiu30
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart> implements ChartService {

    /**
     * 构造图表分页查询封装类
     *
     * @param chartQueryRequest 图表查询请求参数
     * @return MyBatis-Plus 的 LambdaQueryWrapper 封装对象
     */
    @Override
    public LambdaQueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();

        LambdaQueryWrapper<Chart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(id != null, Chart::getId, id);
        queryWrapper.like(StringUtils.isNotBlank(name), Chart::getName, name);
        queryWrapper.like(StringUtils.isNotBlank(goal), Chart::getGoal, goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), Chart::getChartType, chartType);
        queryWrapper.eq(userId != null, Chart::getUserId, userId);
        queryWrapper.eq(Chart::getIsDelete, 0); // 仅查询未逻辑删除的数据
        queryWrapper.orderByDesc(Chart::getCreateTime);
        return queryWrapper;
    }

    /**
     * 获取图表视图对象 (VO)
     *
     * @param chart 图表实体
     * @return 脱敏后的视图对象
     */
    @Override
    public ChartVO getChartVO(Chart chart) {
        if (chart == null) {
            return null;
        }
        ChartVO chartVO = new ChartVO();
        BeanUtil.copyProperties(chart, chartVO);
        return chartVO;
    }

    /**
     * 将图表实体分页对象转换为视图对象分页对象
     *
     * @param chartPage 原生的图表实体分页对象
     * @return 转换后的视图对象分页对象
     */
    @Override
    public Page<ChartVO> getChartVOPage(Page<Chart> chartPage) {
        List<Chart> chartList = chartPage.getRecords();
        Page<ChartVO> chartVOPage = new Page<>(chartPage.getCurrent(), chartPage.getSize(), chartPage.getTotal());
        if (CollUtil.isEmpty(chartList)) {
            return chartVOPage;
        }
        List<ChartVO> chartVOList = chartList.stream().map(this::getChartVO).collect(Collectors.toList());
        chartVOPage.setRecords(chartVOList);
        return chartVOPage;
    }
}
