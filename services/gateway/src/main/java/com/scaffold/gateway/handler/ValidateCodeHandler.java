package com.scaffold.gateway.handler;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import com.scaffold.common.core.exception.CaptchaException;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.gateway.service.ValidateCodeService;
import reactor.core.publisher.Mono;

/**
 * 验证码获取
 *
 * @author scaffold
 */
@Component
public class ValidateCodeHandler implements HandlerFunction<ServerResponse>
{
    private final ValidateCodeService validateCodeService;

    public ValidateCodeHandler(ValidateCodeService validateCodeService)
    {
        this.validateCodeService = validateCodeService;
    }

    @Override
    public Mono<ServerResponse> handle(ServerRequest serverRequest)
    {
        AjaxResult ajax;
        try
        {
            ajax = validateCodeService.createCaptcha();
        }
        catch (CaptchaException | IOException e)
        {
            return Mono.error(e);
        }
        return ServerResponse.status(HttpStatus.OK).body(BodyInserters.fromValue(ajax));
    }
}
