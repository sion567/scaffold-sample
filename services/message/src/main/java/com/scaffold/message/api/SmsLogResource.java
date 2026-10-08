package com.scaffold.message.api;

import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.message.domain.MsgSmsLog;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 短信发送日志接口（§6.1：查询/详情/失败重发/清理/导出）
 *
 * @author ct
 */
@RequestMapping("/message/sms/log")
public interface SmsLogResource {
  /**
   * 日志列表。过滤字段摊平为 @RequestParam：Dubbo Triple REST 对显式
   * {@code @ModelAttribute} 会误读 required 属性而 500（同 system 服务约定）。
   */
  @GetMapping("/list")
  TableDataInfo list(@RequestParam(value = "mobile", required = false) String mobile,
                     @RequestParam(value = "sendStatus", required = false) String sendStatus,
                     @RequestParam(value = "templateCode", required = false) String templateCode,
                     @RequestParam(value = "channelType", required = false) String channelType,
                     @RequestParam(value = "bizType", required = false) String bizType,
                     @RequestParam(value = "beginTime", required = false) String beginTime,
                     @RequestParam(value = "endTime", required = false) String endTime);

  @GetMapping("/{id}")
  AjaxResult getInfo(@PathVariable("id") Long id);

  /** 失败重发（RETRY_COUNT<5） */
  @PostMapping("/retry/{id}")
  AjaxResult retry(@PathVariable("id") Long id);

  @DeleteMapping("/{ids}")
  AjaxResult remove(@PathVariable("ids") Long[] ids);

  /** 导出（手机号中间四位脱敏；过滤字段摊平为 @RequestParam，原因见 {@link #list}） */
  @RequestMapping(
      value = "/export",
      method = {RequestMethod.GET, RequestMethod.POST},
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  byte[] export(@RequestParam(value = "mobile", required = false) String mobile,
                @RequestParam(value = "sendStatus", required = false) String sendStatus,
                @RequestParam(value = "templateCode", required = false) String templateCode,
                @RequestParam(value = "channelType", required = false) String channelType,
                @RequestParam(value = "bizType", required = false) String bizType,
                @RequestParam(value = "beginTime", required = false) String beginTime,
                @RequestParam(value = "endTime", required = false) String endTime);
}
