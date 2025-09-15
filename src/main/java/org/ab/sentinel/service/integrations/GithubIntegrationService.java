package org.ab.sentinel.service.integrations;

import berlin.yuna.typemap.model.TypeMapI;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.dto.GithubDto;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;

public class GithubIntegrationService extends Service {

    @Override
    public void start() {
        context.info(() -> "[{}] started", name());
    }

    @Override
    public void stop() {

    }

    @Override
    public Object onFailure(final Event event) {
        return null;
    }

    @Override
    public void onEvent(final Event event) {
        event.ifPresentAck(AppEvents.GITHUB_INT_REQ, GithubDto.class, this::githubIntReq);
    }

    private Boolean githubIntReq(GithubDto githubDto) {
        // TODO: Verify access token with scope
        return false;
    }

    @Override
    public void configure(final TypeMapI<?> typeMapI, final TypeMapI<?> typeMapI1) {

    }
}
