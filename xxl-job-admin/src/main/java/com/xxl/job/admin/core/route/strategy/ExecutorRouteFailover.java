package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * Created by xuxueli on 17/3/10.
 * 从头到尾遍历地址列表，每个地址执行心跳检测，心跳成功，则返回该地址；若失败，进行下一个地址检测。
 * 缺陷:先心跳一个地址，确认无误后就直接执行了。这里面的问题在于没有考虑执行器节点历史执行情况，因为执行器接收心跳后，没有执行业务逻辑就返回了，如果业务有问题或节点假死，有可能心态还能返回，但是业务等其他操作无法继续，如果选择这个地址，那业务任然无法执行。
 * 依然心跳每个地址，以排除网络都无法处理的情况，最终会产生一个候选列表；
 * 能返回，但是业务等其他操作无法继续，如果选择这个地址，那业务任然无法执行。因此逻辑应改为如下：
 * 依然心跳每个地址，以排除网络都无法处理的情况，最终会产生一个候选列表；
 * ​ 如果候选列表中有多个地址，则选择“标记时间”最远或不存在的节点。
 * https://blog.csdn.net/trtrtg/article/details/140908872
 */
public class ExecutorRouteFailover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        //看看ExecutorRouteFailover地址是怎么选取的：

        StringBuffer beatResultSB = new StringBuffer();
        for (String address : addressList) {
            // beat
            ReturnT<String> beatResult = null;
            try {
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                beatResult = executorBiz.beat();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                beatResult = new ReturnT<String>(ReturnT.FAIL_CODE, ""+e );
            }
            beatResultSB.append( (beatResultSB.length()>0)?"<br><br>":"")
                    .append(I18nUtil.getString("jobconf_beat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMsg());

            // beat success
            if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {

                beatResult.setMsg(beatResultSB.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return new ReturnT<String>(ReturnT.FAIL_CODE, beatResultSB.toString());

    }
}
