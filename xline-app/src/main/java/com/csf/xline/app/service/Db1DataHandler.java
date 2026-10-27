package com.csf.xline.app.service;

import com.csf.xline.db.sqlserver.jtds.JtdsNode;
import com.csf.xline.db.sqlserver.jtds.handler.JtdsDataHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.anyline.entity.DataSet;
import org.anyline.proxy.ServiceProxy;
import org.anyline.service.AnylineService;
import org.anyline.util.ConfigTable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class Db1DataHandler implements JtdsDataHandler, TimerTask {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ThreadFactory threadFactory = new DefaultThreadFactory("data-thread", true, Thread.NORM_PRIORITY);
    private final HashedWheelTimer timer = new HashedWheelTimer(threadFactory, 100L, TimeUnit.MILLISECONDS, 512, true, 10L);


    private JtdsNode node;
    private AnylineService anylineService;

    @Override
    public void init(JtdsNode node) throws Exception {
        this.node = node;

        ConfigTable.IS_LOG_SQL = true;
        ConfigTable.SQL_SELECT_TIMEOUT = 3;
        // 数据源在节点初始化时以节点名注册，按节点名取服务
        anylineService = ServiceProxy.service(node.getName());

        timer.newTimeout(this, 1L, TimeUnit.SECONDS);
    }

    @Override
    public void rec(JtdsNode node, Object data) throws Exception {
        log.info("**************** rec data: {}", data);
    }

    @Override
    public void destroy(JtdsNode node) throws Exception {
        timer.stop();
        executor.shutdown();
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        if (timeout.isCancelled()) {
            return;
        }
        if (anylineService == null) {
            log.error("未取到节点 [{}] 的数据源服务，停止轮询", node.getName());
            return;
        }

        // todo 业务代码
        DataSet res = anylineService.selects("stc", "order by id desc");

        log.info("======> read data: {}", res);

        timer.newTimeout(this, 1L, TimeUnit.SECONDS);
    }
}
