package com.scaffold.audit.dubbo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scaffold.audit.api.TraceLogEntry;
import com.scaffold.audit.domain.AuditTrace;
import com.scaffold.audit.repository.AuditTraceRepository;
import com.scaffold.common.core.crypto.Sm2Engine;
import com.scaffold.common.core.crypto.Sm3Digester;
import com.scaffold.common.core.utils.uuid.SnowflakeIdGenerator;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RemoteTraceLogServiceImpl Mock 测试（G2：SM3 摘要链接续、关键动作 SM2 签名、入参守卫）。 SM3/SM2 走真实国密实现
 * （common/core crypto，BouncyCastle），密钥用 generateKeyPair 现场生成。
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class RemoteTraceLogServiceImplTest {
  @Mock private AuditTraceRepository traceRepository;

  @Mock private SnowflakeIdGenerator idGenerator;

  @InjectMocks private RemoteTraceLogServiceImpl service;

  @BeforeEach
  void resetEngineField() {
    // 默认未配置密钥：引擎字段为 null（@Value 在纯 Mockito 下不注入）
    ReflectionTestUtils.setField(service, "sm2Engine", null);
    ReflectionTestUtils.setField(service, "sm2PrivateKey", "");
    ReflectionTestUtils.setField(service, "sm2PublicKey", "");
  }

  private TraceLogEntry entry(String scene, boolean keyAction) {
    TraceLogEntry entry = new TraceLogEntry();
    entry.setScene(scene);
    entry.setBizType("instruction");
    entry.setBizId("66");
    entry.setOperator("admin");
    entry.setKeyAction(keyAction);
    return entry;
  }

  @Test
  @DisplayName("留痕字段全量映射：报文逐项落库，显式操作时间透传，canonical 摘要精确复算")
  void record_maps_every_field_and_digest() {
    when(traceRepository.selectLastDigest()).thenReturn("PREVDIGEST");
    when(idGenerator.nextId()).thenReturn(990002L);
    java.util.Date opTime = new java.util.Date(1700000000000L);
    TraceLogEntry e = entry("指令下发", false);
    e.setOperateIp("10.2.3.4");
    e.setTerminal("PC");
    e.setMethodName("svc.method");
    e.setSnapshotJson("{\"a\":1}");
    e.setResultJson("{\"b\":2}");
    e.setCostMs(123L);
    e.setOperateTime(opTime);

    service.record(e);

    ArgumentCaptor<AuditTrace> captor = ArgumentCaptor.forClass(AuditTrace.class);
    verify(traceRepository).save(captor.capture());
    AuditTrace trace = captor.getValue();
    assertEquals("指令下发", trace.getScene());
    assertEquals("instruction", trace.getBizType());
    assertEquals("66", trace.getBizId());
    assertEquals("admin", trace.getOperator());
    assertEquals("10.2.3.4", trace.getOperateIp());
    assertEquals("PC", trace.getTerminal());
    assertEquals("svc.method", trace.getMethodName());
    assertEquals("{\"a\":1}", trace.getSnapshotJson());
    assertEquals("{\"b\":2}", trace.getResultJson());
    assertEquals(Long.valueOf(123L), trace.getCostMs());
    assertEquals("PREVDIGEST", trace.getPrevDigest());
    assertEquals(opTime, trace.getOpTime(), "显式操作时间透传不覆盖");
    // 规范化串精确复算：字段序/分隔/占位任一变化都会导致摘要失配
    String canonical =
        "PREVDIGEST|指令下发|instruction|66|admin|10.2.3.4|PC|svc.method|"
            + 1700000000000L
            + "|123|0|{\"a\":1}|{\"b\":2}|";
    assertEquals(Sm3Digester.digest(canonical), trace.getCurrDigest());
  }

  @Test
  @DisplayName("入参守卫：entry 为空或缺 scene 返回 0，不落库")
  void record_guards() {
    assertEquals(0, service.record(null));
    assertEquals(0, service.record(new TraceLogEntry()));
    assertEquals(0, service.record(entry("  ", false)));
    verify(traceRepository, never()).save(any());
  }

  @Test
  @DisplayName("链头：空表 prev 为 null，curr 为真实 SM3 摘要（64 hex）")
  void record_firstLink() {
    when(traceRepository.selectLastDigest()).thenReturn(null);
    when(idGenerator.nextId()).thenReturn(990001L);
    

    assertEquals(1, service.record(entry("指令下发", false)));

    ArgumentCaptor<AuditTrace> captor = ArgumentCaptor.forClass(AuditTrace.class);
    verify(traceRepository).save(captor.capture());
    AuditTrace trace = captor.getValue();
    assertEquals(990001L, trace.getTraceId().longValue());
    assertNull(trace.getPrevDigest());
    assertNotNull(trace.getCurrDigest());
    assertEquals(64, trace.getCurrDigest().length(), "SM3 摘要为 64 位 hex");
    assertEquals("0", trace.getKeyAction());
    assertNull(trace.getSignValue());
  }

  @Test
  @DisplayName("链接续：第二条的 prev = 第一条的 curr，且 curr 随内容变化")
  void record_chains() {
    when(traceRepository.selectLastDigest()).thenReturn("digest-abc", "digest-abc");
    when(idGenerator.nextId()).thenReturn(1L, 2L);

    service.record(entry("指令下发", false));
    ArgumentCaptor<AuditTrace> first = ArgumentCaptor.forClass(AuditTrace.class);
    verify(traceRepository).save(first.capture());
    assertEquals("digest-abc", first.getValue().getPrevDigest());

    TraceLogEntry second = entry("指令签收", false);
    service.record(second);
    ArgumentCaptor<AuditTrace> all = ArgumentCaptor.forClass(AuditTrace.class);
    verify(traceRepository, org.mockito.Mockito.times(2)).save(all.capture());
    AuditTrace last = all.getAllValues().get(1);
    assertEquals("digest-abc", last.getPrevDigest());
    assertNotEquals(first.getValue().getCurrDigest(), last.getCurrDigest(), "场景不同摘要必须不同");
  }

  @Test
  @DisplayName("篡改检测：任一字节变化 → 摘要变化（SM3 碰撞之外）")
  void digest_tamperEvident() {
    when(traceRepository.selectLastDigest()).thenReturn("p");
    when(idGenerator.nextId()).thenReturn(1L, 2L);

    service.record(entry("指令下发", false));
    ArgumentCaptor<AuditTrace> first = ArgumentCaptor.forClass(AuditTrace.class);
    verify(traceRepository).save(first.capture());

    TraceLogEntry tampered = entry("指令下发", false);
    tampered.setBizId("67");
    org.mockito.Mockito.clearInvocations(traceRepository);
    service.record(tampered);
    ArgumentCaptor<AuditTrace> second = ArgumentCaptor.forClass(AuditTrace.class);
    verify(traceRepository).save(second.capture());

    assertNotEquals(first.getValue().getCurrDigest(), second.getValue().getCurrDigest());
  }

  @Test
  @DisplayName("关键动作 SM2 签名：配置密钥后签名落库且可用公钥验签")
  void record_keyAction_signs_and_verifies() {
    KeyPair keyPair = Sm2Engine.generateKeyPair();
    String privHex = toHex(rawPrivate(keyPair));
    String pubHex = toHex(rawPublic(keyPair));
    ReflectionTestUtils.setField(service, "sm2PrivateKey", privHex);
    ReflectionTestUtils.setField(service, "sm2PublicKey", pubHex);
    service.initEngine();

    when(traceRepository.selectLastDigest()).thenReturn(null);
    when(idGenerator.nextId()).thenReturn(990002L);
    

    assertEquals(1, service.record(entry("指令撤回", true)));

    ArgumentCaptor<AuditTrace> captor = ArgumentCaptor.forClass(AuditTrace.class);
    verify(traceRepository).save(captor.capture());
    AuditTrace trace = captor.getValue();
    assertEquals("1", trace.getKeyAction());
    assertNotNull(trace.getSignValue(), "配置了密钥的关键动作必须签名");

    // 用同一密钥的引擎验签：签名确实针对本条 curr_digest
    Sm2Engine verifier = new Sm2Engine(rawPrivate(keyPair), rawPublic(keyPair));
    assertTrue(verifier.verify(trace.getCurrDigest(), trace.getSignValue()));
    assertFalse(verifier.verify("tampered", trace.getSignValue()));
  }

  @Test
  @DisplayName("未配置密钥：关键动作留痕照写，签名跳过")
  void record_keyAction_withoutKey_skipsSign() {
    when(traceRepository.selectLastDigest()).thenReturn(null);
    when(idGenerator.nextId()).thenReturn(990003L);
    

    assertEquals(1, service.record(entry("指令撤回", true)));

    ArgumentCaptor<AuditTrace> captor = ArgumentCaptor.forClass(AuditTrace.class);
    verify(traceRepository).save(captor.capture());
    assertNull(captor.getValue().getSignValue());
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  /** Sm2Engine 要求裸密钥：私钥 32 字节，公钥 65 字节（04||X||Y） */
  private static byte[] rawPrivate(KeyPair keyPair) {
    return to32(((ECPrivateKey) keyPair.getPrivate()).getS());
  }

  private static byte[] rawPublic(KeyPair keyPair) {
    ECPoint w = ((ECPublicKey) keyPair.getPublic()).getW();
    byte[] out = new byte[65];
    out[0] = 0x04;
    System.arraycopy(to32(w.getAffineX()), 0, out, 1, 32);
    System.arraycopy(to32(w.getAffineY()), 0, out, 33, 32);
    return out;
  }

  private static byte[] to32(BigInteger v) {
    byte[] b = v.toByteArray();
    byte[] out = new byte[32];
    if (b.length <= 32) {
      System.arraycopy(b, 0, out, 32 - b.length, b.length);
    } else {
      System.arraycopy(b, b.length - 32, out, 0, 32);
    }
    return out;
  }
}
