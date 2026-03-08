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
 *
 * @author StephenQiu30
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart> implements ChartService {

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
        queryWrapper.eq(Chart::getIsDelete, 0);
        queryWrapper.orderByDesc(Chart::getCreateTime);
        return queryWrapper;
    }

    @Override
    public ChartVO getChartVO(Chart chart) {
        if (chart == null) {
            return null;
        }
        ChartVO chartVO = new ChartVO();
        BeanUtil.copyProperties(chart, chartVO);
        return chartVO;
    }

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
