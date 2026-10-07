package com.scaffold.system.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.system.api.model.LoginUser;
import com.scaffold.system.domain.SysUserOnline;
import com.scaffold.system.service.impl.SysUserOnlineServiceImpl;
import com.scaffold.system.service.convert.SysUserOnlineConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * SysUserOnlineServiceImpl Mock 测试（在线用户查询与转换）。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>selectOnlineByIpaddr：IP 匹配则转换，不匹配返回 null</li>
 *   <li>selectOnlineByUserName：用户名匹配则转换，不匹配返回 null</li>
 *   <li>selectOnlineByInfo：IP + 用户名同时匹配则转换，否则 null</li>
 *   <li>loginUserToUserOnline：user 为 null → null；正常转换</li>
 * </ul>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserOnlineServiceImplTest
{
    @Mock
    private SysUserOnlineConverter sysUserOnlineMapper;

    private SysUserOnlineServiceImpl onlineService;

    @BeforeEach
    void setUp()
    {
        onlineService = new SysUserOnlineServiceImpl(sysUserOnlineMapper);
    }

    @Nested
    @DisplayName("selectOnlineByIpaddr")
    class SelectOnlineByIpaddrTests
    {
        @Test
        @DisplayName("IP 匹配 → 转换并返回")
        void ipMatches_returnsUserOnline()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline online = userOnline("token-1", "admin", "192.168.1.100");
            when(sysUserOnlineMapper.fromLoginUser(user)).thenReturn(online);

            SysUserOnline result = onlineService.selectOnlineByIpaddr("192.168.1.100", user);

            assertNotNull(result);
            assertEquals("token-1", result.getTokenId());
        }

        @Test
        @DisplayName("IP 不匹配 → 返回 null")
        void ipNotMatches_returnsNull()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline result = onlineService.selectOnlineByIpaddr("10.0.0.1", user);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("selectOnlineByUserName")
    class SelectOnlineByUserNameTests
    {
        @Test
        @DisplayName("用户名匹配 → 转换并返回")
        void userNameMatches_returnsUserOnline()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline online = userOnline("token-1", "admin", "192.168.1.100");
            when(sysUserOnlineMapper.fromLoginUser(user)).thenReturn(online);

            SysUserOnline result = onlineService.selectOnlineByUserName("admin", user);

            assertNotNull(result);
        }

        @Test
        @DisplayName("用户名不匹配 → 返回 null")
        void userNameNotMatches_returnsNull()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline result = onlineService.selectOnlineByUserName("other", user);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("selectOnlineByInfo")
    class SelectOnlineByInfoTests
    {
        @Test
        @DisplayName("IP + 用户名都匹配 → 返回")
        void bothMatch_returnsUserOnline()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline online = userOnline("token-1", "admin", "192.168.1.100");
            when(sysUserOnlineMapper.fromLoginUser(user)).thenReturn(online);

            SysUserOnline result = onlineService.selectOnlineByInfo("192.168.1.100", "admin", user);

            assertNotNull(result);
        }

        @Test
        @DisplayName("IP 匹配但用户名不匹配 → 返回 null")
        void ipMatchesUserNameNot_returnsNull()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline result = onlineService.selectOnlineByInfo("192.168.1.100", "other", user);
            assertNull(result);
        }

        @Test
        @DisplayName("用户名匹配但 IP 不匹配 → 返回 null")
        void userNameMatchesIpNot_returnsNull()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline result = onlineService.selectOnlineByInfo("10.0.0.1", "admin", user);
            assertNull(result);
        }

        @Test
        @DisplayName("都不匹配 → 返回 null")
        void neitherMatches_returnsNull()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline result = onlineService.selectOnlineByInfo("10.0.0.1", "other", user);
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("loginUserToUserOnline")
    class LoginUserToUserOnlineTests
    {
        @Test
        @DisplayName("user 为 null → 返回 null，不抛异常")
        void nullUser_returnsNull()
        {
            assertNull(onlineService.loginUserToUserOnline(null));
        }

        @Test
        @DisplayName("正常 user → 调用 mapper.fromLoginUser 并返回结果")
        void normalUser_callsMapper()
        {
            LoginUser user = loginUser(1L, "admin", "192.168.1.100");
            SysUserOnline online = userOnline("token-1", "admin", "192.168.1.100");
            when(sysUserOnlineMapper.fromLoginUser(user)).thenReturn(online);

            SysUserOnline result = onlineService.loginUserToUserOnline(user);

            assertNotNull(result);
            assertEquals("token-1", result.getTokenId());
        }
    }

    // ─────────────────────────────────────────────
    // 辅助方法
    // ─────────────────────────────────────────────

    private LoginUser loginUser(Long userId, String userName, String ipaddr)
    {
        LoginUser u = new LoginUser();
        u.setUserid(userId);
        u.setUsername(userName);
        u.setIpaddr(ipaddr);
        u.setToken("token-" + userId);
        return u;
    }

    private SysUserOnline userOnline(String tokenId, String userName, String ipaddr)
    {
        SysUserOnline o = new SysUserOnline();
        o.setTokenId(tokenId);
        o.setUserName(userName);
        o.setIpaddr(ipaddr);
        return o;
    }
}
