package com.trajectory.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.trajectory.cloud.ai.model.entity.Chart;
import com.trajectory.cloud.api.ai.model.dto.ChartQueryRequest;
import com.trajectory.cloud.api.ai.model.vo.ChartVO;

/**
 * 图表信息服务
 *
 * @author StephenQiu30
 */
public interface ChartService extends IService<Chart> {

    /**
     * 获取查询包装器
     *
     * @param chartQueryRequest 查询请求
     * @return 查询包装器
     */
    LambdaQueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

    /**
     * 获取图表视图
     *
     * @param chart 图表实体
     * @return 图表视图
     */
    ChartVO getChartVO(Chart chart);

    /**
     * 分页获取图表视图
     *
     * @param chartPage 图表分页对象
     * @return 图表视图分页对象
     */
    Page<ChartVO> getChartVOPage(Page<Chart> chartPage);
}
