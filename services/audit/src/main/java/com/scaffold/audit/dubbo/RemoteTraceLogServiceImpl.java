package com.scaffold.audit.dubbo;

import com.scaffold.audit.api.RemoteTraceLogService;
import com.scaffold.audit.api.TraceLogEntry;
import com.scaffold.audit.domain.AuditTrace;
import com.scaffold.audit.repository.AuditTraceRepository;
import com.scaffold.common.core.crypto.Sm2Engine;
import com.scaffold.common.core.crypto.Sm3Digester;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * 全链路业务留痕实现（G2/G3）：
 *
 * <ul>
 *   <li>SM3 摘要链：curr = SM3(prev + "|" + 本条规范化串)，prev 取最新一条留痕的 curr—— 篡改历史任一条，其后所有摘要校验即断链；
 *   <li>关键动作 SM2 签名：对 curr_digest 签名（audit.trace.sm2-private-key/sm2-public-key， hex 裸密钥：私钥 32
 *       字节/公钥 65 字节 04||X||Y）；未配置密钥时签名跳过，留痕照写 （合规密钥由密评阶段经配置中心注入，配好即生效，无需改代码）；
 *   <li>并发说明：多节点同刻写入会读到同一 prev 形成链分叉，校验器按 trace_id 排序 首个断点即报——留痕写入量低（关键业务动作级），分叉概率与处置成本可接受；
 *       写入竞争加剧时收敛为单点写入或按 scene 分链。
 * </ul>
 *
 * @author ct
 */
@DubboService
public class RemoteTraceLogServiceImpl implements RemoteTraceLogService {
  private static final Logger log = LoggerFactory.getLogger(RemoteTraceLogServiceImpl.class);

  /** 链分隔符（prev 与本条规范串之间；摘要字段本身是 hex 不含竖线，无歧义） */
  private static final String CHAIN_SEPARATOR = "|";

  private final AuditTraceRepository traceRepository;

  private final SnowflakeIdGenerator idGenerator;

  /** SM2 私钥（hex，32 字节裸密钥；空 = 签名跳过） */
  @Value("${audit.trace.sm2-private-key:}")
  private String sm2PrivateKey;

  /** SM2 公钥（hex，65 字节裸密钥 04||X||Y） */
  @Value("${audit.trace.sm2-public-key:}")
  private String sm2PublicKey;

  private volatile Sm2Engine sm2Engine;

  public RemoteTraceLogServiceImpl(AuditTraceRepository traceRepository, SnowflakeIdGenerator idGenerator) {
    this.traceRepository = traceRepository;
    this.idGenerator = idGenerator;
  }

  @PostConstruct
  void initEngine() {
    if (StringUtils.isBlank(sm2PrivateKey) || StringUtils.isBlank(sm2PublicKey)) {
      log.info("[业务留痕] 未配置 SM2 密钥（audit.trace.sm2-private-key/public-key），关键动作签名暂跳过");
      return;
    }
    try {
      sm2Engine = new Sm2Engine(hexToBytes(sm2PrivateKey.trim()), hexToBytes(sm2PublicKey.trim()));
      log.info("[业务留痕] SM2 签名已启用（密钥来自配置中心）");
    } catch (Exception e) {
      log.error("[业务留痕] SM2 密钥初始化失败，签名暂跳过（请检查 hex 格式：私钥 64 字符/公钥 130 字符）", e);
    }
  }

  @Override
  public int record(TraceLogEntry entry) {
    if (entry == null || StringUtils.isBlank(entry.getScene())) {
      return 0;
    }
    String prev = traceRepository.selectLastDigest();
    String canonical = canonicalize(entry, prev);
    String curr = Sm3Digester.digest(canonical);

    AuditTrace trace = new AuditTrace();
    trace.setTraceId(idGenerator.nextId());
    trace.setScene(entry.getScene());
    trace.setBizType(entry.getBizType());
    trace.setBizId(entry.getBizId());
    trace.setOperator(entry.getOperator());
    trace.setOperateIp(entry.getOperateIp());
    trace.setTerminal(entry.getTerminal());
    trace.setMethodName(entry.getMethodName());
    trace.setSnapshotJson(entry.getSnapshotJson());
    trace.setResultJson(entry.getResultJson());
    trace.setErrorMsg(entry.getErrorMsg());
    trace.setCostMs(entry.getCostMs());
    trace.setKeyAction(entry.isKeyAction() ? "1" : "0");
    trace.setPrevDigest(prev);
    trace.setCurrDigest(curr);
    trace.setSignValue(signIfNeeded(curr, entry.isKeyAction()));
    trace.setOpTime(entry.getOperateTime() != null ? entry.getOperateTime() : new Date());
    if (trace.getCreateTime() == null) {
      // 与原 insert 的 createTime 兜底 current_timestamp 语义一致
      trace.setCreateTime(new Date());
    }
    traceRepository.save(trace);
    return 1;
  }

  /** 规范化串：固定字段序 + 固定分隔，快照/结果/异常空值以空串占位—— 同一条数据任何字节改动都会改变摘要；新增字段需追加在尾部（历史摘要不变）。 */
  private String canonicalize(TraceLogEntry entry, String prev) {
    return nullToEmpty(prev)
        + CHAIN_SEPARATOR
        + nullToEmpty(entry.getScene())
        + "|"
        + nullToEmpty(entry.getBizType())
        + "|"
        + nullToEmpty(entry.getBizId())
        + "|"
        + nullToEmpty(entry.getOperator())
        + "|"
        + nullToEmpty(entry.getOperateIp())
        + "|"
        + nullToEmpty(entry.getTerminal())
        + "|"
        + nullToEmpty(entry.getMethodName())
        + "|"
        + (entry.getOperateTime() != null ? entry.getOperateTime().getTime() : "")
        + "|"
        + entry.getCostMs()
        + "|"
        + (entry.isKeyAction() ? "1" : "0")
        + "|"
        + nullToEmpty(entry.getSnapshotJson())
        + "|"
        + nullToEmpty(entry.getResultJson())
        + "|"
        + nullToEmpty(entry.getErrorMsg());
  }

  /** 签名失败不阻断留痕（降级为无签名留痕，密评整改以日志定位） */
  private String signIfNeeded(String currDigest, boolean keyAction) {
    if (!keyAction || sm2Engine == null) {
      return null;
    }
    try {
      return sm2Engine.sign(currDigest);
    } catch (Exception e) {
      log.error("[业务留痕] 关键动作 SM2 签名失败（留痕照写，无签名）: digest={}", currDigest, e);
      return null;
    }
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < out.length; i++) {
      out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
    }
    return out;
  }
}
